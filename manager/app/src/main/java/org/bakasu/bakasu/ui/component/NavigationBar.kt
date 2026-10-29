package org.bakasu.bakasu.ui.activity.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bakasu.bakasu.ui.component.FloatingBottomBar
import org.bakasu.bakasu.ui.component.FloatingBottomBarItem
import org.bakasu.bakasu.ui.screen.BottomBarDestination
import org.bakasu.bakasu.ui.theme.BottomBarStyle
import org.bakasu.bakasu.ui.theme.CardConfig
import org.bakasu.bakasu.ui.theme.ThemeConfig
import org.bakasu.bakasu.ui.theme.blurEffect
import org.bakasu.bakasu.ui.util.LocalBlurState
import org.bakasu.bakasu.ui.util.LocalHandlePageChange
import org.bakasu.bakasu.ui.util.LocalPagerState
import org.bakasu.bakasu.ui.util.LocalSelectedPage
import org.bakasu.bakasu.ui.viewmodel.HomeViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.basic.Badge as MiuixBadge
import top.yukonga.miuix.kmp.basic.BadgedBox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem as MiuixNavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationRail as MiuixNavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem as MiuixNavigationRailItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun NavigationBar(
    modifier: Modifier = Modifier,
    destinations: List<BottomBarDestination>,
    isBottomBar: Boolean,
) {
    val themeConfig: ThemeConfig = koinInject()
    val cardConfig: CardConfig = koinInject()
    val homeViewModel = koinViewModel<HomeViewModel>()
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val superuserCount = uiState.systemInfo.superuserCount
    val moduleCount = uiState.systemInfo.moduleCount
    val showNavigationBarBadge = uiState.showNavigationBarBadge
    val page = LocalSelectedPage.current
    val handlePageChange = LocalHandlePageChange.current
    val pagerState = LocalPagerState.current

    if (isBottomBar && themeConfig.bottomBarStyle == BottomBarStyle.FLOATING) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal),
                )
                .padding(
                    bottom = 12.dp + WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding(),
                ),
            contentAlignment = Alignment.Center,
        ) {
            FloatingBottomBar(
                selectedIndex = pagerState.targetPage,
                onSelected = { handlePageChange(it) },
                tabsCount = destinations.size,
                isBlurEnabled = LocalBlurState.current != null,
            ) { activateTab ->
                destinations.forEachIndexed { index, destination ->
                    FloatingBottomBarItem(
                        selected = index == pagerState.targetPage,
                        onClick = { activateTab(index) },
                        modifier = Modifier.defaultMinSize(minWidth = 76.dp),
                    ) {
                        val contentColor = LocalContentColor.current
                        BadgedBox(
                            badge = {
                                DestinationBadge(
                                    dest = destination,
                                    superUser = superuserCount,
                                    module = moduleCount,
                                    show = showNavigationBarBadge,
                                )
                            },
                        ) {
                            Icon(
                                imageVector = destination.iconSelected,
                                contentDescription = stringResource(destination.label),
                                tint = contentColor,
                            )
                        }
                        Text(
                            text = stringResource(destination.label),
                            color = contentColor,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible,
                        )
                    }
                }
            }
        }
    } else if (isBottomBar) {
        MiuixNavigationBar(
            modifier = modifier
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
                )
                .blurEffect(
                    compensateHorizontalOverscroll = true,
                    compensateVerticalOverscroll = true,
                    useFixedSurfaceBoundsForOverscroll = true,
                ),
            color = if (themeConfig.isEnableBlur) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.surfaceContainer.copy(cardConfig.cardAlpha)
            },
            mode = NavigationBarDisplayMode.IconWithSelectedLabel,
        ) {
            destinations.forEachIndexed { index, destination ->
                MiuixNavigationBarItem(
                    selected = index == page,
                    onClick = { handlePageChange(index) },
                    icon = if (index == page) {
                        destination.iconSelected
                    } else {
                        destination.iconNotSelected
                    },
                    label = stringResource(destination.label),
                    badge = {
                        DestinationBadge(
                            dest = destination,
                            superUser = superuserCount,
                            module = moduleCount,
                            show = showNavigationBarBadge,
                        )
                    },
                )
            }
        }
    } else {
        MiuixNavigationRail(
            modifier = modifier
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
                )
                .blurEffect(
                    compensateHorizontalOverscroll = true,
                    compensateVerticalOverscroll = false,
                    useFixedSurfaceBoundsForOverscroll = true,
                ),
            color = if (themeConfig.isEnableBlur) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.surfaceContainer.copy(cardConfig.cardAlpha)
            },
        ) {
            destinations.forEachIndexed { index, destination ->
                MiuixNavigationRailItem(
                    selected = index == page,
                    onClick = { handlePageChange(index) },
                    icon = if (index == page) {
                        destination.iconSelected
                    } else {
                        destination.iconNotSelected
                    },
                    label = stringResource(destination.label),
                    badge = {
                        DestinationBadge(
                            dest = destination,
                            superUser = superuserCount,
                            module = moduleCount,
                            show = showNavigationBarBadge,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun DestinationBadge(
    dest: BottomBarDestination,
    superUser: Int,
    module: Int,
    show: Boolean,
) {
    val count = when (dest) {
        BottomBarDestination.SuperUser -> superUser
        BottomBarDestination.Module -> module
        else -> 0
    }

    AnimatedVisibility(
        visible = count > 0 && show,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        MiuixBadge(
            containerColor = MiuixTheme.colorScheme.primary,
        ) {
            Text(count.toString())
        }
    }
}
