package org.bakasu.bakasu.ui.screen.main

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.system.Os
import android.widget.Toast
import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.MenuBook
import androidx.compose.material.icons.twotone.Android
import androidx.compose.material.icons.twotone.Block
import androidx.compose.material.icons.twotone.DeveloperBoard
import androidx.compose.material.icons.twotone.Error
import androidx.compose.material.icons.twotone.Extension
import androidx.compose.material.icons.twotone.FilterList
import androidx.compose.material.icons.twotone.Group
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.Memory
import androidx.compose.material.icons.twotone.PowerSettingsNew
import androidx.compose.material.icons.twotone.RestartAlt
import androidx.compose.material.icons.twotone.Security
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.Smartphone
import androidx.compose.material.icons.twotone.Tag
import androidx.compose.material.icons.twotone.TaskAlt
import androidx.compose.material.icons.twotone.Tune
import androidx.compose.material.icons.twotone.VolunteerActivism
import androidx.compose.material.icons.twotone.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bakasu.bakasu.BuildConfig
import org.bakasu.bakasu.Natives.KernelPatchImplementation
import org.bakasu.bakasu.R
import org.bakasu.bakasu.domain.model.HomeSystemInfo
import org.bakasu.bakasu.domain.model.KernelStatus
import org.bakasu.bakasu.domain.model.ManagerUpdateChannel
import org.bakasu.bakasu.domain.model.ManagerUpdateInfo
import org.bakasu.bakasu.domain.usecase.EnqueueManagerUpdateUseCase
import org.bakasu.bakasu.magica.MagicaService
import org.bakasu.bakasu.ui.component.KsuIsValid
import org.bakasu.bakasu.ui.component.SwipeableSnackbarHost
import org.bakasu.bakasu.ui.component.rememberConfirmDialog
import org.bakasu.bakasu.ui.component.rememberLoadingDialog
import org.bakasu.bakasu.ui.navigation.LocalNavigator
import org.bakasu.bakasu.ui.navigation.Route
import org.bakasu.bakasu.ui.screen.LabelText
import org.bakasu.bakasu.ui.theme.CardConfig
import org.bakasu.bakasu.ui.theme.ThemeConfig
import org.bakasu.bakasu.ui.theme.blurEffect
import org.bakasu.bakasu.ui.theme.blurSource
import org.bakasu.bakasu.ui.util.LocalPermissionRequestInterface
import org.bakasu.bakasu.ui.util.LocalSnackbarHost
import org.bakasu.bakasu.ui.util.adaptiveScaffoldWindowInsets
import org.bakasu.bakasu.ui.util.downloader.downloadManagerUpdate
import org.bakasu.bakasu.ui.viewmodel.HomeUiAction
import org.bakasu.bakasu.ui.viewmodel.HomeUiEvent
import org.bakasu.bakasu.ui.viewmodel.HomeUiState
import org.bakasu.bakasu.ui.viewmodel.HomeViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentColors
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * @author ShirkNeko
 * @date 2025/9/29.
 */

@Composable
fun HomePage(
    bottomPadding: Dp,
) {
    val context = LocalContext.current
    val viewModel = koinViewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.dispatch(HomeUiAction.AwaitInitialData)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeUiEvent.Error -> if (event.message.isNotBlank()) {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    ReportDrawnWhen { uiState.isInitialDataLoaded }

    val scrollBehavior = MiuixScrollBehavior()
    val scrollState = rememberScrollState()
    val navigator = LocalNavigator.current
    val loadingDialog = rememberLoadingDialog()
    val scope = rememberCoroutineScope()

    // Strings and dialog for the pending SuSFS guard card: a soft restart
    // re-runs the module stage and loads the guard without a full reboot.
    val softRebootLabel = stringResource(R.string.reboot_soft)
    val guardPendingConfirm = stringResource(R.string.susfs_guard_pending_confirm)
    val softRebootDialog = rememberConfirmDialog(
        onConfirm = {
            viewModel.dispatch(HomeUiAction.Reboot("soft_reboot"))
        },
    )

    MiuixScaffold(
        topBar = {
            TopBar(
                uiState = uiState,
                onReboot = { viewModel.dispatch(HomeUiAction.Reboot(it)) },
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = Color.Transparent,
        contentWindowInsets = adaptiveScaffoldWindowInsets(includeBottom = false),
        snackbarHost = {
            SwipeableSnackbarHost(
                modifier = Modifier.padding(bottom = bottomPadding),
                hostState = LocalSnackbarHost.current,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .blurSource()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(scrollState)
                .padding(
                    top = innerPadding.calculateTopPadding() + 2.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // 状态卡片
            if (uiState.isCoreDataLoaded) {
                if (uiState.systemStatus.isManager && !uiState.systemStatus.isFullFeatured) {
                    if ((
                            uiState.systemStatus.kernelUAPIVersion
                                ?: 1
                            ) > uiState.systemStatus.managerUAPIVersion
                    ) {
                        HomeNoticeCard(
                            message = stringResource(R.string.require_manager_version),
                            icon = Icons.TwoTone.Error,
                            onClick = {
                                if (uiState.systemStatus.isLateLoadMode) return@HomeNoticeCard
                                navigator.push(Route.Install(preselectedKernelUri = null))
                            },
                        )
                    } else {
                        HomeNoticeCard(
                            message = if (uiState.systemStatus.lkmMode == true) {
                                stringResource(R.string.require_kernel_version)
                            } else {
                                stringResource(R.string.require_kernel_version_gki)
                            },
                            icon = Icons.TwoTone.Error,
                            onClick = {
                                if (uiState.systemStatus.isLateLoadMode) return@HomeNoticeCard
                                navigator.push(Route.Install(preselectedKernelUri = null))
                            },
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 警告信息
                if (BuildConfig.DEBUG) {
                    HomeNoticeCard(
                        message = stringResource(R.string.debug_version_notice),
                        icon = Icons.TwoTone.Error,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (!uiState.systemStatus.isOfficialSignature) {
                    HomeNoticeCard(
                        message = stringResource(
                            R.string.unofficial_version_notice,
                            stringResource(R.string.app_name),
                        ),
                        icon = Icons.TwoTone.Error,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (BuildConfig.IS_PR_BUILD || uiState.systemStatus.isPrBuild) {
                    HomeNoticeCard(
                        message = stringResource(
                            id = R.string.home_pr_build_warning,
                        ),
                        icon = Icons.TwoTone.Error,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (uiState.systemStatus.kernelPatchImplementation == KernelPatchImplementation.OFFICIAL) {
                    HomeNoticeCard(
                        message = stringResource(
                            R.string.conflict_with_apatch,
                        ),
                        icon = Icons.TwoTone.Error,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (uiState.systemStatus.ksuVersion != null && !uiState.systemStatus.isRootAvailable) {
                    HomeNoticeCard(
                        message = stringResource(id = R.string.grant_root_failed),
                        icon = Icons.TwoTone.Error,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // The SuSFS guard loader module is enabled but its guard LKM
                // is not in the kernel yet: one tap re-runs the module stage
                // (soft restart) and loads the guard — no full reboot needed.
                if (uiState.systemInfo.susfsGuardPending) {
                    HomeNoticeCard(
                        message = stringResource(
                            R.string.susfs_guard_pending,
                            uiState.systemInfo.susfsGuardLoader.orEmpty(),
                        ),
                        icon = Icons.TwoTone.RestartAlt,
                        onClick = {
                            softRebootDialog.showConfirm(
                                title = softRebootLabel,
                                content = guardPendingConfirm,
                                confirm = softRebootLabel,
                            )
                        },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                StatusCard(
                    uiState = uiState,
                    onClickInstall = {
                        if (uiState.systemStatus.isLateLoadMode) return@StatusCard
                        navigator.push(Route.Install(preselectedKernelUri = null))
                    },
                    onClickJailbreak = {
                        loadingDialog.showLoading()
                        context.startService(Intent(context, MagicaService::class.java))
                        // Manager will be force-stopped and restarted by late-load on success.
                        // If that doesn't happen within timeout, jailbreak likely failed.
                        scope.launch(Dispatchers.IO) {
                            delay(30_000.milliseconds)
                            withContext(Dispatchers.Main) {
                                loadingDialog.hide()
                                Toast.makeText(
                                    context,
                                    R.string.jailbreak_timeout,
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }
                    },
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            ManagerUpdateCard(uiState.stableManagerUpdate)
            ManagerUpdateCard(uiState.betaManagerUpdate)
            if (uiState.isBetaManagerUpdateCheckFailed) {
                HomeNoticeCard(
                    message = stringResource(R.string.beta_update_check_failed),
                    icon = Icons.TwoTone.Error,
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (uiState.isExtendedDataLoaded) {
                InfoCard(
                    systemStatus = uiState.systemStatus,
                    systemInfo = uiState.systemInfo,
                    isSimpleMode = uiState.isSimpleMode,
                    showHomeCardIcons = uiState.showHomeCardIcons,
                )
            }

            // 链接卡片
            if (!uiState.isSimpleMode) {
                DonateCard(uiState.showHomeCardIcons)
                LearnMoreCard(uiState.showHomeCardIcons)
            }

            Spacer(Modifier.height(bottomPadding))
        }
    }
}

@Composable
private fun ManagerUpdateCard(update: ManagerUpdateInfo?) {
    val visibilityState = remember { MutableTransitionState(false) }
    var displayedUpdate by remember { mutableStateOf<ManagerUpdateInfo?>(null) }

    LaunchedEffect(update) {
        if (update != null) displayedUpdate = update
        visibilityState.targetState = update != null
    }

    AnimatedVisibility(
        visibleState = visibilityState,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        displayedUpdate?.let { updateInfo ->
            ManagerUpdateCardContent(updateInfo)
        }
    }
}

@Composable
private fun ManagerUpdateCardContent(updateInfo: ManagerUpdateInfo) {
    val context = LocalContext.current
    val permissionRequestInterface = LocalPermissionRequestInterface.current
    val enqueueManagerUpdate = koinInject<EnqueueManagerUpdateUseCase>()
    val channelTitle = stringResource(
        if (updateInfo.channel == ManagerUpdateChannel.STABLE) {
            R.string.manager_update_stable
        } else {
            R.string.manager_update_beta
        },
    )
    val message = if (updateInfo.channel == ManagerUpdateChannel.STABLE) {
        stringResource(R.string.new_version_available, updateInfo.versionCode)
    } else {
        stringResource(R.string.beta_version_available, updateInfo.versionCode)
    }
    val updateText = stringResource(R.string.module_update)
    val details = stringResource(
        R.string.manager_update_details,
        updateInfo.versionName,
        updateInfo.versionCode,
        updateInfo.abi,
    )
    val dialogContent = if (updateInfo.changelog.isBlank()) {
        details
    } else {
        "$details\n\n${updateInfo.changelog}"
    }
    val updateDialog = rememberConfirmDialog(
        onConfirm = {
            downloadManagerUpdate(
                context,
                permissionRequestInterface,
                updateInfo,
                enqueueManagerUpdate,
            )
        },
    )

    HomeNoticeCard(
        message = message,
        color = MiuixTheme.colorScheme.surfaceContainerHighest,
        contentColor = MiuixTheme.colorScheme.onSurfaceContainerHighest,
        icon = Icons.TwoTone.Info,
        onClick = {
            updateDialog.showConfirm(
                title = channelTitle,
                content = dialogContent,
                markdown = updateInfo.changelog.isNotBlank(),
                confirm = updateText,
            )
        },
    )

    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
fun RebootDropdownItems(
    items: Map<Int, String>,
    onReboot: (String) -> Unit,
    onDismiss: () -> Unit = {},
) {
    items.onEachIndexed { _, (id, reason) ->
        BasicComponent(
            title = stringResource(id),
            onClick = {
                onDismiss()
                onReboot(reason)
            },
        )
    }
}

@Composable
private fun TopBar(
    uiState: HomeUiState,
    onReboot: (String) -> Unit,
    scrollBehavior: ScrollBehavior? = null,
) {
    val themeConfig: ThemeConfig = koinInject()
    val cardConfig: CardConfig = koinInject()
    val navigator = LocalNavigator.current

    MiuixTopAppBar(
        modifier = Modifier.blurEffect(progressive = true),
        title = stringResource(R.string.app_name),
        color = if (themeConfig.isEnableBlur) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.surfaceContainer.copy(cardConfig.cardAlpha)
        },
        actions = {
            if (uiState.isCoreDataLoaded) {
                // SuSFS config entry — also reachable while the guard is
                // pending, so the status tab and its soft-reboot action stay
                // reachable before the guard ever loads.
                if (uiState.systemInfo.susfsVersionSupported || uiState.systemInfo.susfsGuardPending) {
                    MiuixIconButton(onClick = {
                        navigator.push(Route.SuSFSConfig)
                    }) {
                        Icon(
                            imageVector = Icons.TwoTone.Tune,
                            contentDescription = stringResource(R.string.susfs_config_setting_title),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                // Reboot menu
                var showDropdown by remember { mutableStateOf(false) }
                KsuIsValid(uiState.systemStatus) {
                    if (uiState.systemStatus.isRootAvailable) {
                        MiuixIconButton(onClick = {
                            showDropdown = true
                        }) {
                            Icon(
                                imageVector = Icons.TwoTone.PowerSettingsNew,
                                contentDescription = stringResource(id = R.string.reboot),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )

                            OverlayListPopup(
                                show = showDropdown,
                                onDismissRequest = { showDropdown = false },
                            ) {
                                ListPopupColumn {
                                    val pm =
                                        LocalContext.current.getSystemService(Context.POWER_SERVICE) as PowerManager?
                                    var methods = mapOf(
                                        R.string.reboot to "",
                                        R.string.reboot_soft to "soft_reboot",
                                        R.string.reboot_recovery to "recovery",
                                        R.string.reboot_bootloader to "bootloader",
                                        R.string.reboot_download to "download",
                                        R.string.reboot_edl to "edl",
                                    )

                                    @Suppress("DEPRECATION")
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && pm?.isRebootingUserspaceSupported == true) {
                                        methods = methods + (R.string.reboot_userspace to "userspace")
                                    }

                                    RebootDropdownItems(methods, onReboot) { showDropdown = false }
                                }
                            }
                        }
                    }
                }
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

/**
 * The home status card: working / not-installed / unsupported verdicts.
 */
@Composable
private fun StatusCard(
    uiState: HomeUiState,
    onClickInstall: () -> Unit = {},
    onClickJailbreak: () -> Unit = {},
) {
    val systemStatus = uiState.systemStatus
    val onClick = {
        if (systemStatus.isRootAvailable || systemStatus.kernelVersion.isGKI()) {
            onClickInstall()
        }
    }

    when {
        systemStatus.ksuVersion != null -> {
            val workingModeText = when {
                systemStatus.isSafeMode -> stringResource(id = R.string.safe_mode)
                else -> stringResource(id = R.string.home_working)
            }

            val workingModeSurfaceText = when {
                systemStatus.lkmMode == true -> "LKM"
                else -> "Built-in"
            }

            HomeCard {
                BasicComponent(
                    title = workingModeText,
                    summary = stringResource(
                        R.string.home_short_info,
                        uiState.systemInfo.superuserCount,
                        uiState.systemInfo.moduleCount,
                    ),
                    startAction = {
                        HomeCardIcon(Icons.TwoTone.TaskAlt)
                    },
                    endActions = {
                        // 工作模式标签
                        LabelText(
                            label = workingModeSurfaceText,
                            containerColor = MiuixTheme.colorScheme.primary,
                        )

                        if (systemStatus.isLateLoadMode) {
                            Spacer(Modifier.width(6.dp))
                            LabelText(
                                label = stringResource(id = R.string.jailbreak_mode),
                                containerColor = MiuixTheme.colorScheme.primary,
                            )
                        }

                        // 架构标签
                        if (Os.uname().machine != "aarch64") {
                            Spacer(Modifier.width(6.dp))
                            LabelText(
                                label = Os.uname().machine,
                                containerColor = MiuixTheme.colorScheme.primary,
                            )
                        }
                    },
                    onClick = onClick,
                )
            }
        }

        systemStatus.kernelVersion.isGKI() -> {
            val onErrorContainer = MiuixTheme.colorScheme.onErrorContainer
            HomeCard(
                color = MiuixTheme.colorScheme.errorContainer,
                contentColor = onErrorContainer,
            ) {
                BasicComponent(
                    title = stringResource(R.string.home_not_installed),
                    summary = stringResource(R.string.home_click_to_install),
                    titleColor = homeTitleColors(onErrorContainer),
                    summaryColor = homeSummaryColors(onErrorContainer),
                    startAction = {
                        HomeCardIcon(Icons.TwoTone.Warning)
                    },
                    onClick = onClick,
                    endActions = if (systemStatus.isSELinuxPermissive) {
                        {
                            Button(
                                onClick = onClickJailbreak,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                ),
                            ) {
                                Text(stringResource(R.string.home_jailbreak))
                            }
                        }
                    } else {
                        null
                    },
                )
            }
        }

        else -> {
            val onErrorContainer = MiuixTheme.colorScheme.onErrorContainer
            HomeCard(
                color = MiuixTheme.colorScheme.errorContainer,
                contentColor = onErrorContainer,
            ) {
                BasicComponent(
                    title = stringResource(R.string.home_unsupported),
                    summary = stringResource(R.string.home_unsupported_reason),
                    titleColor = homeTitleColors(onErrorContainer),
                    summaryColor = homeSummaryColors(onErrorContainer),
                    startAction = {
                        HomeCardIcon(Icons.TwoTone.Block)
                    },
                )
            }
        }
    }
}

@Composable
fun LearnMoreCard(
    showIcon: Boolean,
) {
    val uriHandler = LocalUriHandler.current
    val url = stringResource(R.string.home_learn_kernelsu_url)

    HomeSectionTitle(text = stringResource(R.string.learn_more))
    HomeCard {
        BasicComponent(
            title = stringResource(R.string.home_learn_kernelsu),
            summary = stringResource(R.string.home_click_to_learn_kernelsu),
            startAction = showIcon.toStartAction(Icons.AutoMirrored.TwoTone.MenuBook),
            endActions = { HomeArrowEndAction() },
            onClick = {
                uriHandler.openUri(url)
            },
        )
    }
}

@Composable
fun DonateCard(
    showIcon: Boolean,
) {
    val uriHandler = LocalUriHandler.current

    HomeSectionTitle(text = stringResource(R.string.home_support_title))
    HomeCard {
        BasicComponent(
            title = stringResource(R.string.home_support_title),
            summary = stringResource(R.string.home_support_content),
            startAction = showIcon.toStartAction(Icons.TwoTone.VolunteerActivism),
            endActions = { HomeArrowEndAction() },
            onClick = {
                uriHandler.openUri("https://patreon.com/weishu")
            },
        )
    }
}

@Composable
private fun InfoCard(
    systemStatus: KernelStatus,
    systemInfo: HomeSystemInfo,
    isSimpleMode: Boolean,
    showHomeCardIcons: Boolean,
) {
    val managersList = systemInfo.managersList

    HomeSectionTitle(text = stringResource(R.string.home_version_info))
    HomeCard {
        HomeInfoRow(
            icon = Icons.TwoTone.Smartphone.takeIf { showHomeCardIcons },
            title = stringResource(R.string.home_device_model),
            summary = systemInfo.deviceModel,
        )
        HomeInfoRow(
            icon = Icons.TwoTone.DeveloperBoard.takeIf { showHomeCardIcons },
            title = stringResource(R.string.home_kernel),
            summary = systemInfo.kernelRelease,
        )
        if (!isSimpleMode) {
            HomeInfoRow(
                icon = Icons.TwoTone.Android.takeIf { showHomeCardIcons },
                title = stringResource(R.string.home_android_version),
                summary = systemInfo.androidVersion,
            )
        }
        if (systemStatus.isManager) {
            HomeInfoRow(
                icon = Icons.TwoTone.Memory.takeIf { showHomeCardIcons },
                title = stringResource(R.string.home_kernel_version),
                summary = systemStatus.ksuFullVersion.orEmpty(),
            )
        }
        HomeInfoRow(
            icon = Icons.TwoTone.Tag.takeIf { showHomeCardIcons },
            title = stringResource(R.string.home_manager_version),
            summary = "${systemInfo.managerVersion.first} (${systemInfo.managerVersion.second}/${systemInfo.managerVersion.third})",
        )
        if (!isSimpleMode && systemInfo.susfsEnabled && systemInfo.susfsVersion.isNotEmpty()) {
            HomeInfoRow(
                icon = Icons.TwoTone.Settings.takeIf { showHomeCardIcons },
                title = stringResource(R.string.home_susfs_version),
                summary = systemInfo.susfsVersion,
            )
        }
    }

    HomeSectionTitle(text = stringResource(R.string.home_status_info))
    HomeCard {
        HomeInfoRow(
            icon = Icons.TwoTone.Security.takeIf { showHomeCardIcons },
            title = stringResource(R.string.home_selinux_status),
            summary = systemInfo.selinuxStatus,
        )

        val seccompDisplay = when (systemInfo.seccompStatus) {
            -1 -> stringResource(R.string.seccomp_status_not_supported)
            0 -> stringResource(R.string.seccomp_status_disabled)
            1 -> stringResource(R.string.seccomp_status_strict)
            2 -> stringResource(R.string.seccomp_status_filter)
            else -> stringResource(R.string.seccomp_status_unknown)
        }
        HomeInfoRow(
            icon = Icons.TwoTone.FilterList.takeIf { showHomeCardIcons },
            title = stringResource(R.string.home_seccomp_status),
            summary = seccompDisplay,
        )

        if (!isSimpleMode && managersList != null) {
            val signatureMap =
                managersList?.managers.orEmpty().groupBy { it.signatureIndex }
            val managersText = buildString {
                signatureMap.toSortedMap().forEach { (signatureIndex, managers) ->
                    append(managers.joinToString(", ") { "UID: ${it.uid}" })
                    append(" ")
                    append(
                        when (signatureIndex) {
                            0 -> "(${stringResource(R.string.app_name)})"

                            255 -> "(${stringResource(R.string.dynamic_managerature)})"

                            else -> if (signatureIndex >= 1) {
                                "(${
                                    stringResource(
                                        R.string.signature_index,
                                        signatureIndex,
                                    )
                                })"
                            } else {
                                "(${stringResource(R.string.unknown_signature)})"
                            }
                        },
                    )
                    append(" | ")
                }
            }.trimEnd(' ', '|')

            HomeInfoRow(
                icon = Icons.TwoTone.Group.takeIf { showHomeCardIcons },
                title = stringResource(R.string.multi_manager_list),
                summary = managersText.ifEmpty { stringResource(R.string.no_active_manager) },
            )
        }

        if (!isSimpleMode && systemStatus.isFullFeatured) {
            HomeInfoRow(
                icon = Icons.TwoTone.Tune.takeIf { showHomeCardIcons },
                title = stringResource(R.string.home_hook_type),
                summary = systemStatus.hookType,
            )
        }

        if (!isSimpleMode && systemInfo.zygiskImplement.isNotEmpty() && systemInfo.zygiskImplement != "None") {
            HomeInfoRow(
                icon = Icons.TwoTone.Extension.takeIf { showHomeCardIcons },
                title = stringResource(R.string.home_zygisk_implement),
                summary = systemInfo.zygiskImplement,
            )
        }

        if (!isSimpleMode && systemInfo.metaModuleImplement.isNotEmpty() && systemInfo.metaModuleImplement != "None") {
            HomeInfoRow(
                icon = Icons.TwoTone.Extension.takeIf { showHomeCardIcons },
                title = stringResource(R.string.home_meta_module_implement),
                summary = systemInfo.metaModuleImplement,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// miuix home building blocks
// ---------------------------------------------------------------------------

/**
 * A miuix card container for the home page. Applies the configured card
 * alpha so cards keep their translucency over custom backgrounds.
 */
@Composable
private fun HomeCard(
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.surfaceContainer,
    contentColor: Color = MiuixTheme.colorScheme.onSurfaceContainer,
    content: @Composable () -> Unit,
) {
    val cardConfig: CardConfig = koinInject()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = color.copy(alpha = cardConfig.cardAlpha),
            contentColor = contentColor,
        ),
        insideMargin = PaddingValues(0.dp),
    ) {
        content()
    }
}

/**
 * A notice row (warnings, update banners) rendered as a single miuix card.
 */
@Composable
private fun HomeNoticeCard(
    message: String,
    icon: ImageVector? = null,
    color: Color = MiuixTheme.colorScheme.errorContainer,
    contentColor: Color = MiuixTheme.colorScheme.onErrorContainer,
    onClick: (() -> Unit)? = null,
) {
    HomeCard(
        color = color,
        contentColor = contentColor,
    ) {
        BasicComponent(
            title = message,
            titleColor = homeTitleColors(contentColor),
            startAction = icon?.let { vector -> { HomeCardIcon(vector) } },
            onClick = onClick,
        )
    }
}

/**
 * One info row inside a home card group.
 */
@Composable
private fun HomeInfoRow(
    icon: ImageVector?,
    title: String,
    summary: String,
) {
    BasicComponent(
        title = title,
        summary = summary,
        startAction = icon?.let { vector -> { HomeCardIcon(vector) } },
    )
}

/**
 * Icon slot for home cards; hidden icons simply occupy no leading space.
 */
@Composable
private fun HomeCardIcon(icon: ImageVector) {
    MiuixIcon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
    )
}

/**
 * Section title aligned with the home card content.
 */
@Composable
private fun HomeSectionTitle(text: String) {
    SmallTitle(
        text = text,
        insideMargin = PaddingValues(start = 16.dp, top = 8.dp, bottom = 8.dp),
    )
}

/**
 * Title colors matching a card's content color (BasicComponent does not read
 * the card's LocalContentColor).
 */
@Composable
private fun homeTitleColors(contentColor: Color): BasicComponentColors = BasicComponentDefaults.titleColor(color = contentColor, disabledColor = contentColor)

/**
 * Summary colors matching a card's content color.
 */
@Composable
private fun homeSummaryColors(contentColor: Color): BasicComponentColors = BasicComponentDefaults.summaryColor(color = contentColor, disabledColor = contentColor)

/**
 * The miuix chevron shown on tappable link cards.
 */
@Composable
private fun HomeArrowEndAction() {
    MiuixIcon(
        imageVector = MiuixIcons.Basic.ArrowRight,
        contentDescription = null,
        modifier = Modifier
            .padding(end = 8.dp)
            .size(width = 10.dp, height = 16.dp),
        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
    )
}

private fun Boolean.toStartAction(
    icon: ImageVector,
): (@Composable () -> Unit)? = if (this) {
    { HomeCardIcon(icon) }
} else {
    null
}
