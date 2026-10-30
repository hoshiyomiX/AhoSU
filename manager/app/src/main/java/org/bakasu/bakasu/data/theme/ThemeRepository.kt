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

    /**
     * Persists the user-picked accent color as an 8-digit ARGB hex string, or the
     * literal "default" when the miuix palette accent should be used.
     */
    fun saveAccentColor(color: Int?) {
        settings.putString(
            "accent_color",
            color?.let { raw -> "%08X".format(raw.toLong() and 0xFFFFFFFFL) } ?: "default",
        )
    }

    fun loadAccentColor(): Int? {
        val raw = settings.getString("accent_color", "default") ?: return null
        if (raw.length != 8) return null
        return raw.toLongOrNull(16)?.takeIf { it in 1..0xFFFFFFFFL }?.toInt()
    }
}
