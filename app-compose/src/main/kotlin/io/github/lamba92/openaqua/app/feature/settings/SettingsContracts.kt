package io.github.lamba92.openaqua.app.feature.settings

import co.touchlab.kermit.Severity
import io.github.lamba92.openaqua.app.model.AppLanguage
import io.github.lamba92.openaqua.app.model.AppStoredData
import io.github.lamba92.openaqua.app.theme.AppFontFamily
import io.github.lamba92.openaqua.app.theme.AppMetricsScale
import io.github.lamba92.openaqua.theme.AppColorScheme

enum class SettingsTab {
    GENERAL,
    LOGS,
}

data class LogLine(
    val text: String,
    val severity: Severity? = null,
)

sealed interface CurrentOSSettings {
    data object None : CurrentOSSettings

    data class Linux(
        val hasUdevRule: Boolean,
        val isRemovingPermissions: Boolean = false,
    ) : CurrentOSSettings
}

sealed interface SettingsState {
    data object Loading : SettingsState

    data class Ready(
        val showResetConfirmationDialog: Boolean = false,
        val showRemoveUdevRuleConfirmationDialog: Boolean = false,
        val dataFolderPath: String,
        val storedData: AppStoredData,
        val osSettings: CurrentOSSettings,
        val logs: List<LogLine>,
    ) : SettingsState
}

sealed interface SettingsEvent {
    data object OpenDataFolder : SettingsEvent

    data object RequestRestoreDefaults : SettingsEvent

    data object OpenLogsFolder : SettingsEvent

    data object DismissRestoreDefaults : SettingsEvent

    data object ConfirmRestoreDefaults : SettingsEvent

    data object RequestRemoveUdevRule : SettingsEvent

    data object DismissRemoveUdevRule : SettingsEvent

    data object ConfirmRemoveUdevRule : SettingsEvent

    @JvmInline
    value class SelectColorScheme(
        val scheme: AppColorScheme,
    ) : SettingsEvent

    @JvmInline
    value class SelectMetricsScale(
        val scale: AppMetricsScale,
    ) : SettingsEvent

    @JvmInline
    value class SetFontSp(
        val sp: Int,
    ) : SettingsEvent

    @JvmInline
    value class SelectFontFamily(
        val family: AppFontFamily,
    ) : SettingsEvent

    @JvmInline
    value class SelectLanguage(
        val language: AppLanguage,
    ) : SettingsEvent

    @JvmInline
    value class ToggleHideDisconnectedFans(
        val hide: Boolean,
    ) : SettingsEvent

    @JvmInline
    value class ToggleHideDisconnectedSensorsInFans(
        val hide: Boolean,
    ) : SettingsEvent

    @JvmInline
    value class ToggleHideDisconnectedSensors(
        val hide: Boolean,
    ) : SettingsEvent

    @JvmInline
    value class ToggleHideUnconfiguredVirtualSensors(
        val hide: Boolean,
    ) : SettingsEvent
}

sealed interface SettingsEffect
