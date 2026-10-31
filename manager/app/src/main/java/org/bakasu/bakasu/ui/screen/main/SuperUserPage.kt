package org.bakasu.bakasu.ui.screen.main

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.Article
import androidx.compose.material.icons.twotone.Check
import androidx.compose.material.icons.twotone.ChevronRight
import androidx.compose.material.icons.twotone.MoreVert
import androidx.compose.material.icons.twotone.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.bakasu.bakasu.R
import org.bakasu.bakasu.domain.model.AllowlistOperationResult
import org.bakasu.bakasu.domain.model.InstalledAppGroup
import org.bakasu.bakasu.domain.model.SuperUserLoadFailure
import org.bakasu.bakasu.ui.component.ConfirmResult
import org.bakasu.bakasu.ui.component.PackageIcon
import org.bakasu.bakasu.ui.component.SearchAppBar
import org.bakasu.bakasu.ui.component.SwipeableSnackbarHost
import org.bakasu.bakasu.ui.component.WarningCard
import org.bakasu.bakasu.ui.component.rememberConfirmDialog
import org.bakasu.bakasu.ui.component.rememberSearchAppBarScrollBehavior
import org.bakasu.bakasu.ui.component.settings.SettingsValueRow
import org.bakasu.bakasu.ui.component.settings.lazySegmentColumn
import org.bakasu.bakasu.ui.navigation.LocalNavigator
import org.bakasu.bakasu.ui.navigation.Route
import org.bakasu.bakasu.ui.screen.LabelText
import org.bakasu.bakasu.ui.theme.blurSource
import org.bakasu.bakasu.ui.util.LocalPagerPage
import org.bakasu.bakasu.ui.util.LocalPagerState
import org.bakasu.bakasu.ui.util.LocalSnackbarHost
import org.bakasu.bakasu.ui.util.adaptiveScaffoldWindowInsets
import org.bakasu.bakasu.ui.util.showReplacingSnackbar
import org.bakasu.bakasu.ui.viewmodel.SortType
import org.bakasu.bakasu.ui.viewmodel.SuperUserUiAction
import org.bakasu.bakasu.ui.viewmodel.SuperUserUiEvent
import org.bakasu.bakasu.ui.viewmodel.SuperUserUiState
import org.bakasu.bakasu.ui.viewmodel.SuperUserViewModel
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.SnackbarDuration
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.popup.OverlayDropdownPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperUserPage(bottomPadding: Dp) {
    val context = LocalContext.current
    val viewModel = koinViewModel<SuperUserViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerPage = LocalPagerPage.current
    val isPageVisible = pagerPage == null || LocalPagerState.current.currentPage == pagerPage
    val scope = rememberCoroutineScope()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = rememberSearchAppBarScrollBehavior(
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState),
    )
    val listState = rememberLazyListState()
    val snackBarHostState = LocalSnackbarHost.current

    var showDropdown by remember { mutableStateOf(false) }
    val restoreConfirmDialog = rememberConfirmDialog()
    val restoreConfirmTitle = stringResource(R.string.allowlist_restore_confirm_title)
    val restoreConfirmMessage = stringResource(R.string.allowlist_restore_confirm_message)
    val confirmText = stringResource(R.string.confirm)
    val cancelText = stringResource(R.string.cancel)

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SuperUserUiEvent.AllowlistOperationFinished -> {
                    val successMessage = if (event.restore) {
                        R.string.allowlist_restore_success
                    } else {
                        R.string.allowlist_backup_success
                    }
                    val failureMessage = if (event.restore) {
                        R.string.allowlist_restore_failed
                    } else {
                        R.string.allowlist_backup_failed
                    }
                    snackBarHostState.showReplacingSnackbar(
                        message = context.allowlistOperationMessage(
                            result = event.result,
                            successMessage = successMessage,
                            failureMessage = failureMessage,
                        ),
                        duration = SnackbarDuration.Long,
                    )
                }
            }
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) {
            viewModel.dispatch(SuperUserUiAction.BackupAllowlist(uri.toString()))
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val confirmed = restoreConfirmDialog.awaitConfirm(
                    title = restoreConfirmTitle,
                    content = restoreConfirmMessage,
                    confirm = confirmText,
                    dismiss = cancelText,
                )
                if (confirmed != ConfirmResult.Confirmed) return@launch

                viewModel.dispatch(SuperUserUiAction.RestoreAllowlist(uri.toString()))
            }
        }
    }

    val navigator = LocalNavigator.current

    LaunchedEffect(Unit) {
        viewModel.dispatch(SuperUserUiAction.Search(""))
    }

    LaunchedEffect(viewModel, isPageVisible) {
        if (isPageVisible) viewModel.dispatch(SuperUserUiAction.LoadInitialData)
    }

    MiuixScaffold(
        topBar = {
            SearchAppBar(
                title = stringResource(R.string.superuser),
                searchText = uiState.search,
                onSearchTextChange = { viewModel.dispatch(SuperUserUiAction.Search(it)) },
                dropdownContent = {
                    SuperUserDropdown(
                        expanded = showDropdown,
                        onExpandedChange = { showDropdown = it },
                        viewModel = viewModel,
                        uiState = uiState,
                        onBackupAllowlist = {
                            backupLauncher.launch(createAllowlistBackupFileName())
                        },
                        onRestoreAllowlist = {
                            restoreLauncher.launch(arrayOf("application/octet-stream"))
                        },
                    )
                },
                navigationContent = {
                    MiuixIconButton(onClick = {
                        navigator.push(Route.Sulog)
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.TwoTone.Article,
                            contentDescription = stringResource(R.string.sulog),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                searchBarPlaceHolderText = stringResource(R.string.search_apps),
            )
        },
        containerColor = Color.Transparent,
        snackbarHost = {
            SwipeableSnackbarHost(
                modifier = Modifier.padding(bottom = bottomPadding),
                hostState = snackBarHostState,
            )
        },
        contentWindowInsets = adaptiveScaffoldWindowInsets(includeBottom = false),
    ) { innerPadding ->
        SuperUserContent(
            innerPadding = innerPadding,
            viewModel = viewModel,
            uiState = uiState,
            listState = listState,
            scrollBehavior = scrollBehavior,
            bottomPadding = bottomPadding,
        )
    }
}

private fun Context.allowlistOperationMessage(
    result: AllowlistOperationResult,
    successMessage: Int,
    failureMessage: Int,
): String = when (result) {
    AllowlistOperationResult.Success ->
        getString(successMessage)

    AllowlistOperationResult.InvalidFile ->
        getString(failureMessage, getString(R.string.unknown_file))

    AllowlistOperationResult.UnsupportedVersion ->
        getString(failureMessage, getString(R.string.home_unsupported))

    is AllowlistOperationResult.ProfileUpdateFailed ->
        getString(
            failureMessage,
            getString(R.string.failed_to_update_app_profile, result.uid.toString()),
        )

    is AllowlistOperationResult.Failed ->
        getString(
            failureMessage,
            result.cause?.localizedMessage ?: getString(R.string.unknown),
        )
}

@Composable
private fun SuperUserLoadError(failure: SuperUserLoadFailure, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val message = stringResource(failure.resId)
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WarningCard(message = message)
        TextButton(
            text = stringResource(R.string.network_retry),
            onClick = onRetry,
        )
    }
}

private fun createAllowlistBackupFileName(): String {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    return "ksu_allowlist_backup_$timestamp.dat"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuperUserContent(
    innerPadding: PaddingValues,
    viewModel: SuperUserViewModel,
    uiState: SuperUserUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    scrollBehavior: TopAppBarScrollBehavior,
    bottomPadding: Dp,
) {
    val navigator = LocalNavigator.current
    val pullRefreshState = rememberPullToRefreshState()

    if (uiState.appGroupList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blurSource(),
            contentAlignment = Alignment.Center,
        ) {
            if (uiState.isRefreshing) {
                InfiniteProgressIndicator(color = MiuixTheme.colorScheme.primary)
            } else if (uiState.loadFailure != null || uiState.search.isEmpty()) {
                SuperUserLoadError(
                    failure = uiState.loadFailure ?: SuperUserLoadFailure.PACKAGE_LOAD_FAILED,
                    onRetry = { viewModel.dispatch(SuperUserUiAction.Refresh) },
                    modifier = Modifier.padding(innerPadding).padding(16.dp),
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.TwoTone.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(96.dp)
                            .padding(bottom = 16.dp),
                    )
                    Text(
                        text = stringResource(R.string.no_apps_found),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        return
    }

    PullToRefresh(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.dispatch(SuperUserUiAction.Refresh) },
        modifier = Modifier
            .fillMaxSize()
            .blurSource(),
        pullToRefreshState = pullRefreshState,
        contentPadding = PaddingValues(top = innerPadding.calculateTopPadding()),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        ) {
            item {
                Spacer(modifier = Modifier.height(innerPadding.calculateTopPadding()))
            }
            uiState.loadFailure?.let { failure ->
                item(key = "load-error") {
                    SuperUserLoadError(
                        failure = failure,
                        onRetry = { viewModel.dispatch(SuperUserUiAction.Refresh) },
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            lazySegmentColumn(
                items = uiState.appGroupList,
                key = { _, appGroup -> "${appGroup.uid}-${appGroup.profileKey}" },
                contentType = { _, appGroup -> "${appGroup.uid}-${appGroup.profileKey}" },
            ) { _, appGroup ->
                AppGroupItem(
                    appGroup = appGroup,
                    isManager = appGroup.uid in uiState.managerUids,
                ) {
                    navigator.push(Route.AppProfile(appGroup.uid, appGroup.profileKey))
                }
            }

            item {
                Spacer(modifier = Modifier.height(bottomPadding + innerPadding.calculateBottomPadding() + 15.dp))
            }
        }
    }
}

@Composable
private fun SuperUserDropdown(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    viewModel: SuperUserViewModel,
    uiState: SuperUserUiState,
    onBackupAllowlist: () -> Unit,
    onRestoreAllowlist: () -> Unit,
) {
    // Strings are hoisted out of the remember block: stringResource is a
    // composable call and may not run inside the calculation lambda.
    val sortLabels = SortType.entries.associateWith { sortType ->
        stringResource(sortType.displayNameRes)
    }
    val reverseOrderLabel = stringResource(R.string.reverse_order)
    val showSystemAppsLabel = stringResource(R.string.show_system_apps)
    val backupLabel = stringResource(R.string.backup_allowlist)
    val restoreLabel = stringResource(R.string.restore_allowlist)

    // The app-standard menu family: the popup hangs on the Box that wraps the
    // icon button (same anchoring as OverlayIconDropdownMenu internals), the
    // rows render through the battle-tested DropdownImpl and groups are
    // separated by the built-in entry divider. Selection rows keep the menu
    // open (collapseOnSelection = false); the backup/restore actions close it
    // manually before launching the document pickers.
    val entries = remember(
        sortLabels,
        uiState.currentSortType,
        uiState.reverseOrder,
        uiState.showSystemApps,
        reverseOrderLabel,
        showSystemAppsLabel,
        backupLabel,
        restoreLabel,
    ) {
        listOf(
            DropdownEntry(
                items = SortType.entries.map { sortType ->
                    DropdownItem(
                        text = sortLabels.getValue(sortType),
                        selected = uiState.currentSortType == sortType,
                        onClick = {
                            viewModel.dispatch(SuperUserUiAction.SetSort(sortType))
                        },
                    )
                },
            ),
            DropdownEntry(
                items = listOf(
                    DropdownItem(
                        text = reverseOrderLabel,
                        selected = uiState.reverseOrder,
                        onClick = {
                            viewModel.dispatch(
                                SuperUserUiAction.SetReverseOrder(!uiState.reverseOrder),
                            )
                        },
                    ),
                    DropdownItem(
                        text = showSystemAppsLabel,
                        selected = uiState.showSystemApps,
                        onClick = {
                            viewModel.dispatch(
                                SuperUserUiAction.SetShowSystemApps(!uiState.showSystemApps),
                            )
                        },
                    ),
                    DropdownItem(
                        text = backupLabel,
                        onClick = {
                            onExpandedChange(false)
                            onBackupAllowlist()
                        },
                    ),
                    DropdownItem(
                        text = restoreLabel,
                        onClick = {
                            onExpandedChange(false)
                            onRestoreAllowlist()
                        },
                    ),
                ),
            ),
        )
    }

    Box {
        MiuixIconButton(onClick = { onExpandedChange(true) }) {
            Icon(
                imageVector = Icons.TwoTone.MoreVert,
                contentDescription = stringResource(id = R.string.settings),
            )
        }

        OverlayDropdownPopup(
            entries = entries,
            show = expanded,
            onDismiss = { onExpandedChange(false) },
            onDismissFinished = {},
            maxHeight = null,
            dropdownColors = DropdownDefaults.dropdownColors(),
            renderInRootScaffold = true,
            collapseOnSelection = false,
        )
    }
}

@Composable
private fun AppGroupItem(
    appGroup: InstalledAppGroup,
    isManager: Boolean,
    onClick: () -> Unit,
) {
    val mainApp = appGroup.mainApp
    SettingsValueRow(
        onClick = {
            onClick()
        },
        title = mainApp.label,
        summary = if (appGroup.apps.size > 1) {
            stringResource(R.string.group_contains_apps, appGroup.apps.size)
        } else {
            mainApp.displayIdentifier
        },
        bottomContent = {
            Spacer(modifier = Modifier.height(5.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (appGroup.allowSu) {
                    LabelText(label = "ROOT")
                } else {
                    if (appGroup.shouldUmount) {
                        LabelText(
                            label = "UMOUNT",
                            containerColor = MiuixTheme.colorScheme.secondaryContainer,
                            contentColor = MiuixTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                if (appGroup.hasCustomProfile) {
                    LabelText(
                        label = "CUSTOM",
                        containerColor = MiuixTheme.colorScheme.tertiaryContainer,
                        contentColor = MiuixTheme.colorScheme.onTertiaryContainer,
                    )
                } else if (!appGroup.allowSu) {
                    LabelText(
                        label = "DEFAULT",
                        containerColor = MiuixTheme.colorScheme.primaryContainer,
                        contentColor = MiuixTheme.colorScheme.onPrimaryContainer,
                    )
                }
                if (isManager) {
                    LabelText(
                        label = "MANAGER",
                        containerColor = MiuixTheme.colorScheme.errorContainer,
                        contentColor = MiuixTheme.colorScheme.onErrorContainer,
                    )
                }
                if (appGroup.apps.size > 1) {
                    appGroup.userName?.let {
                        LabelText(
                            label = it,
                            containerColor = MiuixTheme.colorScheme.secondaryContainer,
                            contentColor = MiuixTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                if (appGroup.isRecentlyInstalled) {
                    LabelText(
                        label = stringResource(R.string.recently_installed),
                        containerColor = MiuixTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MiuixTheme.colorScheme.onSurfaceContainerHighest,
                    )
                }
            }
        },
        leadingContent = {
            PackageIcon(
                packageName = if (appGroup.isWebViewZygote) "android" else mainApp.packageName,
                contentDescription = mainApp.label,
                modifier = Modifier
                    .padding(4.dp)
                    .size(48.dp),
            )
        },
        endContent = {
            Icon(
                imageVector = Icons.TwoTone.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        },
    )
}
