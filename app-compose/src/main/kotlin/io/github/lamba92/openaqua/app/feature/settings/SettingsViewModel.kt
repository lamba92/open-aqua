package io.github.lamba92.openaqua.app.feature.settings

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.LoggingScope
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.logging.AppLogService
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.mvi.viewModelLogger
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val appDataRepository: AppDataRepository,
    private val appLogService: AppLogService,
    scopeProvider: CoroutineScopeProvider,
) : MVIViewModel<SettingsState, SettingsEvent, SettingsEffect> {
    override val viewModelScope: LoggingScope = scopeProvider.getScope()

    override val state: StateFlow<SettingsState>
        field = MutableStateFlow(SettingsState(dataFolderPath = appDataRepository.baseAppDataDir.path.toString()))

    override val effects: SharedFlow<SettingsEffect>
        field = MutableSharedFlow<SettingsEffect>()

    init {
        appDataRepository.state
            .filterIsInstance<AppDataRepository.State.Ready>()
            .onEach { ready ->
                state.update {
                    it.copy(
                        colorScheme = ready.data.colorScheme,
                        metricsScale = ready.data.metricsScale,
                        fontSp = ready.data.fontSp,
                        fontFamily = ready.data.fontFamily,
                        language = ready.data.language,
                        hideDisconnectedFans = ready.data.hideDisconnectedFans,
                        hideDisconnectedSensorsInFans = ready.data.hideDisconnectedSensorsInFans,
                        hideDisconnectedSensors = ready.data.hideDisconnectedSensors,
                        hideUnconfiguredVirtualSensors = ready.data.hideUnconfiguredVirtualSensors,
                    )
                }
            }.launchIn(viewModelScope)

        appLogService.logLines
            .onEach { lines -> state.update { it.copy(logs = lines) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: SettingsEvent): Job =
        viewModelScope.launch {
            when (event) {
                SettingsEvent.OpenDataFolder -> {
                    viewModelLogger.i { "Opening app data folder: ${state.value.dataFolderPath}" }
                    appDataRepository.openDataFolder()
                }

                SettingsEvent.OpenLogsFolder -> {
                    viewModelLogger.i { "Opening logs folder: ${appLogService.logsDirectory.value}" }
                    appLogService.openLogsFolder()
                }

                SettingsEvent.RequestRestoreDefaults -> {
                    state.update { it.copy(showResetConfirmationDialog = true) }
                }

                SettingsEvent.DismissRestoreDefaults -> {
                    state.update { it.copy(showResetConfirmationDialog = false) }
                }

                SettingsEvent.ConfirmRestoreDefaults -> {
                    viewModelLogger.i { "Restored settings to defaults" }
                    appDataRepository.resetToDefaults()
                    state.update { it.copy(showResetConfirmationDialog = false) }
                }

                is SettingsEvent.SelectColorScheme -> {
                    viewModelLogger.i { "Selected color scheme: ${event.scheme}" }
                    appDataRepository.update { it.copy(colorScheme = event.scheme) }
                }

                is SettingsEvent.SelectMetricsScale -> {
                    viewModelLogger.i { "Selected metrics scale: ${event.scale}" }
                    appDataRepository.update { it.copy(metricsScale = event.scale) }
                }

                is SettingsEvent.SetFontSp -> {
                    viewModelLogger.i { "Updated font size: ${event.sp} sp" }
                    appDataRepository.update { it.copy(fontSp = event.sp) }
                }

                is SettingsEvent.SelectFontFamily -> {
                    viewModelLogger.i { "Selected font family: ${event.family}" }
                    appDataRepository.update { it.copy(fontFamily = event.family) }
                }

                is SettingsEvent.SelectLanguage -> {
                    viewModelLogger.i { "Selected language: ${event.language}" }
                    appDataRepository.update { it.copy(language = event.language) }
                }

                is SettingsEvent.ToggleHideDisconnectedFans -> {
                    viewModelLogger.i { "Set hide disconnected fans: ${event.hide}" }
                    appDataRepository.setHideDisconnectedFans(event.hide)
                }

                is SettingsEvent.ToggleHideDisconnectedSensorsInFans -> {
                    viewModelLogger.i { "Set hide disconnected sensors in fans: ${event.hide}" }
                    appDataRepository.setHideDisconnectedSensorsInFans(event.hide)
                }

                is SettingsEvent.ToggleHideDisconnectedSensors -> {
                    viewModelLogger.i { "Set hide disconnected sensors: ${event.hide}" }
                    appDataRepository.setHideDisconnectedSensors(event.hide)
                }

                is SettingsEvent.ToggleHideUnconfiguredVirtualSensors -> {
                    viewModelLogger.i { "Set hide unconfigured virtual sensors: ${event.hide}" }
                    appDataRepository.setHideUnconfiguredVirtualSensors(event.hide)
                }
            }
        }
}
