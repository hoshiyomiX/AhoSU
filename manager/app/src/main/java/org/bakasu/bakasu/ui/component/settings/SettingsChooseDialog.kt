package org.bakasu.bakasu.ui.component.settings

import android.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsChooseDialog(
    show: Boolean,
    title: String,
    items: List<String>,
    itemDescriptions: List<String?> = emptyList(),
    afterContent: @Composable (Int) -> Unit = {},
    selectedIndex: Int,
    maxHeight: Dp? = 400.dp,
    onDismiss: () -> Unit,
    onSelectedIndexChange: (Int) -> Unit,
) {
    if (!show || items.isEmpty()) return

    var currentIndex by remember(show, selectedIndex) { mutableIntStateOf(selectedIndex) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        SettingsChooseDialogFrame(
            title = title,
            maxHeight = maxHeight,
            onDismiss = onDismiss,
            onConfirm = {
                onSelectedIndexChange(currentIndex)
                onDismiss()
            },
        ) {
            lazySegmentColumn(items, noHorizontalPadding = true) { index, item ->
                SettingsBaseWidget(
                    title = item,
                    fillMaxWidth = false,
                    isOnBackground = false,
                    description = itemDescriptions.getOrNull(index),
                    selected = currentIndex == index,
                    onClick = {
                        currentIndex = index
                    },
                    leadingContent = {
                        RadioButton(
                            selected = currentIndex == index,
                            onClick = null,
                        )
                    },
                ) {
                    afterContent(index)
                }
            }
        }
    }
}

@Composable
private fun SettingsChooseDialogFrame(
    title: String,
    maxHeight: Dp?,
    showConfirmButton: Boolean = true,
    footerStartContent: (@Composable () -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: LazyListScope.() -> Unit,
) {
    Surface(
        modifier = Modifier
            .sizeIn(minWidth = 280.dp, maxWidth = 560.dp)
            .padding(horizontal = 32.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MiuixTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight ?: 400.dp)
                    .padding(top = 16.dp),
                content = content,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (footerStartContent != null) {
                    Spacer(modifier = Modifier.padding(start = 12.dp))
                    Box(modifier = Modifier.padding(bottom = 5.dp)) {
                        footerStartContent()
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
                TextButton(
                    text = stringResource(id = R.string.cancel),
                    onClick = onDismiss,
                )
                if (showConfirmButton) {
                    TextButton(
                        text = stringResource(id = R.string.ok),
                        onClick = onConfirm,
                    )
                }
            }
        }
    }
}
