package org.bakasu.bakasu.ui.screen.susfs.subpages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.RestartAlt
import androidx.compose.material.icons.twotone.Restore
import androidx.compose.material.icons.twotone.Save
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.bakasu.bakasu.R
import org.bakasu.bakasu.domain.model.SuSFSStatusInfo
import org.bakasu.bakasu.domain.usecase.RebootUseCase
import org.bakasu.bakasu.ui.component.WarningCard
import org.bakasu.bakasu.ui.component.rememberConfirmDialog
import org.bakasu.bakasu.ui.component.settings.SegmentedColumn
import org.bakasu.bakasu.ui.component.settings.SettingsBaseWidget
import org.bakasu.bakasu.ui.component.settings.SettingsJumpPageWidget
import org.bakasu.bakasu.ui.component.settings.SettingsSwitchWidget
import org.bakasu.bakasu.ui.screen.susfs.RegisterSuSFSRefresh
import org.bakasu.bakasu.ui.screen.susfs.SuSFSRefreshRegistrar
import org.bakasu.bakasu.ui.util.LocalSnackbarHost
import org.bakasu.bakasu.ui.util.showReplacingSnackbar
import org.bakasu.bakasu.ui.viewmodel.SuSFSUiAction
import org.bakasu.bakasu.ui.viewmodel.SuSFSViewModel
import org.bakasu.bakasu.ui.viewmodel.awaitSuSFSBoolean
import org.bakasu.bakasu.ui.viewmodel.awaitSuSFSStatusInfo
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog

@Composable
fun StatusTab(
    nestedScrollConnection: NestedScrollConnection,
    innerPadding: PaddingValues,
    onRegisterRefresh: SuSFSRefreshRegistrar,
    configEnabled: Boolean,
    configEnabledLoaded: Boolean,
    onConfigEnabledChange: (Boolean) -> Unit,
    onConfigRestored: () -> Unit,
) {
    val configHelper = koinViewModel<SuSFSViewModel>()
    var statusInfo by remember { mutableStateOf(SuSFSStatusInfo("", "", "")) }

    RegisterSuSFSRefresh(onRegisterRefresh) { _, forceRefresh ->
        statusInfo = awaitSuSFSStatusInfo(configHelper, forceRefresh)
            ?: SuSFSStatusInfo("", "", "")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
    ) {
        item {
            Spacer(Modifier.height(innerPadding.calculateTopPadding()))
        }

        item {
            AnimatedVisibility(
                visible = configEnabledLoaded && !configEnabled,
            ) {
                WarningCard(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    message = stringResource(R.string.susfs_config_disable_warning),
                )
            }
        }

        item {
            AnimatedVisibility(
                visible = !statusInfo.supercallSupported,
            ) {
                val guardModule = statusInfo.guardModule
                val guardLoader = statusInfo.guardLoader
                if (guardModule != null) {
                    WarningCard(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        message = stringResource(R.string.susfs_supercall_unsupported_guard, guardModule),
                    )
                } else if (guardLoader != null) {
                    // A known guard loader module is enabled but its guard LKM
                    // is not in the kernel yet: offer the one-tap fix (soft
                    // restart re-runs the module stage) instead of only
                    // stating that changes are blocked.
                    GuardPendingCard(guardLoader)
                } else {
                    WarningCard(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        message = stringResource(R.string.susfs_supercall_unsupported),
                    )
                }
            }
        }

        item {
            SegmentedColumn {
                item {
                    SettingsSwitchWidget(
                        icon = Icons.TwoTone.Save,
                        title = stringResource(R.string.susfs_enable_config),
                        description = stringResource(R.string.susfs_enable_config_summary),
                        checked = configEnabled,
                        enabled = configEnabledLoaded,
                        onCheckedChange = onConfigEnabledChange,
                    )
                }
                item {
                    SettingsBaseWidget(
                        icon = Icons.TwoTone.Info,
                        title = stringResource(R.string.susfs_status_version),
                        description = statusInfo.version.ifBlank {
                            stringResource(R.string.susfs_status_no_data)
                        },
                    )
                }
                item {
                    SettingsBaseWidget(
                        icon = Icons.TwoTone.Settings,
                        title = stringResource(R.string.susfs_status_variant),
                        description = statusInfo.variant.ifBlank {
                            stringResource(R.string.susfs_status_no_data)
                        },
                    )
                }
                item {
                    SettingsBaseWidget(
                        icon = Icons.TwoTone.TaskAlt,
                        title = stringResource(R.string.susfs_status_enabled_features),
                        description = statusInfo.enabledFeatures.ifBlank {
                            stringResource(R.string.susfs_status_no_data)
                        },
                    )
                }
            }
        }

        item {
            BackupRestoreSection(onConfigRestored = onConfigRestored)
        }

        item {
            Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
        }
    }
}

@Composable
fun BackupRestoreSection(
    onConfigRestored: () -> Unit,
) {
    val configHelper = koinViewModel<SuSFSViewModel>()
    val snackbarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()

    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val exportTitle = stringResource(R.string.susfs_backup_export)
    val exportDesc = stringResource(R.string.susfs_backup_description)
    val importTitle = stringResource(R.string.susfs_backup_import)
    val importDesc = stringResource(R.string.susfs_restore_description)
    val exportSuccessMsg = stringResource(R.string.susfs_backup_export_success)
    val exportFailedMsg = stringResource(R.string.susfs_backup_export_failed)
    val importSuccessMsg = stringResource(R.string.susfs_backup_import_success)
    val importFailedMsg = stringResource(R.string.susfs_backup_import_failed)
    val restoreDefaultTitle = stringResource(R.string.susfs_backup_restore_default)
    val restoreDefaultDesc = stringResource(R.string.susfs_backup_restore_default_desc)
    val confirmTitle = stringResource(R.string.susfs_backup_import_confirm_title)
    val confirmMsg = stringResource(R.string.susfs_backup_import_confirm_message)
    val importLabel = stringResource(R.string.susfs_backup_import_label)
    val cancelLabel = stringResource(R.string.susfs_entry_cancel)
    val operationSuccessMsg = stringResource(R.string.susfs_operation_success)
    val operationFailedMsg = stringResource(R.string.susfs_operation_failed)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = awaitSuSFSBoolean(configHelper) { reply ->
                SuSFSUiAction.ExportConfig(uri.toString(), reply)
            }
            scope.launch {
                snackbarHost.showReplacingSnackbar(if (ok) exportSuccessMsg else exportFailedMsg)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        pendingImportUri = uri
        showImportConfirm = true
    }

    SegmentedColumn {
        item {
            SettingsJumpPageWidget(
                icon = Icons.TwoTone.Save,
                title = exportTitle,
                description = exportDesc,
                onClick = {
                    exportLauncher.launch("susfs_backup.json")
                },
            )
        }
        item {
            SettingsJumpPageWidget(
                icon = Icons.TwoTone.Restore,
                title = importTitle,
                description = importDesc,
                onClick = {
                    importLauncher.launch(arrayOf("application/json"))
                },
            )
        }
        item {
            RestoreDefaultRow(
                title = restoreDefaultTitle,
                description = restoreDefaultDesc,
                snackbarHost = snackbarHost,
                operationSuccessMsg = operationSuccessMsg,
                operationFailedMsg = operationFailedMsg,
                onConfigRestored = onConfigRestored,
            )
        }
    }

    OverlayDialog(
        show = showImportConfirm,
        onDismissRequest = {
            showImportConfirm = false
            pendingImportUri = null
        },
        title = confirmTitle,
        summary = confirmMsg,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            Button(
                onClick = {
                    val uri = pendingImportUri
                    showImportConfirm = false
                    pendingImportUri = null
                    if (uri != null) {
                        scope.launch {
                            val ok = awaitSuSFSBoolean(configHelper) { reply ->
                                SuSFSUiAction.ImportConfig(uri.toString(), reply)
                            }
                            if (ok) {
                                onConfigRestored()
                            }
                            scope.launch {
                                snackbarHost.showReplacingSnackbar(if (ok) importSuccessMsg else importFailedMsg)
                            }
                        }
                    }
                },
            ) {
                Text(importLabel)
            }
            TextButton(
                text = cancelLabel,
                onClick = {
                    showImportConfirm = false
                    pendingImportUri = null
                },
            )
        }
    }
}

@Composable
private fun RestoreDefaultRow(
    title: String,
    description: String,
    snackbarHost: SnackbarHostState,
    operationSuccessMsg: String,
    operationFailedMsg: String,
    onConfigRestored: () -> Unit,
) {
    val configHelper = koinViewModel<SuSFSViewModel>()
    val scope = rememberCoroutineScope()
    SettingsJumpPageWidget(
        icon = Icons.TwoTone.RestartAlt,
        title = title,
        description = description,
        onClick = {
            scope.launch {
                val ok = awaitSuSFSBoolean(configHelper) { reply ->
                    SuSFSUiAction.RestoreDefault(reply)
                }
                if (ok) {
                    onConfigRestored()
                }
                snackbarHost.showReplacingSnackbar(if (ok) operationSuccessMsg else operationFailedMsg)
            }
        },
    )
}

@Composable
private fun GuardPendingCard(guardLoader: String) {
    val scope = rememberCoroutineScope()
    val reboot = koinInject<RebootUseCase>()
    val softRebootLabel = stringResource(R.string.reboot_soft)
    val guardPendingConfirm = stringResource(R.string.susfs_guard_pending_confirm)
    val softRebootDialog = rememberConfirmDialog(
        onConfirm = {
            // Same path as the reboot menu on the home screen: `ksud
            // soft-reboot` stops the framework, re-runs the module stage
            // (post-fs-data) which loads the guard LKM, then restarts the
            // framework — a full reboot is not required.
            scope.launch {
                reboot("soft_reboot")
            }
        },
    )

    WarningCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        message = stringResource(R.string.susfs_guard_pending, guardLoader),
        icon = {
            Icon(
                imageVector = Icons.TwoTone.RestartAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp),
            )
        },
        onClick = {
            softRebootDialog.showConfirm(
                title = softRebootLabel,
                content = guardPendingConfirm,
                confirm = softRebootLabel,
            )
        },
    )
}
