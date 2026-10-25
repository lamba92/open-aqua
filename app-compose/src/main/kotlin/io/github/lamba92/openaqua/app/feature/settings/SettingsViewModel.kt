package io.github.lamba92.openaqua.app.feature.settings

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.io.path.absolutePathString

class SettingsViewModel(
    private val appDataRepository: AppDataRepository,
    scopeProvider: CoroutineScopeProvider,
) : MVIViewModel<SettingsState, SettingsEvent, SettingsEffect> {
    override val viewModelScope: CoroutineScope = scopeProvider.getScope()

    override val state: StateFlow<SettingsState>
        field = MutableStateFlow(SettingsState(dataFolderPath = appDataRepository.storageDirectory.absolutePathString()))

    override val effects: SharedFlow<SettingsEffect>
        field = MutableSharedFlow<SettingsEffect>()

    override fun onEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.OpenDataFolder -> appDataRepository.openDataFolder()
            SettingsEvent.RequestRestoreDefaults -> state.update { it.copy(showResetConfirmationDialog = true) }
            SettingsEvent.DismissRestoreDefaults -> state.update { it.copy(showResetConfirmationDialog = false) }
            SettingsEvent.ConfirmRestoreDefaults -> {
                appDataRepository.resetToDefaults()
                state.update { it.copy(showResetConfirmationDialog = false) }
            }
        }
    }
}
