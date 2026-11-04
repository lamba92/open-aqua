package io.github.lamba92.openaqua.app.feature.settings

import io.github.lamba92.openaqua.app.model.AppLanguage
import io.github.lamba92.openaqua.app.theme.AppFontFamily
import io.github.lamba92.openaqua.app.theme.AppMetricsScale
import io.github.lamba92.openaqua.app.theme.FontScaleConstants
import io.github.lamba92.openaqua.theme.AppColorScheme

enum class SettingsTab {
    GENERAL,
    LOGS,
}

data class SettingsState(
    val showResetConfirmationDialog: Boolean = false,
    val dataFolderPath: String,
    val colorScheme: AppColorScheme = AppColorScheme.HYDRO_DARK,
    val metricsScale: AppMetricsScale = AppMetricsScale.STANDARD,
    val fontSp: Int = FontScaleConstants.DEFAULT_SP,
    val fontFamily: AppFontFamily = AppFontFamily.JETBRAINS_MONO,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val hideDisconnectedFans: Boolean = false,
    val hideDisconnectedSensorsInFans: Boolean = false,
    val hideDisconnectedSensors: Boolean = false,
    val hideUnconfiguredVirtualSensors: Boolean = false,
    val logs: List<String> = emptyList(),
)

sealed interface SettingsEvent {
    data object OpenDataFolder : SettingsEvent

    data object RequestRestoreDefaults : SettingsEvent

    data object OpenLogsFolder : SettingsEvent

    data object DismissRestoreDefaults : SettingsEvent

    data object ConfirmRestoreDefaults : SettingsEvent

    data class SelectColorScheme(
        val scheme: AppColorScheme,
    ) : SettingsEvent

    data class SelectMetricsScale(
        val scale: AppMetricsScale,
    ) : SettingsEvent

    data class SetFontSp(
        val sp: Int,
    ) : SettingsEvent

    data class SelectFontFamily(
        val family: AppFontFamily,
    ) : SettingsEvent

    data class SelectLanguage(
        val language: AppLanguage,
    ) : SettingsEvent

    data class ToggleHideDisconnectedFans(
        val hide: Boolean,
    ) : SettingsEvent

    data class ToggleHideDisconnectedSensorsInFans(
        val hide: Boolean,
    ) : SettingsEvent

    data class ToggleHideDisconnectedSensors(
        val hide: Boolean,
    ) : SettingsEvent

    data class ToggleHideUnconfiguredVirtualSensors(
        val hide: Boolean,
    ) : SettingsEvent
}

sealed interface SettingsEffect
