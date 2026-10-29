package org.bakasu.bakasu.ui.component.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.OverlayListPopup
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check

@Composable
fun SettingsDropdownWidget(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    title: String,
    description: String? = null,
    iconPlaceholder: Boolean = true,
    enabled: Boolean = true,
    renderBackgroundBlur: Boolean = true,
    isError: Boolean = false,
    choice: Int,
    data: List<String>,
    leadingContent: (@Composable () -> Unit)? = null,
    onChoiceChange: (Int) -> Unit,
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    var touchPoint: Offset by remember { mutableStateOf(Offset.Zero) }

    val (offsetX, offsetY) = with(LocalDensity.current) {
        (touchPoint.x.toDp()) to (touchPoint.y.toDp())
    }

    Box {
        SettingsBaseWidget(
            modifier = modifier,
            icon = icon,
            title = title,
            description = description,
            enabled = enabled,
            isOnBackground = renderBackgroundBlur,
            isError = isError,
            onClick = { offest ->
                touchPoint = offest

                expanded = !expanded
            },
            leadingContent = leadingContent,
            iconPlaceholder = iconPlaceholder,
        ) {}

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(offsetX, offsetY),
        ) {
            OverlayListPopup(
                show = expanded,
                onDismissRequest = { expanded = false },
            ) {
                ListPopupColumn {
                    data.forEachIndexed { index, item ->
                        val isSelected = index == choice

                        BasicComponent(
                            title = item,
                            onClick = {
                                onChoiceChange(index)
                                expanded = false
                            },
                            endActions = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = MiuixIcons.Basic.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }
}
