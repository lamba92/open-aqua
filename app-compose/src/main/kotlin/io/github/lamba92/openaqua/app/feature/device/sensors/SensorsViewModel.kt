package io.github.lamba92.openaqua.app.feature.device.sensors

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.LoggingScope
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.feature.device.common.toSensorItems
import io.github.lamba92.openaqua.app.model.DeviceStoredData
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.mvi.viewModelLogger
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import io.github.lamba92.openaqua.app.repository.DeviceDataCacheService
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.model.device.DeviceId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SensorsViewModel(
    cacheService: DeviceDataCacheService,
    private val appDataRepository: AppDataRepository,
    scopeProvider: CoroutineScopeProvider,
    private val deviceId: DeviceId,
) : MVIViewModel<SensorsUiState, SensorsUiEvent, SensorsUiEffect> {
    override val viewModelScope: LoggingScope = scopeProvider.getScope()

    override val state: StateFlow<SensorsUiState>
        field = MutableStateFlow<SensorsUiState>(SensorsUiState.Loading)

    override val effects: SharedFlow<SensorsUiEffect>
        field = MutableSharedFlow<SensorsUiEffect>()

    private data class CombinedPreferences(
        val storedData: DeviceStoredData,
        val hideDisconnectedSensors: Boolean,
        val hideUnconfiguredVirtualSensors: Boolean,
    )

    init {
        val preferencesFlow =
            appDataRepository.state
                .filterIsInstance<AppDataRepository.State.Ready>()
                .map { ready ->
                    CombinedPreferences(
                        storedData = ready.data.devices[deviceId] ?: DeviceStoredData(),
                        hideDisconnectedSensors = ready.data.hideDisconnectedSensors,
                        hideUnconfiguredVirtualSensors = ready.data.hideUnconfiguredVirtualSensors,
                    )
                }.distinctUntilChanged()

        combine(
            cacheService.getAquaDeviceStateFlow(deviceId),
            preferencesFlow,
        ) { deviceState, preferences ->
            state.update { current ->
                when (deviceState) {
                    is DeviceState.Aqua.Connecting, is DeviceState.Aqua.Error -> SensorsUiState.Loading
                    is DeviceState.Aqua.Ready -> {
                        val sensors = deviceState.telemetry.sensors.toSensorItems(deviceState.config.sensorOffsets, preferences.storedData)

                        if (current is SensorsUiState.Ready) {
                            current.copy(
                                sensors = sensors,
                                hideDisconnectedSensors = preferences.hideDisconnectedSensors,
                                hideUnconfiguredVirtualSensors = preferences.hideUnconfiguredVirtualSensors,
                            )
                        } else {
                            SensorsUiState.Ready(
                                sensors = sensors,
                                selectedSensorChannel = 0,
                                hideDisconnectedSensors = preferences.hideDisconnectedSensors,
                                hideUnconfiguredVirtualSensors = preferences.hideUnconfiguredVirtualSensors,
                            )
                        }
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    override fun onEvent(event: SensorsUiEvent): Job =
        viewModelScope.launch {
            when (event) {
                is SensorsUiEvent.SelectSensor ->
                    state.update { current ->
                        if (current is SensorsUiState.Ready) current.copy(selectedSensorChannel = event.channel) else current
                    }

                is SensorsUiEvent.StartRenamingSensor ->
                    state.update { current ->
                        if (current is SensorsUiState.Ready) current.copy(renamingChannel = event.channel) else current
                    }

                is SensorsUiEvent.DismissRenameDialog ->
                    state.update { current ->
                        if (current is SensorsUiState.Ready) current.copy(renamingChannel = null) else current
                    }

                is SensorsUiEvent.RenameSensor -> {
                    viewModelLogger.i { "Renamed sensor channel ${event.channel} on device '$deviceId' to '${event.name}'" }
                    appDataRepository.setSensorName(deviceId = deviceId, channel = event.channel, name = event.name)
                    state.update { current ->
                        if (current is SensorsUiState.Ready) current.copy(renamingChannel = null) else current
                    }
                }

                SensorsUiEvent.ToggleShowAllSensors -> {
                    val currentState = state.value as? SensorsUiState.Ready ?: return@launch
                    val isHidingAny = currentState.hideDisconnectedSensors || currentState.hideUnconfiguredVirtualSensors
                    viewModelLogger.i { "Toggled sensor visibility on device '$deviceId' (hideDisconnected=${!isHidingAny})" }
                    if (isHidingAny) {
                        appDataRepository.setHideDisconnectedSensors(false)
                        appDataRepository.setHideUnconfiguredVirtualSensors(false)
                    } else {
                        appDataRepository.setHideDisconnectedSensors(true)
                        appDataRepository.setHideUnconfiguredVirtualSensors(true)
                    }
                }
            }
        }
}
