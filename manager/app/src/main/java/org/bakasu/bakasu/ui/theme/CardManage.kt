package org.bakasu.bakasu.ui.theme

import androidx.compose.material3.CardDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.bakasu.bakasu.data.AppSettingsRepository
import org.koin.compose.koinInject

@Stable
class CardConfig(
    private val settings: AppSettingsRepository,
) {
    // 卡片透明度 — custom background stack pruned; cards stay opaque
    val cardAlpha: Float
        get() = 1f

    // 卡片阴影
    var cardElevation by mutableStateOf(0.dp)
        internal set

    // 功能开关
    var isShadowEnabled by mutableStateOf(true)
        internal set
    var isUserDarkModeEnabled by mutableStateOf(false)
        internal set
    var isUserLightModeEnabled by mutableStateOf(false)
        internal set

    // 配置键名
    private object Keys {
        const val IS_SHADOW_ENABLED = "is_shadow_enabled"
        const val IS_USER_DARK_MODE_ENABLED = "is_user_dark_mode_enabled"
        const val IS_USER_LIGHT_MODE_ENABLED = "is_user_light_mode_enabled"
    }

    fun updateShadow(enabled: Boolean, elevation: Dp = cardElevation) {
        isShadowEnabled = enabled
        cardElevation = if (enabled) elevation else cardElevation
    }

    fun updateThemePreference(darkMode: Boolean?, lightMode: Boolean?) {
        isUserDarkModeEnabled = darkMode ?: false
        isUserLightModeEnabled = lightMode ?: false
    }

    fun reset() {
        cardElevation = 0.dp
        isShadowEnabled = true
        isUserDarkModeEnabled = false
        isUserLightModeEnabled = false
    }

    fun setThemeDefaults(isDarkMode: Boolean) {
        // 暗色模式下默认启用轻微阴影
        if (isDarkMode) {
            updateShadow(true, 2.dp)
        }
    }

    fun save() {
        val prefs = settings
        prefs.putBoolean(Keys.IS_SHADOW_ENABLED, isShadowEnabled)
        prefs.putBoolean(Keys.IS_USER_DARK_MODE_ENABLED, isUserDarkModeEnabled)
        prefs.putBoolean(Keys.IS_USER_LIGHT_MODE_ENABLED, isUserLightModeEnabled)
    }

    fun load() {
        val prefs = settings
        isShadowEnabled = prefs.getBoolean(Keys.IS_SHADOW_ENABLED, true)
        isUserDarkModeEnabled = prefs.getBoolean(Keys.IS_USER_DARK_MODE_ENABLED, false)
        isUserLightModeEnabled = prefs.getBoolean(Keys.IS_USER_LIGHT_MODE_ENABLED, false)

        // 应用阴影设置
        updateShadow(isShadowEnabled, if (isShadowEnabled) cardElevation else 0.dp)
    }

    @Deprecated("使用 updateShadow 替代", ReplaceWith("updateShadow(enabled)"))
    fun updateShadowEnabled(enabled: Boolean) {
        updateShadow(enabled)
    }
}

object CardStyleProvider {

    @Composable
    fun getCardColors(
        originalColor: Color,
        transparent: Boolean,
    ): androidx.compose.material3.CardColors {
        val cardConfig = koinInject<CardConfig>()
        return CardDefaults.cardColors(
            containerColor = if (transparent) Color.Transparent else originalColor.copy(alpha = cardConfig.cardAlpha),
            contentColor = contentColorFor(originalColor),
            disabledContainerColor = if (transparent) {
                Color.Transparent
            } else {
                originalColor.copy(
                    alpha = cardConfig.cardAlpha * 0.38f,
                )
            },
            disabledContentColor = contentColorFor(originalColor).copy(alpha = 0.38f),
        )
    }

    @Composable
    fun getCardElevation(): androidx.compose.material3.CardElevation {
        val cardConfig = koinInject<CardConfig>()
        return CardDefaults.cardElevation(
            defaultElevation = cardConfig.cardElevation,
            pressedElevation = if (cardConfig.isShadowEnabled) {
                (cardConfig.cardElevation.value + 0).dp
            } else {
                0.dp
            },
            focusedElevation = if (cardConfig.isShadowEnabled) {
                (cardConfig.cardElevation.value + 0).dp
            } else {
                0.dp
            },
            hoveredElevation = if (cardConfig.isShadowEnabled) {
                (cardConfig.cardElevation.value + 0).dp
            } else {
                0.dp
            },
            draggedElevation = if (cardConfig.isShadowEnabled) {
                (cardConfig.cardElevation.value + 0).dp
            } else {
                0.dp
            },
            disabledElevation = 0.dp,
        )
    }
}

@Composable
fun getCardColors(
    originalColor: Color,
    renderBackground: Boolean = true,
): androidx.compose.material3.CardColors {
    val themeConfig = koinInject<ThemeConfig>()
    return CardStyleProvider.getCardColors(
        originalColor,
        renderBackground && themeConfig.isEnableBlurExp,
    )
}

@Composable
fun getCardElevation() = CardStyleProvider.getCardElevation()
