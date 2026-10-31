package org.bakasu.bakasu.ui.screen.themeSettings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Android
import androidx.compose.material.icons.twotone.Animation
import androidx.compose.material.icons.twotone.Badge
import androidx.compose.material.icons.twotone.BlurOn
import androidx.compose.material.icons.twotone.Brush
import androidx.compose.material.icons.twotone.Check
import androidx.compose.material.icons.twotone.Contrast
import androidx.compose.material.icons.twotone.DarkMode
import androidx.compose.material.icons.twotone.Dock
import androidx.compose.material.icons.twotone.Draw
import androidx.compose.material.icons.twotone.FormatSize
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.LightMode
import androidx.compose.material.icons.twotone.Opacity
import androidx.compose.material.icons.twotone.Palette
import androidx.compose.material.icons.twotone.Pin
import androidx.compose.material.icons.twotone.SwapHoriz
import androidx.compose.material.icons.twotone.Swipe
import androidx.compose.material.icons.twotone.Translate
import androidx.compose.material.icons.twotone.ViewCarousel
import androidx.compose.material.icons.twotone.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yalantis.ucrop.UCrop
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bakasu.bakasu.R
import org.bakasu.bakasu.data.theme.ThemeRepository
import org.bakasu.bakasu.domain.usecase.IsSystemLanguageSettingsUseCase
import org.bakasu.bakasu.domain.usecase.LaunchSystemLanguageSettingsUseCase
import org.bakasu.bakasu.ui.component.ConfirmResult
import org.bakasu.bakasu.ui.component.KeyPointSlider
import org.bakasu.bakasu.ui.component.rememberConfirmDialog
import org.bakasu.bakasu.ui.component.settings.AppBackButton
import org.bakasu.bakasu.ui.component.settings.SegmentedColumn
import org.bakasu.bakasu.ui.component.settings.SegmentedColumnScope
import org.bakasu.bakasu.ui.component.settings.SettingsChooseDialog
import org.bakasu.bakasu.ui.component.settings.SettingsChooseRow
import org.bakasu.bakasu.ui.component.settings.SettingsJumpRow
import org.bakasu.bakasu.ui.component.settings.SettingsSliderRow
import org.bakasu.bakasu.ui.component.settings.SettingsSwitchRow
import org.bakasu.bakasu.ui.component.settings.SettingsValueRow
import org.bakasu.bakasu.ui.navigation.LocalNavigator
import org.bakasu.bakasu.ui.screen.themeSettings.crop.BackgroundCropActivity
import org.bakasu.bakasu.ui.theme.BackgroundManager
import org.bakasu.bakasu.ui.theme.BottomBarStyle
import org.bakasu.bakasu.ui.theme.CardConfig
import org.bakasu.bakasu.ui.theme.ThemeConfig
import org.bakasu.bakasu.ui.theme.blurEffect
import org.bakasu.bakasu.ui.theme.blurSource
import org.bakasu.bakasu.ui.util.adaptiveScaffoldWindowInsets
import org.bakasu.bakasu.ui.viewmodel.HomeUiAction
import org.bakasu.bakasu.ui.viewmodel.HomeUiState
import org.bakasu.bakasu.ui.viewmodel.HomeViewModel
import org.bakasu.bakasu.ui.viewmodel.ModuleUiAction
import org.bakasu.bakasu.ui.viewmodel.ModuleUiState
import org.bakasu.bakasu.ui.viewmodel.ModuleViewModel
import org.bakasu.bakasu.ui.viewmodel.PredictiveBackAnimation
import org.bakasu.bakasu.ui.viewmodel.PredictiveBackExitDirection
import org.bakasu.bakasu.ui.viewmodel.SettingsUiAction
import org.bakasu.bakasu.ui.viewmodel.SettingsUiState
import org.bakasu.bakasu.ui.viewmodel.SettingsViewModel
import org.bakasu.bakasu.ui.viewmodel.dpiFriendlyNameRes
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ColorPicker
import top.yukonga.miuix.kmp.basic.ColorSpace
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@SuppressLint(
    "LocalContextConfigurationRead",
    "LocalContextResourcesRead",
    "ObsoleteSdkInt",
)
@Composable
fun ThemeSettingsScreen(
    settingsViewModel: SettingsViewModel,
) {
    val themeConfig: ThemeConfig = koinInject()
    val cardConfig: CardConfig = koinInject()
    // 顶部滚动行为
    val scrollBehavior = MiuixScrollBehavior()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // 创建设置状态管理器
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val homeViewModel = koinViewModel<HomeViewModel>()
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    val moduleViewModel = koinViewModel<ModuleViewModel>()
    val moduleUiState by moduleViewModel.uiState.collectAsStateWithLifecycle()

    // Image selection and cropping
    var pendingBackgroundImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingExternalCropOutputUri by remember { mutableStateOf<Uri?>(null) }
    var showCropMethodDialog by remember { mutableStateOf(false) }

    val externalCropUnavailable = stringResource(R.string.background_external_crop_unavailable)
    val backgroundCropFailed = stringResource(R.string.background_crop_failed)

    val uCropLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                UCrop.getOutput(data)?.let {
                    settingsViewModel.dispatch(
                        SettingsUiAction.SetCustomBackground(it.toString()),
                    )
                }
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            Toast.makeText(
                context,
                backgroundCropFailed,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val externalCropLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val outputUri = pendingExternalCropOutputUri ?: result.data?.data
            outputUri?.let {
                settingsViewModel.dispatch(
                    SettingsUiAction.SetCustomBackground(it.toString()),
                )
            }
        }
        pendingExternalCropOutputUri = null
    }

    val launchInAppCrop = { sourceUri: Uri ->
        val outputUri = createBackgroundCropOutputUri(context, "background_crop_ucrop")
        val (screenWidth, screenHeight) = context.backgroundCropSize()
        val intent = Intent(context, BackgroundCropActivity::class.java).apply {
            putExtra(UCrop.EXTRA_INPUT_URI, sourceUri)
            putExtra(UCrop.EXTRA_OUTPUT_URI, outputUri)
            putExtra(UCrop.EXTRA_ASPECT_RATIO_X, screenWidth.toFloat())
            putExtra(UCrop.EXTRA_ASPECT_RATIO_Y, screenHeight.toFloat())
            putExtra(UCrop.EXTRA_MAX_SIZE_X, screenWidth)
            putExtra(UCrop.EXTRA_MAX_SIZE_Y, screenHeight)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        uCropLauncher.launch(intent)
    }

    val launchExternalCrop = { sourceUri: Uri ->
        val uri = createBackgroundCropOutputUri(context, "background_crop_external")
        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        val (screenWidth, screenHeight) = context.backgroundCropSize()
        val intent = Intent("com.android.camera.action.CROP").apply {
            setDataAndType(uri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            putExtra("crop", "true")
            putExtra("aspectX", screenWidth)
            putExtra("aspectY", screenHeight)
            putExtra("outputX", screenWidth)
            putExtra("outputY", screenHeight)
            putExtra("return-data", false)
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
        }

        try {
            pendingExternalCropOutputUri = uri
            externalCropLauncher.launch(intent)
        } catch (t: Throwable) {
            Log.e("ThemeSettings", "External Crop failed, fallback to in-app crop", t)
            pendingExternalCropOutputUri = null
            Toast.makeText(
                context,
                externalCropUnavailable,
                Toast.LENGTH_SHORT,
            ).show()
            launchInAppCrop(sourceUri)
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            pendingBackgroundImageUri = it
            showCropMethodDialog = true
        }
    }

    SettingsChooseDialog(
        show = showCropMethodDialog,
        title = stringResource(R.string.background_crop_method_title),
        items = listOf(
            stringResource(R.string.background_crop_method_in_app),
            stringResource(R.string.background_crop_method_external),
        ),
        selectedIndex = 0,
        onDismiss = {
            showCropMethodDialog = false
            pendingBackgroundImageUri = null
        },
        onSelectedIndexChange = { index ->
            pendingBackgroundImageUri?.let { sourceUri ->
                when (index) {
                    0 -> launchInAppCrop(sourceUri)
                    1 -> launchExternalCrop(sourceUri)
                }
            }
        },
    )

    // 各种设置对话框（语言选择器由 LanguageSetting 直接渲染）

    val navigator = LocalNavigator.current

    LaunchedEffect(Unit) {
        scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffsetLimit
    }

    MiuixScaffold(
        contentWindowInsets = adaptiveScaffoldWindowInsets(),
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MiuixTopAppBar(
                modifier = Modifier
                    .blurEffect(),
                title = stringResource(R.string.theme_settings),
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
                scrollBehavior = scrollBehavior,
            )
        },
        containerColor = Color.Transparent,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .blurSource(),
        ) {
            item {
                Spacer(modifier = Modifier.height(paddingValues.calculateTopPadding()))
            }

            item {
                // 外观设置
                AppearanceSettings(
                    state = settingsState,
                    viewModel = settingsViewModel,
                    pickImageLauncher = pickImageLauncher,
                    coroutineScope = coroutineScope,
                )
            }

            item {
                // Predictive Back Settings
                SegmentedColumn(
                    title = stringResource(R.string.predictive_back_settings),
                ) {
                    item {
                        PredictiveBackAnimationWidget(settingsState) { animation ->
                            settingsViewModel.dispatch(
                                SettingsUiAction.SetPredictiveBackAnimation(animation),
                            )
                        }
                    }
                    item(
                        visible = settingsState.predictiveBackAnimation == PredictiveBackAnimation.Scale,
                    ) {
                        PredictiveBackAnimationDirectionWidget(settingsState) { direction ->
                            settingsViewModel.dispatch(
                                SettingsUiAction.SetPredictiveBackExitDirection(direction),
                            )
                        }
                    }
                }
            }

            item {
                SegmentedColumn(
                    title = stringResource(R.string.settings_pager_gesture_mode),
                ) {
                    item {
                        SettingsSwitchRow(
                            icon = Icons.TwoTone.Swipe,
                            title = stringResource(R.string.settings_enable_swipe_dismiss),
                            summary = stringResource(R.string.settings_enable_swipe_dismiss_summary),
                            checked = settingsState.enableSwipeDismiss,
                            onCheckedChange = { enabled ->
                                settingsViewModel.dispatch(
                                    SettingsUiAction.SetSwipeDismiss(enabled),
                                )
                            },
                        )
                    }
                    item {
                        SettingsChooseRow(
                            icon = Icons.TwoTone.ViewCarousel,
                            title = stringResource(R.string.settings_pager_gesture_mode),
                            items = listOf(
                                stringResource(R.string.settings_pager_gesture_native),
                                stringResource(R.string.settings_pager_gesture_cross_axis),
                                stringResource(R.string.settings_pager_gesture_ios_like),
                            ),
                            selectedIndex = settingsState.pagerInterceptionMode.coerceIn(0, 2),
                            onSelectedIndexChange = { index ->
                                settingsViewModel.dispatch(
                                    SettingsUiAction.SetPagerInterceptionMode(index),
                                )
                            },
                        )
                    }
                }
            }

            item {
                // 自定义设置
                CustomizationSettings(
                    homeUiState = homeUiState,
                    homeViewModel = homeViewModel,
                    moduleUiState = moduleUiState,
                    moduleViewModel = moduleViewModel,
                    settingsUiState = settingsState,
                    settingsViewModel = settingsViewModel,
                )
            }

            item {
                // 系统导航栏padding计算
                Spacer(modifier = Modifier.height(paddingValues.calculateBottomPadding()))
            }
        }
    }
}

@Composable
fun PredictiveBackAnimationWidget(
    uiState: SettingsUiState,
    onSelect: (PredictiveBackAnimation) -> Unit,
) {
    SettingsChooseRow(
        icon = Icons.TwoTone.Animation,
        title = stringResource(R.string.predictive_back_animation),
        items = PredictiveBackAnimation.entries.map { stringResource(it.resId) },
        selectedIndex = uiState.predictiveBackAnimation.ordinal,
        onSelectedIndexChange = { index ->
            PredictiveBackAnimation.entries.getOrNull(index)?.let(onSelect)
        },
    )
}

@Composable
fun PredictiveBackAnimationDirectionWidget(
    uiState: SettingsUiState,
    onSelect: (PredictiveBackExitDirection) -> Unit,
) {
    SettingsChooseRow(
        icon = Icons.TwoTone.SwapHoriz,
        title = stringResource(R.string.predictive_back_exit_direction),
        items = PredictiveBackExitDirection.entries.map { stringResource(it.resId) },
        selectedIndex = uiState.predictiveBackExitDirection.ordinal,
        onSelectedIndexChange = { index ->
            PredictiveBackExitDirection.entries.getOrNull(index)?.let(onSelect)
        },
    )
}

@Composable
private fun AppearanceSettings(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    pickImageLauncher: ManagedActivityResultLauncher<String, Uri?>,
    coroutineScope: CoroutineScope,
) {
    val themeConfig: ThemeConfig = koinInject()
    val cardConfig: CardConfig = koinInject()
    val backgroundManager: BackgroundManager = koinInject()
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.screenWidthDp < configuration.screenHeightDp ||
        (configuration.screenHeightDp.toFloat() / configuration.screenWidthDp > 1.4f)
    SegmentedColumn(title = stringResource(R.string.appearance_settings)) {
        item {
            // 语言设置
            LanguageSetting(state = state, viewModel = viewModel)
        }

        item {
            // 主题模式
            SettingsChooseRow(
                icon = Icons.TwoTone.DarkMode,
                title = stringResource(R.string.theme_mode),
                items = state.themeOptions.map { stringResource(it) },
                selectedIndex = state.themeMode,
                onSelectedIndexChange = { index ->
                    viewModel.dispatch(SettingsUiAction.SetThemeMode(index))
                },
            )
        }

        item {
            // 主题强调色
            AccentColorSetting(themeConfig = themeConfig)
        }

        item(
            forceFlatBottom = true,
        ) {
            SettingsValueRow(
                icon = Icons.TwoTone.FormatSize,
                title = stringResource(R.string.app_dpi_title),
                summary = stringResource(R.string.app_dpi_summary),
                endContent = {
                    Text(
                        text = stringResource(dpiFriendlyNameRes(state.tempDpi)),
                        style = MiuixTheme.textStyles.main,
                        color = MiuixTheme.colorScheme.primary,
                    )
                },
            )
        }

        item(
            topPadding = 1.dp,
            forceFlatTop = true,
        ) { shape ->
            Surface(
                modifier = Modifier
                    .clip(shape),
                color = if (themeConfig.isEnableBlurExp) {
                    Color.Transparent
                } else {
                    MaterialTheme.colorScheme.surfaceBright.copy(
                        alpha = cardConfig.cardAlpha,
                    )
                },
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    DpiSliderControls(
                        state = state,
                        viewModel = viewModel,
                        coroutineScope = coroutineScope,
                    )
                }
            }
        }

        item(visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            SettingsSwitchRow(
                icon = Icons.TwoTone.BlurOn,
                title = stringResource(id = R.string.settings_config_enable_blur),
                summary = stringResource(id = R.string.settings_config_enable_blur_summary),
                checked = themeConfig.isEnableBlur,
                onCheckedChange = { isChecked ->
                    backgroundManager.saveEnableBlur(isChecked)
                    if (!isChecked) {
                        backgroundManager.saveEnableBlurExp(false)
                    }
                },
            )
        }

        item(visible = isPortrait) {
            SettingsSwitchRow(
                icon = Icons.TwoTone.Dock,
                title = stringResource(R.string.enable_floating_bottom_bar),
                summary = stringResource(R.string.enable_floating_bottom_bar_summary),
                checked = themeConfig.bottomBarStyle == BottomBarStyle.FLOATING,
                onCheckedChange = { enabled ->
                    val style = if (enabled) BottomBarStyle.FLOATING else BottomBarStyle.MATERIAL3_EXPRESSIVE
                    backgroundManager.saveBottomBarStyle(style)
                },
            )
        }

        item(
            visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            topPadding = 1.dp,
        ) {
            SettingsSwitchRow(
                icon = Icons.TwoTone.Contrast,
                title = stringResource(id = R.string.settings_custom_enable_high_contrast),
                summary = stringResource(id = R.string.settings_custom_enable_high_contrast_summary),
                checked = themeConfig.isHighContrastMode,
                onCheckedChange = { isChecked ->
                    backgroundManager.saveEnableHighContrastMode(isChecked)
                },
            )
        }

        expandableItem(
            expanded = state.isCustomBackgroundEnabled,
            topContent = {
                CustomBackgroundSettings(
                    state = state,
                    viewModel = viewModel,
                    pickImageLauncher = pickImageLauncher,
                )
            },
            bottomContent = {
                backgroundAdjustmentControls(
                    state,
                    viewModel,
                    coroutineScope,
                )
            },
        )

        item(visible = themeConfig.isEnableBlur, topPadding = 1.dp) {
            SettingsSwitchRow(
                icon = Icons.TwoTone.Draw,
                title = stringResource(id = R.string.settings_exp_draw_background_to_blur),
                summary = stringResource(id = R.string.settings_exp_draw_background_to_blur_description),
                isError = true,
                checked = themeConfig.isEnableBlurExp,
                onCheckedChange = { isChecked ->
                    backgroundManager.saveEnableBlurExp(isChecked)
                },
            )
        }
    }
}

@Composable
private fun CustomizationSettings(
    homeUiState: HomeUiState,
    moduleUiState: ModuleUiState,
    settingsUiState: SettingsUiState,
    settingsViewModel: SettingsViewModel,
    homeViewModel: HomeViewModel,
    moduleViewModel: ModuleViewModel,
) {
    SegmentedColumn(title = stringResource(R.string.custom_settings)) {
        item {
            // 图标切换
            SettingsSwitchRow(
                icon = Icons.TwoTone.Android,
                title = stringResource(R.string.icon_switch_title),
                summary = stringResource(R.string.icon_switch_summary),
                checked = settingsUiState.useAltIcon,
                onCheckedChange = { enabled ->
                    settingsViewModel.dispatch(SettingsUiAction.SetAlternateIcon(enabled))
                },
            )
        }

        item {
            // 显示更多模块信息
            SettingsSwitchRow(
                icon = Icons.TwoTone.Info,
                title = stringResource(R.string.show_more_module_info),
                summary = stringResource(R.string.show_more_module_info_summary),
                checked = moduleUiState.showMoreModuleInfo,
                onCheckedChange = { enabled ->
                    moduleViewModel.dispatch(ModuleUiAction.SetShowMoreInfo(enabled))
                },
            )
        }

        item {
            // 使用内置Mono字体开关
            SettingsSwitchRow(
                icon = Icons.TwoTone.FormatSize,
                title = stringResource(R.string.settings_custom_monospace_font),
                summary = stringResource(R.string.settings_custom_monospace_font_summary),
                checked = settingsUiState.useBuiltinMonoFont,
                onCheckedChange = { settingsViewModel.handleBuiltinMonospaceFontChange(it) },
            )
        }

        item {
            // 简洁模式开关
            SettingsSwitchRow(
                icon = Icons.TwoTone.Brush,
                title = stringResource(R.string.simple_mode),
                summary = stringResource(R.string.simple_mode_summary),
                checked = homeUiState.isSimpleMode,
                onCheckedChange = { enabled ->
                    homeViewModel.dispatch(HomeUiAction.SetSimpleMode(enabled))
                },
            )
        }

        item {
            SettingsSwitchRow(
                icon = Icons.TwoTone.Pin,
                title = stringResource(R.string.navigation_bar_badge),
                summary = stringResource(R.string.navigation_bar_badge_summary),
                checked = homeUiState.showNavigationBarBadge,
                onCheckedChange = { enabled ->
                    homeViewModel.dispatch(HomeUiAction.SetNavigationBarBadge(enabled))
                },
            )
        }

        item {
            SettingsSwitchRow(
                icon = Icons.TwoTone.Badge,
                title = stringResource(R.string.home_card_icons),
                summary = stringResource(R.string.home_card_icons_summary),
                checked = homeUiState.showHomeCardIcons,
                onCheckedChange = { enabled ->
                    homeViewModel.dispatch(HomeUiAction.SetHomeCardIcons(enabled))
                },
            )
        }
    }
}

@Composable
private fun DpiSliderControls(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    coroutineScope: CoroutineScope,
) {
    val confirmDialog = rememberConfirmDialog()
    val dpiConfirmTitle = stringResource(R.string.dpi_confirm_title)
    val dpiConfirmMessage =
        stringResource(R.string.dpi_confirm_message, state.currentDpi, state.tempDpi)
    val confirmText = stringResource(R.string.confirm)
    val cancelText = stringResource(R.string.cancel)

    val sliderValue by animateFloatAsState(
        targetValue = state.tempDpi.toFloat(),
        label = "DPI Slider Animation",
    )

    KeyPointSlider(
        value = sliderValue,
        onValueChange = { newValue ->
            viewModel.dispatch(SettingsUiAction.SetTempDpi(newValue.toInt()))
        },
        modifier = Modifier.fillMaxWidth(),
        valueRange = 160f..600f,
        keyPoints = state.dpiPresets.map { (_, dpi) -> dpi.toFloat() },
    )

    // DPI 预设按钮行
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        state.dpiPresets.forEach { (nameResource, dpi) ->
            val isSelected = state.tempDpi == dpi
            val buttonColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceBright
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(buttonColor)
                    .clickable {
                        viewModel.dispatch(SettingsUiAction.SetTempDpi(dpi))
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(nameResource),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    Text(
        text = if (state.isDpiCustom) {
            "${stringResource(R.string.dpi_size_custom)}: ${state.tempDpi}"
        } else {
            "${stringResource(dpiFriendlyNameRes(state.tempDpi))}: ${state.tempDpi}"
        },
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 8.dp),
    )

    Button(
        onClick = {
            coroutineScope.launch {
                val confirmResult = confirmDialog.awaitConfirm(
                    title = dpiConfirmTitle,
                    content = dpiConfirmMessage,
                    confirm = confirmText,
                    dismiss = cancelText,
                )

                if (confirmResult != ConfirmResult.Confirmed) return@launch

                viewModel.dispatch(SettingsUiAction.ApplyDpi)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        enabled = state.tempDpi != state.currentDpi,
    ) {
        Icon(
            Icons.TwoTone.Check,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.dpi_apply_settings))
    }
}

@Composable
private fun LanguageSetting(state: SettingsUiState, viewModel: SettingsViewModel) {
    val isSystemLanguageSettings = koinInject<IsSystemLanguageSettingsUseCase>()
    val launchSystemLanguageSettings = koinInject<LaunchSystemLanguageSettingsUseCase>()
    val context = LocalContext.current
    val language = stringResource(id = R.string.settings_language)
    val languageSystemDefault = stringResource(R.string.language_system_default)

    // Compute display name based on current app locale
    val currentLanguageDisplay = remember(state.currentAppLocale) {
        val locale = state.currentAppLocale
        if (locale != null) {
            locale.getDisplayName(locale)
        } else {
            languageSystemDefault
        }
    }

    // Android 13+ delegates the choice to the system per-app language picker,
    // so the row jumps straight there instead of flashing an empty dialog.
    if (isSystemLanguageSettings()) {
        SettingsJumpRow(
            icon = Icons.TwoTone.Translate,
            title = language,
            summary = currentLanguageDisplay,
            onClick = { launchSystemLanguageSettings(context) },
        )
        return
    }

    val systemLanguage = stringResource(R.string.settings_language)

    // Locale tags paired with their self-named display strings, with the
    // system default leading the list.
    val options = remember {
        val locales = mutableListOf<java.util.Locale>()
        locales.add(java.util.Locale.ROOT)

        val resourceDirs = listOf(
            "ar", "bg", "de", "fa", "fr", "hu", "in", "it",
            "ja", "ko", "pl", "pt-rBR", "ru", "th", "tr",
            "uk", "vi", "zh-rCN", "zh-rTW",
        )

        resourceDirs.forEach { dir ->
            try {
                val locale = when {
                    dir.contains("-r") -> {
                        val parts = dir.split("-r")
                        java.util.Locale.Builder()
                            .setLanguage(parts[0])
                            .setRegion(parts[1])
                            .build()
                    }

                    else -> java.util.Locale.Builder()
                        .setLanguage(dir)
                        .build()
                }

                val config = android.content.res.Configuration()
                config.setLocale(locale)
                val localizedContext = context.createConfigurationContext(config)
                val testString = localizedContext.getString(R.string.settings_language)

                if (testString != systemLanguage || locale.language == "en") {
                    locales.add(locale)
                }
            } catch (_: Exception) {
            }
        }

        val sortedLocales = locales.drop(1).sortedBy { it.getDisplayName(it) }
        mutableListOf<java.util.Locale>().apply {
            add(locales.first())
            addAll(sortedLocales)
        }.map { locale ->
            val tag = if (locale == java.util.Locale.ROOT) {
                "system"
            } else if (locale.country.isEmpty()) {
                locale.language
            } else {
                "${locale.language}_${locale.country}"
            }

            val displayName = if (locale == java.util.Locale.ROOT) {
                languageSystemDefault
            } else {
                locale.getDisplayName(locale)
            }

            tag to displayName
        }
    }

    val currentLocaleTag = state.currentAppLocale?.let { locale ->
        if (locale.country.isEmpty()) locale.language else "${locale.language}_${locale.country}"
    } ?: "system"
    val selectedIndex = options.indexOfFirst { (tag, _) -> tag == currentLocaleTag }
        .coerceAtLeast(0)

    SettingsChooseRow(
        icon = Icons.TwoTone.Translate,
        title = language,
        summary = currentLanguageDisplay,
        items = options.map { (_, displayName) -> displayName },
        selectedIndex = selectedIndex,
        onSelectedIndexChange = { index ->
            val localeTag = options[index].first
            // Update local state immediately
            viewModel.dispatch(SettingsUiAction.SetLanguage(localeTag))
            // Apply locale change immediately for Android < 13
            viewModel.dispatch(SettingsUiAction.RestartActivity)
        },
    )
}

@Composable
private fun CustomBackgroundSettings(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    pickImageLauncher: ManagedActivityResultLauncher<String, Uri?>,
) {
    // TODO Portrait/Landscape wallpaper split

    SettingsSwitchRow(
        icon = Icons.TwoTone.Wallpaper,
        title = stringResource(id = R.string.settings_custom_background),
        summary = stringResource(id = R.string.settings_custom_background_summary),
        checked = state.isCustomBackgroundEnabled,
        onCheckedChange = { isChecked ->
            if (isChecked) {
                pickImageLauncher.launch("image/*")
            } else {
                viewModel.dispatch(SettingsUiAction.RemoveCustomBackground)
            }
        },
    )
}

private fun SegmentedColumnScope.backgroundAdjustmentControls(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    coroutineScope: CoroutineScope,
) {
    item(
        topPadding = 1.dp,
    ) {
        AlphaSlider(
            state = state,
            viewModel = viewModel,
            coroutineScope = coroutineScope,
        )
    }

    item(
        topPadding = 1.dp,
    ) {
        DimSlider(
            state = state,
            viewModel = viewModel,
            coroutineScope = coroutineScope,
        )
    }
}

@Composable
private fun AlphaSlider(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    coroutineScope: CoroutineScope,
) {
    SettingsSliderRow(
        icon = Icons.TwoTone.Opacity,
        title = stringResource(R.string.settings_card_alpha),
        value = state.cardAlpha,
        onValueChange = { newValue ->
            viewModel.dispatch(SettingsUiAction.SetCardAlpha(newValue))
        },
        onValueChangeFinished = {
            coroutineScope.launch(Dispatchers.IO) {
                viewModel.dispatch(SettingsUiAction.SaveCardConfig)
            }
        },
        valueRange = 0f..1f,
        keyPoints = listOf(0.25f, 0.5f, 0.75f),
        valueText = "${(state.cardAlpha * 100).roundToInt()}%",
    )
}

@Composable
private fun DimSlider(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    coroutineScope: CoroutineScope,
) {
    SettingsSliderRow(
        icon = Icons.TwoTone.LightMode,
        title = stringResource(R.string.settings_background_dim),
        value = state.backgroundDim,
        onValueChange = { newValue ->
            viewModel.dispatch(SettingsUiAction.SetBackgroundDim(newValue))
        },
        onValueChangeFinished = {
            coroutineScope.launch(Dispatchers.IO) {
                viewModel.dispatch(SettingsUiAction.SaveCardConfig)
            }
        },
        valueRange = 0f..1f,
        keyPoints = listOf(0.25f, 0.5f, 0.75f),
        valueText = "${(state.backgroundDim * 100).roundToInt()}%",
    )
}

@Composable
private fun AccentColorSetting(
    themeConfig: ThemeConfig,
) {
    val themeRepository = koinInject<ThemeRepository>()
    var showPicker by remember { mutableStateOf(false) }
    var pendingAccent by remember { mutableStateOf<Color?>(null) }

    SettingsJumpRow(
        icon = Icons.TwoTone.Palette,
        title = stringResource(R.string.theme_accent_color),
        summary = themeConfig.customAccentColor
            ?.let { accent -> "#%06X".format(accent.toArgb() and 0x00FFFFFF) }
            ?: stringResource(R.string.theme_accent_color_default),
        onClick = {
            pendingAccent = themeConfig.customAccentColor
            showPicker = true
        },
    )

    if (showPicker) {
        OverlayDialog(
            show = true,
            onDismissRequest = {
                // Outside tap and Cancel revert the live preview to the accent
                // the dialog opened with.
                themeConfig.customAccentColor = pendingAccent
                themeRepository.saveAccentColor(pendingAccent?.toArgb())
                showPicker = false
            },
            title = stringResource(R.string.choose_theme_color),
        ) {
            ColorPicker(
                color = themeConfig.customAccentColor ?: MiuixTheme.colorScheme.primary,
                onColorChanged = { color ->
                    // Live preview: feeds MiuixTheme and the bridged m3 primary
                    // instantly. The accent is forced opaque - a translucent theme
                    // accent would break Switch/Button contrast.
                    themeConfig.customAccentColor = color.copy(alpha = 1f)
                },
                showPreview = true,
                colorSpace = ColorSpace.OKHSV,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    text = stringResource(R.string.theme_accent_color_reset),
                    onClick = {
                        themeConfig.customAccentColor = null
                        themeRepository.saveAccentColor(null)
                        showPicker = false
                    },
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    text = stringResource(R.string.cancel),
                    onClick = {
                        themeConfig.customAccentColor = pendingAccent
                        themeRepository.saveAccentColor(pendingAccent?.toArgb())
                        showPicker = false
                    },
                )
                TextButton(
                    text = stringResource(id = android.R.string.ok),
                    onClick = {
                        themeRepository.saveAccentColor(themeConfig.customAccentColor?.toArgb())
                        showPicker = false
                    },
                )
            }
        }
    }
}

private fun Context.backgroundCropSize(): Pair<Int, Int> {
    val displayMetrics = resources.displayMetrics
    return displayMetrics.widthPixels to displayMetrics.heightPixels
}

private fun createBackgroundCropOutputUri(context: Context, prefix: String): Uri {
    val outputFile = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.jpg").apply {
        parentFile?.mkdirs()
        delete()
        createNewFile()
        deleteOnExit()
    }

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        outputFile,
    )
}
