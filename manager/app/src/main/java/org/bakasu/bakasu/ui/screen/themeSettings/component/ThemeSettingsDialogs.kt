package org.bakasu.bakasu.ui.screen.themeSettings.component

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.bakasu.bakasu.R
import org.bakasu.bakasu.domain.usecase.IsSystemLanguageSettingsUseCase
import org.bakasu.bakasu.domain.usecase.LaunchSystemLanguageSettingsUseCase
import org.bakasu.bakasu.ui.component.settings.SettingsChooseDialog
import org.bakasu.bakasu.ui.theme.ThemeConfig
import org.bakasu.bakasu.ui.viewmodel.SettingsUiAction
import org.bakasu.bakasu.ui.viewmodel.SettingsUiState
import org.bakasu.bakasu.ui.viewmodel.SettingsViewModel
import org.koin.compose.koinInject

@Composable
fun LanguageSelectionDialog(
    currentLocale: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val isSystemLanguageSettingsUseCase =
        koinInject<IsSystemLanguageSettingsUseCase>()
    val launchSystemLanguageSettingsUseCase =
        koinInject<LaunchSystemLanguageSettingsUseCase>()
    val context = LocalContext.current
    val languageUseSystemDefault = stringResource(R.string.language_system_default)
    val systemLanguage = stringResource(R.string.settings_language)

    if (isSystemLanguageSettingsUseCase()) {
        launchSystemLanguageSettingsUseCase(context)
        onDismiss()
    } else {
        val supportedLocales = remember {
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
            }
        }

        val allOptions = supportedLocales.map { locale ->
            val tag = if (locale == java.util.Locale.ROOT) {
                "system"
            } else if (locale.country.isEmpty()) {
                locale.language
            } else {
                "${locale.language}_${locale.country}"
            }

            val displayName = if (locale == java.util.Locale.ROOT) {
                languageUseSystemDefault
            } else {
                locale.getDisplayName(locale)
            }

            tag to displayName
        }

        var selectedIndex by remember {
            mutableIntStateOf(allOptions.indexOfFirst { (tag, _) -> currentLocale == tag })
        }

        SettingsChooseDialog(
            show = true,
            title = stringResource(R.string.settings_language),
            items = allOptions.map { (_, displayName) -> displayName },
            selectedIndex = selectedIndex,
            onDismiss = onDismiss,
            onSelectedIndexChange = { index ->
                selectedIndex = index
                if (selectedIndex >= 0 && selectedIndex < allOptions.size) {
                    val newLocale = allOptions[selectedIndex].first
                    onLanguageSelected(newLocale)
                }
            },
        )
    }
}
