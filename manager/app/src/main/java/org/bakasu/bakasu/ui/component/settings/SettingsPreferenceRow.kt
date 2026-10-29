package org.bakasu.bakasu.ui.component.settings

import android.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.bakasu.bakasu.ui.theme.CardConfig
import org.bakasu.bakasu.ui.theme.ThemeConfig
import org.bakasu.bakasu.ui.theme.renderBackgroundBlur
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.BasicComponentDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
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
 * options are miuix [RadioButtonPreference] rows (the HyperOS picker pattern). The current
 * value is mirrored into the row summary.
 *
 * @param icon The [ImageVector] to be displayed at the start of the row.
 * @param title The primary text displayed in the row.
 * @param items The selectable option labels.
 * @param selectedIndex The currently selected index into [items].
 * @param onSelectedIndexChange Callback to be invoked when the user confirms a new selection.
 * @param enabled Whether the row is enabled and interactive.
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
) {
    var showDialog by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableIntStateOf(selectedIndex) }

    PreferenceRowContainer(modifier = modifier) {
        ArrowPreference(
            title = title,
            summary = items.getOrNull(selectedIndex),
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

    if (showDialog && items.isNotEmpty()) {
        MiuixChoiceDialog(
            title = title,
            items = items,
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
 * @param modifier The modifier to be applied to the row container.
 */
@Composable
fun SettingsJumpRow(
    icon: ImageVector? = null,
    title: String,
    summary: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PreferenceRowContainer(modifier = modifier) {
        ArrowPreference(
            title = title,
            summary = summary,
            enabled = enabled,
            onClick = if (enabled) onClick else null,
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
 * The HyperOS-style single-choice dialog: a miuix surface with the dialog title, a list of
 * miuix [RadioButtonPreference] options, and cancel/confirm [TextButton]s.
 */
@Composable
private fun MiuixChoiceDialog(
    title: String,
    items: List<String>,
    currentIndex: Int,
    onCurrentIndexChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .sizeIn(minWidth = 280.dp, maxWidth = 560.dp)
                .padding(horizontal = 32.dp),
            shape = RoundedCornerShape(24.dp),
            color = MiuixTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.title3,
                    color = MiuixTheme.colorScheme.onBackground,
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .padding(top = 16.dp),
                ) {
                    itemsIndexed(items) { index, item ->
                        RadioButtonPreference(
                            title = item,
                            selected = index == currentIndex,
                            onClick = { onCurrentIndexChange(index) },
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
    }
}
