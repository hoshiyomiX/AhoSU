package org.bakasu.bakasu.domain.model

data class SettingsPlatformSnapshot(
    val dpi: Int = 0,
    val predictiveBackAnimation: String = "",
    val predictiveBackExitDirection: String = "",
    val themeMode: Int = 0,
    val currentLocaleTag: String? = null,
    val useAltIcon: Boolean = false,
    val cardAlpha: Float = 1f,
    val backgroundDim: Float = 0f,
    val customBackgroundEnabled: Boolean = false,
    val systemDpi: Int = 0,
    val currentDpi: Int = 0,
    val checkManagerUpdate: Boolean = true,
    val checkBetaUpdate: Boolean = true,
    val checkModuleUpdate: Boolean = true,
    val autoJailbreakEnabled: Boolean = false,
    val useBuiltinMonoFont: Boolean = false,
    val useSoftReboot: Boolean = false,
    val enableSwipeDismiss: Boolean = true,
    val pagerInterceptionMode: Int = 2,
)

data class PlatformFeatureStatus(
    val suCompatPersistValue: Long? = null,
    val suStatus: String = "",
    val kernelUmountStatus: String = "",
    val adbRootStatus: String = "",
    val adbRootEnabled: Boolean = false,
    val sulogStatus: String = "",
    val selinuxHideStatus: String = "",
)

sealed interface AppearanceSetting {
    data class ThemeMode(val index: Int) : AppearanceSetting
    data class CustomBackground(val uri: String) : AppearanceSetting
    data object RemoveCustomBackground : AppearanceSetting
    data class CardAlpha(val value: Float) : AppearanceSetting
    data class BackgroundDim(val value: Float) : AppearanceSetting
    data object SaveCardConfig : AppearanceSetting
}

sealed interface PlatformSetting {
    data object InitializeFirstRun : PlatformSetting
    data class PredictiveBackAnimation(val value: String) : PlatformSetting
    data class PredictiveBackExitDirection(val value: String) : PlatformSetting
    data class Dpi(val value: Int) : PlatformSetting
    data class AlternateIcon(val enabled: Boolean) : PlatformSetting
    data class ManagerUpdateCheck(val enabled: Boolean) : PlatformSetting
    data class BetaUpdateCheck(val enabled: Boolean) : PlatformSetting
    data class ModuleUpdateCheck(val enabled: Boolean) : PlatformSetting
    data class Locale(val tag: String) : PlatformSetting
    data class AutoJailbreak(val enabled: Boolean) : PlatformSetting
    data class AdbRoot(val enabled: Boolean) : PlatformSetting
    data class SuCompatMode(val value: Int) : PlatformSetting
    data class BuiltinMonospaceFont(val enabled: Boolean) : PlatformSetting
    data class UseSoftReboot(val enabled: Boolean) : PlatformSetting
    data class SwipeDismiss(val enabled: Boolean) : PlatformSetting
    data class PagerInterceptionMode(val value: Int) : PlatformSetting
}
