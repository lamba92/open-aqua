package io.github.lamba92.openaqua.app.feature.device.sensors

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.feature.device.common.toSensorItems
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import io.github.lamba92.openaqua.app.repository.DeviceDataCacheService
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.model.DeviceId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update

class SensorsViewModel(
    cacheService: DeviceDataCacheService,
    private val appDataRepository: AppDataRepository,
    scopeProvider: CoroutineScopeProvider,
    private val deviceId: DeviceId,
) : MVIViewModel<SensorsUiState, SensorsUiEvent, SensorsUiEffect> {
    override val viewModelScope: CoroutineScope = scopeProvider.getScope()

    override val state: StateFlow<SensorsUiState>
        field = MutableStateFlow<SensorsUiState>(SensorsUiState.Loading)

    override val effects: SharedFlow<SensorsUiEffect>
        field = MutableSharedFlow<SensorsUiEffect>()

    init {
        combine(
            cacheService.getDeviceStateFlow(deviceId),
            appDataRepository.getDeviceDataFlow(deviceId),
        ) { deviceState, storedData ->
            state.update { current ->
                when (deviceState) {
                    is DeviceState.Connecting, is DeviceState.Error -> SensorsUiState.Loading
                    is DeviceState.Ready -> {
                        val sensors = deviceState.telemetry.sensors.toSensorItems(deviceState.config.sensorOffsets, storedData)

                        if (current is SensorsUiState.Ready) {
                            current.copy(sensors = sensors)
                        } else {
                            SensorsUiState.Ready(
                                sensors = sensors,
                                selectedSensorChannel = 0,
                            )
                        }
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    override fun onEvent(event: SensorsUiEvent) {
        when (event) {
            is SensorsUiEvent.SelectSensor ->
                state.update { current ->
                    if (current is SensorsUiState.Ready) current.copy(selectedSensorChannel = event.channel) else current
                }

            is SensorsUiEvent.RenameSensor ->
                appDataRepository.setSensorName(deviceId = deviceId, channel = event.channel, name = event.name)
        }
    }
}
