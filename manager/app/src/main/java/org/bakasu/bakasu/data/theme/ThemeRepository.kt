package org.bakasu.bakasu.data.theme

import org.bakasu.bakasu.data.AppSettingsRepository

class ThemeRepository(
    private val settings: AppSettingsRepository,
) {
    fun saveThemeMode(forceDark: Boolean?) {
        settings.putString(
            "theme_mode",
            when (forceDark) {
                true -> "dark"
                false -> "light"
                null -> "system"
            },
        )
    }

    fun loadThemeMode(): Boolean? = when (settings.getString("theme_mode", "system")) {
        "dark" -> true
        "light" -> false
        else -> null
    }
}
