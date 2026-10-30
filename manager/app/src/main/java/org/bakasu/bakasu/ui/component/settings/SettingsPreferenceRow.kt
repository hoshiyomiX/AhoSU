package org.bakasu.bakasu.ui.component.settings

import android.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.bakasu.bakasu.ui.theme.CardConfig
import org.bakasu.bakasu.ui.theme.ThemeConfig
import org.bakasu.bakasu.ui.theme.renderBackgroundBlur
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * A settings row built on the miuix [SwitchPreference], rendered inside the segmented glass
 * card used across the settings screens. The row itself is transparent ([SwitchPreference]
 * draws no container), so the liquid-glass treatment of the surrounding [SegmentedColumn]
 * is fully preserved.
 *
 * @param icon The [ImageVector] to be displayed at the start of the row.
 * @param title The primary text displayed in the row.
 * @param summary Optional supporting text displayed below the title.
 * @param enabled Whether the row is enabled and interactive.
 * @param isError If true, renders the summary with the miuix error color.
 * @param checked Whether the switch is currently on or off.
 * @param onCheckedChange Callback to be invoked when the switch state changes.
 * @param modifier The modifier to be applied to the row container.
 */
@Composable
fun SettingsSwitchRow(
    icon: ImageVector? = null,
    title: String,
    summary: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    PreferenceRowContainer(modifier = modifier) {
        SwitchPreference(
            checked = checked,
            onCheckedChange = onCheckedChange,
            title = title,
            summary = summary,
            summaryColor = BasicComponentDefaults.summaryColor(
                color = if (isError) {
                    MiuixTheme.colorScheme.error
                } else {
                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                },
            ),
            enabled = enabled,
            startAction = icon?.let { vector -> { PreferenceRowIcon(vector) } },
        )
    }
}

/**
 * A settings row built on the miuix [ArrowPreference] that opens a choice dialog whose
 * options are miuix [RadioButtonPreference] rows (the HyperOS picker pattern). When no
 * explicit [summary] is provided, the current value is mirrored into the row summary.
 *
 * @param icon The [ImageVector] to be displayed at the start of the row.
 * @param title The primary text displayed in the row.
 * @param items The selectable option labels.
 * @param selectedIndex The currently selected index into [items].
 * @param onSelectedIndexChange Callback to be invoked when the user confirms a new selection.
 * @param enabled Whether the row is enabled and interactive.
 * @param summary Optional supporting text below the title; when set it replaces the
 *   selected-value mirror (the selection is still visible inside the dialog).
 * @param itemDescriptions Optional per-option supporting text shown inside the dialog.
 * @param dialogEmptyContent Optional content rendered inside the dialog instead of the option
 *   list — e.g. an offline retry placeholder when there is nothing to choose from. When set,
 *   the confirm button is omitted.
 * @param itemEndActions Optional per-option trailing slot rendered inside the dialog rows —
 *   e.g. a per-entry "view" affordance next to the radio button.
 * @param modifier The modifier to be applied to the row container.
 */
@Composable
fun SettingsChooseRow(
    icon: ImageVector? = null,
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    summary: String? = null,
    itemDescriptions: List<String?> = emptyList(),
    dialogEmptyContent: (@Composable () -> Unit)? = null,
    itemEndActions: (@Composable (Int) -> Unit)? = null,
) {
    var showDialog by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableIntStateOf(selectedIndex) }

    PreferenceRowContainer(modifier = modifier) {
        ArrowPreference(
            title = title,
            summary = summary ?: items.getOrNull(selectedIndex),
            enabled = enabled,
            onClick = if (enabled) {
                {
                    currentIndex = selectedIndex
                    showDialog = true
                }
            } else {
                null
            },
            startAction = icon?.let { vector -> { PreferenceRowIcon(vector) } },
        )
    }

    if (showDialog && (items.isNotEmpty() || dialogEmptyContent != null)) {
        MiuixChoiceDialog(
            title = title,
            items = items,
            itemDescriptions = itemDescriptions,
            emptyContent = dialogEmptyContent,
            itemEndActions = itemEndActions,
            currentIndex = currentIndex,
            onCurrentIndexChange = { currentIndex = it },
            onDismiss = { showDialog = false },
            onConfirm = {
                onSelectedIndexChange(currentIndex)
                showDialog = false
            },
        )
    }
}

/**
 * A settings row built on the miuix [ArrowPreference] that simply navigates or triggers an
 * action on click, with an optional summary below the title.
 *
 * @param icon The [ImageVector] to be displayed at the start of the row.
 * @param title The primary text displayed in the row.
 * @param summary Optional supporting text displayed below the title.
 * @param enabled Whether the row is enabled and interactive.
 * @param onClick Callback to be invoked when the row is clicked.
 * @param bottomAction Optional content rendered below the title and summary — e.g. a badge
 *   row for list entries built on the jump row.
 * @param modifier The modifier to be applied to the row container.
 */
@Composable
fun SettingsJumpRow(
    icon: ImageVector? = null,
    title: String,
    summary: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
    bottomAction: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    PreferenceRowContainer(modifier = modifier) {
        ArrowPreference(
            title = title,
            summary = summary,
            enabled = enabled,
            onClick = if (enabled) onClick else null,
            startAction = icon?.let { vector -> { PreferenceRowIcon(vector) } },
            bottomAction = bottomAction,
        )
    }
}

/**
 * A non-interactive settings row built on the miuix [BasicComponent]: an icon, a title and an
 * optional summary for read-only status values, rendered inside the segmented glass card.
 *
 * @param icon The [ImageVector] to be displayed at the start of the row.
 * @param title The primary text displayed in the row.
 * @param summary Optional supporting text displayed below the title.
 * @param enabled Whether the row content is shown in its enabled state.
 * @param isError If true, renders the summary with the miuix error color.
 * @param modifier The modifier to be applied to the row container.
 */
@Composable
fun SettingsInfoRow(
    icon: ImageVector? = null,
    title: String,
    summary: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    modifier: Modifier = Modifier,
) {
    PreferenceRowContainer(modifier = modifier) {
        BasicComponent(
            title = title,
            summary = summary,
            summaryColor = BasicComponentDefaults.summaryColor(
                color = if (isError) {
                    MiuixTheme.colorScheme.error
                } else {
                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                },
            ),
            enabled = enabled,
            startAction = icon?.let { vector -> { PreferenceRowIcon(vector) } },
        )
    }
}

/**
 * A settings row built on the miuix [SliderPreference]: title (and optional icon) on top,
 * the slider in the row's bottom area, and the current value shown on the end side — the
 * native HyperOS "row slider" pattern, replacing the hand-rolled trailing-value layouts.
 *
 * @param icon The [ImageVector] to be displayed at the start of the row.
 * @param title The primary text displayed in the row.
 * @param value The current value of the slider.
 * @param onValueChange Callback to be invoked when the value changes.
 * @param onValueChangeFinished Callback to be invoked when the value change has ended.
 * @param valueRange The range the slider value can take.
 * @param keyPoints Optional key point values to display and magnetically snap to.
 * @param valueText Optional text representing the current value, shown on the end side.
 * @param enabled Whether the row is enabled and interactive.
 * @param modifier The modifier to be applied to the row container.
 */
@Composable
fun SettingsSliderRow(
    icon: ImageVector? = null,
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float>,
    keyPoints: List<Float>? = null,
    valueText: String? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    PreferenceRowContainer(modifier = modifier) {
        SliderPreference(
            value = value,
            onValueChange = onValueChange,
            title = title,
            valueText = valueText,
            enabled = enabled,
            valueRange = valueRange,
            onValueChangeFinished = onValueChangeFinished,
            showKeyPoints = keyPoints != null,
            keyPoints = keyPoints,
            startAction = icon?.let { vector -> { PreferenceRowIcon(vector) } },
        )
    }
}

/**
 * A display or clickable value row built on the miuix [BasicComponent]: title, optional summary,
 * an optional start-side slot (icon or custom leading content), an optional end-side slot for the
 * current value, and an optional bottom slot for extra content such as selection chips.
 *
 * @param icon The [ImageVector] to be displayed at the start of the row.
 * @param title The primary text displayed in the row.
 * @param summary Optional supporting text displayed below the title.
 * @param enabled Whether the row is enabled and interactive.
 * @param isError If true, renders the summary with the miuix error color.
 * @param onClick Optional callback to be invoked when the row is clicked.
 * @param modifier The modifier to be applied to the row container.
 * @param leadingContent Custom composable rendered at the start of the row instead of [icon].
 * @param endContent Optional composable rendered at the end of the row, e.g. the current value.
 * @param bottomContent Optional composable rendered below the title and summary.
 * @param transparentContainer When true, renders the row bare, without the glass card container,
 *   for rows that already sit inside their own card surface.
 */
@Composable
fun SettingsValueRow(
    icon: ImageVector? = null,
    title: String? = null,
    summary: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
    endContent: (@Composable RowScope.() -> Unit)? = null,
    bottomContent: (@Composable () -> Unit)? = null,
    transparentContainer: Boolean = false,
) {
    val summaryColor = BasicComponentDefaults.summaryColor(
        color = if (isError) {
            MiuixTheme.colorScheme.error
        } else {
            MiuixTheme.colorScheme.onSurfaceVariantSummary
        },
    )

    val row: @Composable () -> Unit = {
        BasicComponent(
            title = title,
            summary = summary,
            summaryColor = summaryColor,
            enabled = enabled,
            onClick = if (enabled) onClick else null,
            startAction = leadingContent ?: icon?.let { vector -> { PreferenceRowIcon(vector) } },
            endActions = endContent,
            bottomAction = bottomContent,
        )
    }

    if (transparentContainer) {
        Box(modifier = modifier) {
            row()
        }
    } else {
        PreferenceRowContainer(modifier = modifier) {
            row()
        }
    }
}

/**
 * A multi-select settings row built on the miuix [ArrowPreference] that opens a choice dialog
 * whose options are miuix [CheckboxPreference] rows. The confirmed selection is reported as a
 * set of indices into [items]; an optional [maxSelected] cap is enforced while toggling.
 *
 * @param icon The [ImageVector] to be displayed at the start of the row.
 * @param title The primary text displayed in the row.
 * @param items The selectable option labels.
 * @param selectedIndices The currently selected indices into [items].
 * @param onSelectedIndicesChange Callback invoked with the confirmed selection when the user
 *   confirms the dialog.
 * @param modifier The modifier to be applied to the row container.
 * @param enabled Whether the row is enabled and interactive.
 * @param summary Optional supporting text below the title.
 * @param itemDescriptions Optional per-option supporting text shown inside the dialog.
 * @param maxSelected Optional cap on how many options may be selected at once.
 * @param bottomContent Optional composable rendered below the title and summary (selection
 *   chips).
 */
@Composable
fun SettingsMultiChooseRow(
    icon: ImageVector? = null,
    title: String,
    items: List<String>,
    selectedIndices: Set<Int>,
    onSelectedIndicesChange: (Set<Int>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    summary: String? = null,
    itemDescriptions: List<String?> = emptyList(),
    maxSelected: Int? = null,
    bottomContent: (@Composable () -> Unit)? = null,
) {
    var showDialog by remember { mutableStateOf(false) }
    var currentIndices by remember { mutableStateOf(selectedIndices) }

    PreferenceRowContainer(modifier = modifier) {
        ArrowPreference(
            title = title,
            summary = summary,
            enabled = enabled,
            onClick = if (enabled) {
                {
                    currentIndices = selectedIndices
                    showDialog = true
                }
            } else {
                null
            },
            startAction = icon?.let { vector -> { PreferenceRowIcon(vector) } },
            bottomAction = bottomContent,
        )
    }

    if (showDialog && items.isNotEmpty()) {
        MiuixMultiChoiceDialog(
            title = title,
            items = items,
            itemDescriptions = itemDescriptions,
            currentIndices = currentIndices,
            onToggle = { index ->
                currentIndices = if (index in currentIndices) {
                    currentIndices - index
                } else if (maxSelected == null || currentIndices.size < maxSelected) {
                    currentIndices + index
                } else {
                    currentIndices
                }
            },
            onDismiss = { showDialog = false },
            onConfirm = {
                onSelectedIndicesChange(currentIndices)
                showDialog = false
            },
        )
    }
}

/**
 * A settings input row built on the miuix [TextField]: the row label doubles as the field
 * placeholder (the HyperOS floating-label pattern), and an optional error line renders below
 * the field in the miuix error color.
 *
 * @param title The row label, also used as the field placeholder.
 * @param state The [TextFieldState] backing the input.
 * @param modifier The modifier to be applied to the row container.
 * @param error Optional error hint rendered below the field.
 * @param enabled Whether the input is enabled.
 * @param readOnly Whether the input is read-only.
 * @param inputTransformation Optional [InputTransformation] applied to the input.
 * @param keyboardOptions The soft-keyboard options for the input.
 * @param onKeyboardAction Optional handler for IME actions.
 * @param lineLimits The line limits applied to the input (single- or multi-line).
 * @param leadingIcon Optional composable rendered inside the field on the leading side.
 * @param trailingIcon Optional composable rendered inside the field on the trailing side.
 */
@Composable
fun SettingsTextFieldRow(
    title: String,
    state: TextFieldState,
    modifier: Modifier = Modifier,
    error: String = "",
    enabled: Boolean = true,
    readOnly: Boolean = false,
    inputTransformation: InputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    PreferenceRowContainer(modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            TextField(
                state = state,
                modifier = Modifier.fillMaxWidth(),
                label = title,
                useLabelAsPlaceholder = true,
                enabled = enabled,
                readOnly = readOnly,
                inputTransformation = inputTransformation,
                keyboardOptions = keyboardOptions,
                onKeyboardAction = onKeyboardAction,
                lineLimits = lineLimits,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
            )

            if (error.isNotBlank()) {
                Text(
                    text = error,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/**
 * A settings row built on the miuix [WindowDropdownPreference]: clicking the row opens a
 * dropdown rendered in the window layer and anchored to the row. With [showValue] disabled the
 * row acts as a plain action menu (no persistent selection is rendered on the row).
 *
 * @param title The primary text displayed in the row.
 * @param items The dropdown option labels.
 * @param selectedIndex The currently selected index into [items], or -1 when nothing is
 *   selected (action-menu mode).
 * @param onSelectedIndexChange Callback to be invoked when an option is picked.
 * @param modifier The modifier to be applied to the row container.
 * @param icon The [ImageVector] to be displayed at the start of the row.
 * @param summary Optional supporting text displayed below the title.
 * @param enabled Whether the row is enabled and interactive.
 * @param showValue Whether the currently selected value is rendered at the end of the row.
 * @param leadingContent Custom composable rendered at the start of the row instead of [icon].
 */
@Composable
fun SettingsDropdownRow(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    summary: String? = null,
    enabled: Boolean = true,
    showValue: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    PreferenceRowContainer(modifier = modifier) {
        WindowDropdownPreference(
            items = items,
            selectedIndex = selectedIndex,
            title = title,
            summary = summary,
            enabled = enabled,
            showValue = showValue,
            onSelectedIndexChange = onSelectedIndexChange,
            startAction = leadingContent ?: icon?.let { vector -> { PreferenceRowIcon(vector) } },
        )
    }
}

/**
 * The glass card every preference row lives in. Mirrors the container treatment of
 * [SettingsBaseWidget]: clipped to the segmented item shape, tinted with the card alpha,
 * and translucent when the experimental group blur takes over the background rendering.
 */
@Composable
private fun PreferenceRowContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val themeConfig: ThemeConfig = koinInject()
    val cardConfig: CardConfig = koinInject()
    val shape: Shape = LocalSegmentedItemShape.current

    val containerColor = MiuixTheme.colorScheme.surfaceContainerHighest
        .copy(alpha = cardConfig.cardAlpha)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .renderBackgroundBlur(containerColor)
            .background(
                color = if (themeConfig.isEnableBlurExp) Color.Transparent else containerColor,
            ),
    ) {
        content()
    }
}

/**
 * The start-side row icon. Tinted explicitly with the miuix on-background color because the
 * miuix [LocalContentColor][top.yukonga.miuix.kmp.theme.LocalContentColor] defaults to black
 * and the transparent preference rows do not provide it.
 */
@Composable
private fun PreferenceRowIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint = MiuixTheme.colorScheme.onBackground,
    )
}

/**
 * The HyperOS-style single-choice dialog: the app-standard miuix [OverlayDialog] shell with
 * the dialog title, a list of miuix [RadioButtonPreference] options, and cancel/confirm
 * [TextButton]s that stay visible below the scrolling option list. When [emptyContent] is
 * provided it replaces the option list and the confirm button is omitted.
 */
@Composable
private fun MiuixChoiceDialog(
    title: String,
    items: List<String>,
    itemDescriptions: List<String?> = emptyList(),
    emptyContent: (@Composable () -> Unit)? = null,
    itemEndActions: (@Composable (Int) -> Unit)? = null,
    currentIndex: Int,
    onCurrentIndexChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    OverlayDialog(
        show = true,
        onDismissRequest = onDismiss,
        title = title,
    ) {
        if (emptyContent != null) {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                emptyContent()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
            ) {
                itemsIndexed(items) { index, item ->
                    RadioButtonPreference(
                        title = item,
                        selected = index == currentIndex,
                        onClick = { onCurrentIndexChange(index) },
                        summary = itemDescriptions.getOrNull(index),
                        endActions = itemEndActions?.let { slot -> { slot(index) } },
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                text = stringResource(id = R.string.cancel),
                onClick = onDismiss,
            )
            if (emptyContent == null) {
                TextButton(
                    text = stringResource(id = R.string.ok),
                    onClick = onConfirm,
                )
            }
        }
    }
}

/**
 * The HyperOS-style multi-choice dialog: the app-standard miuix [OverlayDialog] shell with the
 * dialog title, a list of miuix [CheckboxPreference] options, and cancel/confirm [TextButton]s.
 * The selection is only committed through [onConfirm]; dismissing the dialog discards any
 * pending toggles.
 */
@Composable
private fun MiuixMultiChoiceDialog(
    title: String,
    items: List<String>,
    itemDescriptions: List<String?> = emptyList(),
    currentIndices: Set<Int>,
    onToggle: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    OverlayDialog(
        show = true,
        onDismissRequest = onDismiss,
        title = title,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
        ) {
            itemsIndexed(items) { index, item ->
                CheckboxPreference(
                    title = item,
                    checked = index in currentIndices,
                    onCheckedChange = { onToggle(index) },
                    summary = itemDescriptions.getOrNull(index),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                text = stringResource(id = R.string.cancel),
                onClick = onDismiss,
            )
            TextButton(
                text = stringResource(id = R.string.ok),
                onClick = onConfirm,
            )
        }
    }
}
