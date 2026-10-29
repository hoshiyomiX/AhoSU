package org.bakasu.bakasu.ui.component

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import org.bakasu.bakasu.ui.component.settings.AppBackButton
import org.bakasu.bakasu.ui.theme.CardConfig
import org.bakasu.bakasu.ui.theme.ThemeConfig
import org.bakasu.bakasu.ui.theme.blurEffect
import org.bakasu.bakasu.ui.theme.renderBackgroundBlur
import org.bakasu.bakasu.ui.util.LocalPagerPage
import org.bakasu.bakasu.ui.util.LocalSelectedPage
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val SearchBarInitialHeight = 77.dp

@OptIn(ExperimentalMaterial3Api::class)
@Stable
class SearchAppBarScrollBehavior internal constructor(
    private val topAppBarScrollBehavior: TopAppBarScrollBehavior,
    searchBarHeightState: MutableFloatState,
    searchBarHeightOffsetState: MutableFloatState,
) : TopAppBarScrollBehavior {
    private var searchBarHeight by searchBarHeightState
    private var searchBarHeightOffset by searchBarHeightOffsetState
    private var lastSearchBarScrollDelta = 0f
    private var isUserScrollInProgress = false
    private var ignoreCurrentScroll = false

    val searchBarExpandedFraction: Float
        get() = (1f + searchBarHeightOffset / searchBarHeight).coerceIn(0f, 1f)

    internal fun updateSearchBarHeight(height: Float) {
        // Keep the last measured height while AnimatedVisibility has no content.
        if (height <= 0f || height == searchBarHeight) return
        val fraction = searchBarExpandedFraction
        searchBarHeight = height
        searchBarHeightOffset = -height * (1f - fraction)
    }

    override val state = topAppBarScrollBehavior.state
    override val isPinned = topAppBarScrollBehavior.isPinned
    override val snapAnimationSpec: AnimationSpec<Float>?
        get() = topAppBarScrollBehavior.snapAnimationSpec
    override val flingAnimationSpec: DecayAnimationSpec<Float>?
        get() = topAppBarScrollBehavior.flingAnimationSpec

    fun expandSearchBar() {
        ignoreCurrentScroll = isUserScrollInProgress
        searchBarHeightOffset = 0f
        lastSearchBarScrollDelta = 0f
    }

    fun collapseSearchBar() {
        searchBarHeightOffset = -searchBarHeight
        lastSearchBarScrollDelta = 0f
        isUserScrollInProgress = false
        ignoreCurrentScroll = false
    }

    fun reset() {
        searchBarHeightOffset = 0f
        lastSearchBarScrollDelta = 0f
        isUserScrollInProgress = false
        ignoreCurrentScroll = false
    }

    private fun consumeSearchBarScroll(delta: Float): Float {
        val previousOffset = searchBarHeightOffset
        searchBarHeightOffset = (searchBarHeightOffset + delta).coerceIn(-searchBarHeight, 0f)
        val consumed = searchBarHeightOffset - previousOffset
        if (consumed != 0f) lastSearchBarScrollDelta = consumed
        return consumed
    }

    private suspend fun animateSearchBarTo(targetOffset: Float, initialVelocity: Float = 0f) {
        if (searchBarHeightOffset == targetOffset) return

        AnimationState(
            initialValue = searchBarHeightOffset,
            initialVelocity = initialVelocity,
        ).animateTo(
            targetValue = targetOffset,
            animationSpec = snapAnimationSpec ?: spring(),
        ) {
            searchBarHeightOffset = value.coerceIn(-searchBarHeight, 0f)
        }
        lastSearchBarScrollDelta = 0f
    }

    override val nestedScrollConnection: NestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    isUserScrollInProgress = true
                }
                if (ignoreCurrentScroll || available.y >= 0f) {
                    return topAppBarScrollBehavior.nestedScrollConnection.onPreScroll(
                        available,
                        source,
                    )
                }

                val previousOffset = searchBarHeightOffset
                consumeSearchBarScroll(available.y)
                if (previousOffset != searchBarHeightOffset) {
                    return available.copy(x = 0f)
                }

                return topAppBarScrollBehavior.nestedScrollConnection.onPreScroll(
                    available,
                    source,
                )
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val topAppBarConsumed =
                    topAppBarScrollBehavior.nestedScrollConnection.onPostScroll(
                        consumed,
                        available,
                        source,
                    )

                if (ignoreCurrentScroll || available.y <= 0f) return topAppBarConsumed

                val remainingY = available.y - topAppBarConsumed.y
                val searchBarConsumed = consumeSearchBarScroll(remainingY)
                return topAppBarConsumed + Offset(0f, searchBarConsumed)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (ignoreCurrentScroll) {
                    return topAppBarScrollBehavior.nestedScrollConnection.onPreFling(available)
                }
                if (available.y < 0f && searchBarHeightOffset > -searchBarHeight) {
                    animateSearchBarTo(
                        targetOffset = -searchBarHeight,
                        initialVelocity = available.y,
                    )
                    return available.copy(x = 0f)
                }

                return topAppBarScrollBehavior.nestedScrollConnection.onPreFling(available)
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity {
                isUserScrollInProgress = false
                if (ignoreCurrentScroll) {
                    ignoreCurrentScroll = false
                    return topAppBarScrollBehavior.nestedScrollConnection.onPostFling(
                        consumed,
                        available,
                    )
                }

                if (searchBarHeightOffset > -searchBarHeight && searchBarHeightOffset < 0f) {
                    val shouldExpand =
                        available.y > 0f ||
                            (available.y == 0f && lastSearchBarScrollDelta > 0f)
                    animateSearchBarTo(
                        targetOffset = if (shouldExpand) 0f else -searchBarHeight,
                        initialVelocity = available.y,
                    )
                    return available.copy(x = 0f)
                }

                val topAppBarConsumed =
                    topAppBarScrollBehavior.nestedScrollConnection.onPostFling(
                        consumed,
                        available,
                    )
                val remainingY = available.y - topAppBarConsumed.y
                if (
                    remainingY > 0f &&
                    state.collapsedFraction < 0.01f &&
                    searchBarHeightOffset < 0f
                ) {
                    animateSearchBarTo(
                        targetOffset = 0f,
                        initialVelocity = remainingY,
                    )
                    return Velocity(topAppBarConsumed.x, available.y)
                }
                return topAppBarConsumed
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberSearchAppBarScrollBehavior(
    topAppBarScrollBehavior: TopAppBarScrollBehavior,
): SearchAppBarScrollBehavior {
    val density = LocalDensity.current
    val searchBarHeightOffset = rememberSaveable(density) { mutableFloatStateOf(0f) }
    val initialSearchBarHeight = with(density) { SearchBarInitialHeight.toPx() }
    val searchBarHeight = rememberSaveable(density) { mutableFloatStateOf(initialSearchBarHeight) }
    return remember(topAppBarScrollBehavior, density) {
        SearchAppBarScrollBehavior(
            topAppBarScrollBehavior = topAppBarScrollBehavior,
            searchBarHeightState = searchBarHeight,
            searchBarHeightOffsetState = searchBarHeightOffset,
        )
    }
}

private fun Modifier.collapseWithTopAppBar(scrollBehavior: SearchAppBarScrollBehavior?): Modifier = clipToBounds().layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0))
    scrollBehavior?.updateSearchBarHeight(placeable.height.toFloat())
    val fraction = scrollBehavior?.searchBarExpandedFraction ?: 1f
    val visibleHeight = (placeable.height * fraction).roundToInt()

    layout(placeable.width, visibleHeight) {
        placeable.placeRelative(
            x = 0,
            y = visibleHeight - placeable.height,
        )
    }
}

@Composable
private fun CompactSearchBar(
    modifier: Modifier = Modifier,
    onSearch: (String) -> Unit,
    textFieldState: TextFieldState,
    searchBarPlaceHolderText: String,
    requestFocus: Boolean = false,
    onFocusRequestHandled: () -> Unit = {},
) {
    val themeConfig: ThemeConfig = koinInject()
    val cardConfig: CardConfig = koinInject()
    val focusManager = LocalFocusManager.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()

    val isImeVisible = WindowInsets.isImeVisible
    val hasFocusReassignBug = Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1
    var allowFocus by remember { mutableStateOf(!hasFocusReassignBug) }
    var expanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // The miuix InputField contract ties the label (placeholder) visibility to the
    // collapsed state, so the expansion flag tracks the input focus: the label is
    // shown whenever the field is empty and not focused.
    LaunchedEffect(focused) {
        if (!focused) {
            expanded = false
        }
    }

    LaunchedEffect(pressed) {
        if (pressed && hasFocusReassignBug && !allowFocus) {
            allowFocus = true
        }
    }

    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            allowFocus = true
            delay(100.milliseconds)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                focusRequester.requestFocus()
            }
            onFocusRequestHandled()
        }
    }

    LaunchedEffect(focused) {
        if (!focused && hasFocusReassignBug) {
            allowFocus = false
        }
    }

    LaunchedEffect(isImeVisible) {
        if (!isImeVisible && focused) {
            if (hasFocusReassignBug) {
                allowFocus = false
                delay(100.milliseconds)
            }
            focusManager.clearFocus()
        }
    }

    BackHandler(enabled = textFieldState.text.isNotEmpty()) {
        textFieldState.clearText()
    }

    InputField(
        query = textFieldState.text.toString(),
        onQueryChange = { newValue ->
            textFieldState.edit {
                replace(0, length, newValue)
            }
        },
        onSearch = onSearch,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
            .heightIn(0.dp, 45.dp)
            .focusProperties { canFocus = allowFocus }
            .focusRequester(focusRequester),
        label = searchBarPlaceHolderText,
        color = if (themeConfig.isEnableBlurExp) {
            Color.Transparent
        } else {
            MiuixTheme.colorScheme.surfaceContainerHighest.copy(alpha = cardConfig.cardAlpha)
        },
        interactionSource = interactionSource,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchAppBar(
    title: String,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onBackClick: (() -> Unit)? = null,
    dropdownContent: @Composable (() -> Unit)? = null,
    navigationContent: @Composable (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    searchBarPlaceHolderText: String,
    preserveStateOnNavigation: Boolean = false,
    searchContext: String? = null,
) {
    val themeConfig: ThemeConfig = koinInject()
    val cardConfig: CardConfig = koinInject()
    val textFieldState = key(searchContext) { rememberTextFieldState(initialText = searchText) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateAsState()
    val pagerPage = LocalPagerPage.current
    val isCurrentPage = pagerPage == null || pagerPage == LocalSelectedPage.current
    val currentIsCurrentPage by rememberUpdatedState(isCurrentPage)
    val isPageActive = isCurrentPage && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val searchAppBarScrollBehavior = scrollBehavior as? SearchAppBarScrollBehavior
    val searchBarExpansionFraction =
        searchAppBarScrollBehavior?.searchBarExpandedFraction ?: 1f
    val isSearchBarCollapsing = searchBarExpansionFraction < 0.99f
    val isSearchBarCollapsed = searchBarExpansionFraction <= 0.01f
    var requestSearchFocus by remember { mutableStateOf(false) }
    val currentOnSearchTextChange by rememberUpdatedState(onSearchTextChange)
    val resetSearch by rememberUpdatedState {
        requestSearchFocus = false
        if (!preserveStateOnNavigation) textFieldState.clearText()
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        if (!preserveStateOnNavigation) currentOnSearchTextChange("")
    }

    LaunchedEffect(textFieldState.text) {
        currentOnSearchTextChange(textFieldState.text.toString())
    }

    LaunchedEffect(isCurrentPage, preserveStateOnNavigation) {
        if (!preserveStateOnNavigation && !isCurrentPage && scrollBehavior?.state?.collapsedFraction?.toDouble() == 1.0) {
            searchAppBarScrollBehavior?.collapseSearchBar()
        }
    }

    LaunchedEffect(isSearchBarCollapsing, isPageActive) {
        if (isSearchBarCollapsing && isPageActive) {
            requestSearchFocus = false
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    DisposableEffect(isPageActive) {
        onDispose {
            if (!isPageActive) resetSearch()
        }
    }

    // Navigation changes the entry lifecycle while it is still visible in the transition.
    // Only switching pager pages should reset the search bar's expansion.
    DisposableEffect(isCurrentPage, searchAppBarScrollBehavior, preserveStateOnNavigation) {
        onDispose {
            if (isCurrentPage && !preserveStateOnNavigation) searchAppBarScrollBehavior?.reset()
        }
    }

    DisposableEffect(lifecycleOwner, searchAppBarScrollBehavior, preserveStateOnNavigation) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && currentIsCurrentPage) {
                resetSearch()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column {
        LargeFlexibleTopAppBar(
            modifier = Modifier.blurEffect(),
            scrollBehavior = scrollBehavior,
            title = {
                Text(text = title)
            },
            navigationIcon = {
                if (onBackClick != null) {
                    AppBackButton(
                        onClick = {
                            onBackClick.invoke()
                        },
                    )
                } else {
                    navigationContent?.invoke()
                }
            },
            actions = {
                AnimatedVisibility(
                    visible = isSearchBarCollapsed,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    IconButton(
                        onClick = {
                            searchAppBarScrollBehavior?.expandSearchBar()
                            requestSearchFocus = true
                        },
                    ) {
                        Icon(
                            imageVector = Icons.TwoTone.Search,
                            contentDescription = searchBarPlaceHolderText,
                        )
                    }
                }
                dropdownContent?.invoke()
            },
            windowInsets = TopAppBarDefaults.windowInsets.add(WindowInsets(left = 12.dp)),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor =
                    if (themeConfig.isEnableBlur) {
                        Color.Transparent
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = cardConfig.cardAlpha)
                    },
                scrolledContainerColor =
                    if (themeConfig.isEnableBlur) {
                        Color.Transparent
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = cardConfig.cardAlpha)
                    },
            ),
        )

        AnimatedVisibility(
            visible = !isSearchBarCollapsed,
            enter = EnterTransition.None,
            exit = ExitTransition.None,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(searchBarExpansionFraction)
                .collapseWithTopAppBar(searchAppBarScrollBehavior),
        ) {
            Column {
                CompactSearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .padding(horizontal = 16.dp)
                        .clip(CircleShape)
                        .renderBackgroundBlur(MiuixTheme.colorScheme.surfaceContainerHighest),
                    textFieldState = textFieldState,
                    searchBarPlaceHolderText = searchBarPlaceHolderText,
                    onSearch = {
                        keyboardController?.hide()
                    },
                    requestFocus = requestSearchFocus,
                    onFocusRequestHandled = {
                        requestSearchFocus = false
                    },
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun SearchAppBarPreview() {
    SearchAppBar(
        title = "",
        searchText = "",
        onSearchTextChange = {},
        searchBarPlaceHolderText = "",
    )
}
