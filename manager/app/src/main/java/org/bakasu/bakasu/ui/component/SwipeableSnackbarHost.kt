package org.bakasu.bakasu.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.bakasu.bakasu.ui.util.LocalSnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState

/**
 * Hosts the miuix snackbar stack. Swipe-to-dismiss is built into the miuix
 * [SnackbarHost] (anchored draggable), so the former m3 SwipeToDismissBox
 * wrapper is no longer needed.
 */
@Composable
fun SwipeableSnackbarHost(
    modifier: Modifier = Modifier,
    hostState: SnackbarHostState = LocalSnackbarHost.current,
) {
    SnackbarHost(
        state = hostState,
        modifier = modifier,
    )
}
