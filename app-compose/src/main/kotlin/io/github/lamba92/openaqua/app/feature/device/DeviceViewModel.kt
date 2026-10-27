package io.github.lamba92.openaqua.app.feature.device

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.feature.device.common.toPwmFanItems
import io.github.lamba92.openaqua.app.feature.device.common.toSensorItems
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import io.github.lamba92.openaqua.app.repository.DeviceDataCacheService
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.app.repository.OpenAquaEngineApi
import io.github.lamba92.openaqua.model.DeviceId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update

class DeviceViewModel(
    @Suppress("unused") private val repository: OpenAquaEngineApi,
    cacheService: DeviceDataCacheService,
    private val appDataRepository: AppDataRepository,
    scopeProvider: CoroutineScopeProvider,
    private val deviceId: DeviceId,
) : MVIViewModel<DeviceUiState, DeviceUiEvent, DeviceUiEffect> {
    override val viewModelScope: CoroutineScope = scopeProvider.getScope()

    override val state: StateFlow<DeviceUiState>
        field = MutableStateFlow<DeviceUiState>(DeviceUiState.Loading)

    override val effects: SharedFlow<DeviceUiEffect>
        field = MutableSharedFlow<DeviceUiEffect>()

    init {
        combine(
            cacheService.getDeviceStateFlow(deviceId),
            appDataRepository.getDeviceDataFlow(deviceId),
        ) { deviceState, storedData ->
            state.update { current ->
                when (deviceState) {
                    is DeviceState.Connecting -> DeviceUiState.Loading
                    is DeviceState.Error ->
                        DeviceUiState.Error(
                            device = deviceState.device,
                            errorType = deviceState.errorType,
                        )
                    is DeviceState.Ready if current is DeviceUiState.Ready ->
                        current.copy(
                            deviceState = deviceState,
                            fans = deviceState.telemetry.fans.toPwmFanItems(deviceState.config.fans, storedData),
                            sensors = deviceState.telemetry.sensors.toSensorItems(deviceState.config.sensorOffsets, storedData),
                        )
                    is DeviceState.Ready ->
                        DeviceUiState.Ready(
                            deviceState = deviceState,
                            fans = deviceState.telemetry.fans.toPwmFanItems(deviceState.config.fans, storedData),
                            selectedFanChannel = 0,
                            sensors = deviceState.telemetry.sensors.toSensorItems(deviceState.config.sensorOffsets, storedData),
                            selectedSensorChannel = 0,
                        )
                }
            }
        }.launchIn(viewModelScope)
    }

    override fun onEvent(event: DeviceUiEvent) {
        when (event) {
            is DeviceUiEvent.SelectFan ->
                state.update { current ->
                    if (current is DeviceUiState.Ready) current.copy(selectedFanChannel = event.channel) else current
                }
            is DeviceUiEvent.SelectSensor ->
                state.update { current ->
                    if (current is DeviceUiState.Ready) current.copy(selectedSensorChannel = event.channel) else current
                }
            is DeviceUiEvent.RenameFan ->
                appDataRepository.setFanName(deviceId = deviceId, channel = event.channel, name = event.name)
            is DeviceUiEvent.RenameSensor ->
                appDataRepository.setSensorName(deviceId = deviceId, channel = event.channel, name = event.name)
        }
    }
}
