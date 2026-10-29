package org.bakasu.bakasu.ui.screen.susfs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.bakasu.bakasu.R
import org.bakasu.bakasu.ui.component.HorizontalPagerWithInteraction
import org.bakasu.bakasu.ui.component.SwipeableSnackbarHost
import org.bakasu.bakasu.ui.component.settings.AppBackButton
import org.bakasu.bakasu.ui.navigation.LocalNavigator
import org.bakasu.bakasu.ui.screen.susfs.subpages.OpenRedirectTab
import org.bakasu.bakasu.ui.screen.susfs.subpages.StandardFeaturesTab
import org.bakasu.bakasu.ui.screen.susfs.subpages.StatusTab
import org.bakasu.bakasu.ui.screen.susfs.subpages.SusKstatTab
import org.bakasu.bakasu.ui.screen.susfs.subpages.SusMapTab
import org.bakasu.bakasu.ui.screen.susfs.subpages.SusPathTab
import org.bakasu.bakasu.ui.theme.CardConfig
import org.bakasu.bakasu.ui.theme.ThemeConfig
import org.bakasu.bakasu.ui.theme.blurEffect
import org.bakasu.bakasu.ui.theme.blurSource
import org.bakasu.bakasu.ui.util.ActivityResumeEffect
import org.bakasu.bakasu.ui.util.LocalSnackbarHost
import org.bakasu.bakasu.ui.util.adaptiveScaffoldWindowInsets
import org.bakasu.bakasu.ui.util.showReplacingSnackbar
import org.bakasu.bakasu.ui.viewmodel.SuSFSUiAction
import org.bakasu.bakasu.ui.viewmodel.SuSFSUiEvent
import org.bakasu.bakasu.ui.viewmodel.SuSFSViewModel
import org.bakasu.bakasu.ui.viewmodel.awaitSuSFSBoolean
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState

private class SuSFSConfigSubpage(
    val requirePersist: Boolean,
    val title: String,
    val content: @Composable (PaddingValues, NestedScrollConnection) -> Unit,
)

@Composable
fun SuSFSConfigScreen() {
    val themeConfig: ThemeConfig = koinInject()
    val cardConfig: CardConfig = koinInject()
    val configHelper = koinViewModel<SuSFSViewModel>()
    val navigator = LocalNavigator.current
    val snackBarHost = LocalSnackbarHost.current
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var configEnabled by remember { mutableStateOf<Boolean?>(null) }
    val scrollBehavior = MiuixScrollBehavior()
    val operationFailedMsg = stringResource(R.string.susfs_operation_failed)
    val pullRefreshState = rememberPullToRefreshState()
    val refreshCoordinator = remember(coroutineScope, configHelper) {
        SuSFSRefreshCoordinator(coroutineScope, configHelper)
    }
    val onRegisterRefresh: SuSFSRefreshRegistrar = remember(refreshCoordinator) {
        refreshCoordinator::register
    }

    LaunchedEffect(configHelper) {
        configHelper.events.collectLatest { event ->
            when (event) {
                is SuSFSUiEvent.Error -> if (event.message.isNotBlank()) {
                    snackBarHost.showReplacingSnackbar(event.message)
                }
            }
        }
    }

    fun requestRefresh() {
        if (isRefreshing) return
        isRefreshing = true
        coroutineScope.launch {
            try {
                refreshCoordinator.refresh(forceRefresh = true) { config ->
                    configEnabled = config.enabled
                }
            } finally {
                isRefreshing = false
            }
        }
    }

    val handleConfigEnabledChange: (Boolean) -> Unit = { newValue ->
        coroutineScope.launch {
            if (awaitSuSFSBoolean(configHelper) { reply ->
                    SuSFSUiAction.SetEnabled(newValue, reply)
                }
            ) {
                configEnabled = newValue
            } else {
                snackBarHost.showReplacingSnackbar(operationFailedMsg)
            }
        }
    }

    val subpages = listOf(
        SuSFSConfigSubpage(
            requirePersist = false,
            title = stringResource(R.string.susfs_tab_status),
        ) { innerPadding, nestedScrollConnection ->
            StatusTab(
                nestedScrollConnection = nestedScrollConnection,
                innerPadding = innerPadding,
                onRegisterRefresh = onRegisterRefresh,
                configEnabled = configEnabled ?: false,
                configEnabledLoaded = configEnabled != null,
                onConfigEnabledChange = handleConfigEnabledChange,
                onConfigRestored = ::requestRefresh,
            )
        },
        SuSFSConfigSubpage(
            requirePersist = true,
            title = stringResource(R.string.susfs_tab_standard),
        ) { innerPadding, nestedScrollConnection ->
            StandardFeaturesTab(
                nestedScrollConnection = nestedScrollConnection,
                innerPadding = innerPadding,
                onRegisterRefresh = onRegisterRefresh,
            )
        },
        SuSFSConfigSubpage(
            requirePersist = true,
            title = stringResource(R.string.susfs_tab_sus_path),
        ) { innerPadding, nestedScrollConnection ->
            SusPathTab(
                nestedScrollConnection = nestedScrollConnection,
                innerPadding = innerPadding,
                onRegisterRefresh = onRegisterRefresh,
            )
        },
        SuSFSConfigSubpage(
            requirePersist = true,
            title = stringResource(R.string.susfs_tab_sus_kstat),
        ) { innerPadding, nestedScrollConnection ->
            SusKstatTab(
                nestedScrollConnection = nestedScrollConnection,
                innerPadding = innerPadding,
                onRegisterRefresh = onRegisterRefresh,
            )
        },
        SuSFSConfigSubpage(
            requirePersist = true,
            title = stringResource(R.string.susfs_tab_open_redirect),
        ) { innerPadding, nestedScrollConnection ->
            OpenRedirectTab(
                nestedScrollConnection = nestedScrollConnection,
                innerPadding = innerPadding,
                onRegisterRefresh = onRegisterRefresh,
            )
        },
        SuSFSConfigSubpage(
            requirePersist = true,
            title = stringResource(R.string.susfs_tab_sus_map),
        ) { innerPadding, nestedScrollConnection ->
            SusMapTab(
                nestedScrollConnection = nestedScrollConnection,
                innerPadding = innerPadding,
                onRegisterRefresh = onRegisterRefresh,
            )
        },
    )
    val defaultPage = subpages.indexOfFirst { !it.requirePersist }.coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = defaultPage,
        pageCount = { subpages.size },
    )
    val visibleSubpages = subpages.withIndex().filter { (_, subpage) ->
        !subpage.requirePersist || configEnabled == true
    }
    val selectedTabIndex = visibleSubpages
        .indexOfFirst { it.index == pagerState.currentPage }
        .coerceAtLeast(0)

    LaunchedEffect(Unit) {
        scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffsetLimit
    }

    ActivityResumeEffect(refreshCoordinator) {
        refreshCoordinator.refresh(forceRefresh = true) { config ->
            configEnabled = config.enabled
        }
    }

    LaunchedEffect(configEnabled, defaultPage) {
        if (configEnabled == false && subpages[pagerState.currentPage].requirePersist) {
            pagerState.scrollToPage(defaultPage)
        }
    }

    MiuixScaffold(
        topBar = {
            Column(modifier = Modifier.blurEffect()) {
                MiuixTopAppBar(
                    title = stringResource(R.string.susfs_config_title),
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        AppBackButton(
                            onClick = {
                                navigator.pop()
                            },
                        )
                    },
                    color =
                        if (themeConfig.isEnableBlur) {
                            Color.Transparent
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer.copy(cardConfig.cardAlpha)
                        },
                )

                TabRowWithContour(
                    tabs = visibleSubpages.map { it.value.title },
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { index ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(visibleSubpages[index].index)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                BackHandler(
                    enabled = pagerState.currentPage != defaultPage,
                ) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(defaultPage)
                    }
                }
            }
        },
        containerColor = Color.Transparent,
        contentWindowInsets = adaptiveScaffoldWindowInsets(),
        snackbarHost = { SwipeableSnackbarHost(hostState = snackBarHost) },
    ) { innerPadding ->
        PullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = ::requestRefresh,
            modifier = Modifier
                .fillMaxSize()
                .blurSource(),
            pullToRefreshState = pullRefreshState,
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                HorizontalPagerWithInteraction(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = configEnabled == true,
                ) { page ->
                    subpages[page].content(innerPadding, scrollBehavior.nestedScrollConnection)
                }
            }
        }
    }
}
