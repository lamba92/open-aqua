package io.github.lamba92.openaqua.app.feature.settings

data class SettingsState(
    val showResetConfirmationDialog: Boolean = false,
    val dataFolderPath: String,
)

sealed interface SettingsEvent {
    data object OpenDataFolder : SettingsEvent

    data object RequestRestoreDefaults : SettingsEvent

    data object DismissRestoreDefaults : SettingsEvent

    data object ConfirmRestoreDefaults : SettingsEvent
}

sealed interface SettingsEffect
