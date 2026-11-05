package org.bakasu.bakasu.ui.theme

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.bakasu.bakasu.data.AppSettingsRepository

@Stable
class CardConfig(
    private val settings: AppSettingsRepository,
) {
    // 卡片透明度
    private var _cardAlpha by mutableFloatStateOf(1f)

    var cardAlpha: Float
        get() = run {
            if (isCustomBackgroundEnabled) {
                _cardAlpha
            } else {
                1f
            }
        }
        set(value) {
            _cardAlpha = value.coerceIn(0f, 1f)
        }

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
    var isCustomBackgroundEnabled by mutableStateOf(false)
        internal set

    var isCustomAlphaSet by mutableStateOf(false)
        internal set

    // 配置键名
    private object Keys {
        const val CARD_ALPHA = "card_alpha"
        const val IS_SHADOW_ENABLED = "is_shadow_enabled"
        const val CUSTOM_BACKGROUND_ENABLED = "custom_background_enabled"
        const val IS_USER_DARK_MODE_ENABLED = "is_user_dark_mode_enabled"
        const val IS_USER_LIGHT_MODE_ENABLED = "is_user_light_mode_enabled"
        const val IS_CUSTOM_ALPHA_SET = "is_custom_alpha_set"
    }

    fun updateShadow(enabled: Boolean, elevation: Dp = cardElevation) {
        isShadowEnabled = enabled
        cardElevation = if (enabled) elevation else cardElevation
    }

    fun updateThemePreference(darkMode: Boolean?, lightMode: Boolean?) {
        isUserDarkModeEnabled = darkMode ?: false
        isUserLightModeEnabled = lightMode ?: false
    }

    fun updateAlpha(alpha: Float, isCustom: Boolean = true) {
        cardAlpha = alpha.coerceIn(0f, 1f)
        if (isCustom) isCustomAlphaSet = true
    }

    fun updateBackground(enabled: Boolean) {
        isCustomBackgroundEnabled = enabled
        if (enabled) {
            if (!isCustomAlphaSet) {
                updateAlpha(0.55f, isCustom = false)
            }
            updateShadow(false)
        }
    }

    fun reset() {
        cardElevation = 0.dp
        cardAlpha = 1f
        isShadowEnabled = true
        isUserDarkModeEnabled = false
        isUserLightModeEnabled = false
        isCustomBackgroundEnabled = false
        isCustomAlphaSet = false
    }

    fun setThemeDefaults(isDarkMode: Boolean) {
        // 暗色模式下默认启用轻微阴影
        if (isDarkMode && !isCustomBackgroundEnabled) {
            updateShadow(true, 2.dp)
        }
    }

    fun save() {
        val prefs = settings
        prefs.putFloat(Keys.CARD_ALPHA, cardAlpha)
        prefs.putBoolean(Keys.IS_SHADOW_ENABLED, isShadowEnabled)
        prefs.putBoolean(Keys.CUSTOM_BACKGROUND_ENABLED, isCustomBackgroundEnabled)
        prefs.putBoolean(Keys.IS_USER_DARK_MODE_ENABLED, isUserDarkModeEnabled)
        prefs.putBoolean(Keys.IS_USER_LIGHT_MODE_ENABLED, isUserLightModeEnabled)
        prefs.putBoolean(Keys.IS_CUSTOM_ALPHA_SET, isCustomAlphaSet)
    }

    fun load() {
        val prefs = settings
        cardAlpha = prefs.getFloat(Keys.CARD_ALPHA, 1f).coerceIn(0f, 1f)
        isShadowEnabled = prefs.getBoolean(Keys.IS_SHADOW_ENABLED, true)
        isCustomBackgroundEnabled = prefs.getBoolean(Keys.CUSTOM_BACKGROUND_ENABLED, false)
        isUserDarkModeEnabled = prefs.getBoolean(Keys.IS_USER_DARK_MODE_ENABLED, false)
        isUserLightModeEnabled = prefs.getBoolean(Keys.IS_USER_LIGHT_MODE_ENABLED, false)
        isCustomAlphaSet = prefs.getBoolean(Keys.IS_CUSTOM_ALPHA_SET, false)

        if (!isCustomAlphaSet) {
            updateAlpha(
                if (isCustomBackgroundEnabled) {
                    0.55f
                } else if (isUserDarkModeEnabled) {
                    0.88f
                } else {
                    1f
                },
                isCustom = false,
            )
        }

        // 应用阴影设置
        updateShadow(isShadowEnabled, if (isShadowEnabled) cardElevation else 0.dp)
    }

    @Deprecated("使用 updateShadow 替代", ReplaceWith("updateShadow(enabled)"))
    fun updateShadowEnabled(enabled: Boolean) {
        updateShadow(enabled)
    }
}
