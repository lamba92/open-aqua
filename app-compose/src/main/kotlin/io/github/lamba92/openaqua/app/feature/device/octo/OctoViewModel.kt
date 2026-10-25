package io.github.lamba92.openaqua.app.feature.device.octo

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

class OctoViewModel(
    @Suppress("unused") private val repository: OpenAquaEngineApi,
    cacheService: DeviceDataCacheService,
    private val appDataRepository: AppDataRepository,
    scopeProvider: CoroutineScopeProvider,
    private val deviceId: DeviceId,
) : MVIViewModel<OctoState, OctoEvent, OctoEffect> {
    override val viewModelScope: CoroutineScope = scopeProvider.getScope()

    override val state: StateFlow<OctoState>
        field = MutableStateFlow<OctoState>(OctoState.Loading)

    override val effects: SharedFlow<OctoEffect>
        field = MutableSharedFlow<OctoEffect>()

    init {
        combine(
            cacheService.getDeviceStateFlow(deviceId),
            appDataRepository.getDeviceDataFlow(deviceId),
        ) { deviceState, storedData ->
            state.update { current ->
                when (deviceState) {
                    is DeviceState.Connecting -> OctoState.Loading
                    is DeviceState.Error ->
                        OctoState.Error(
                            device = deviceState.device,
                            errorType = deviceState.errorType,
                        )
                    is DeviceState.Octo if current is OctoState.Ready ->
                        current.copy(
                            deviceState = deviceState,
                            fans = deviceState.telemetry.fans.toPwmFanItems(deviceState.config.fans, storedData),
                            sensors = deviceState.telemetry.sensors.toSensorItems(deviceState.config.sensorOffsets, storedData),
                        )
                    is DeviceState.Octo ->
                        OctoState.Ready(
                            deviceState = deviceState,
                            fans = deviceState.telemetry.fans.toPwmFanItems(deviceState.config.fans, storedData),
                            selectedFanChannel = 0,
                            sensors = deviceState.telemetry.sensors.toSensorItems(deviceState.config.sensorOffsets, storedData),
                            selectedSensorChannel = 0,
                        )
                    else -> current
                }
            }
        }.launchIn(viewModelScope)
    }

    override fun onEvent(event: OctoEvent) {
        when (event) {
            is OctoEvent.SelectFan ->
                state.update { current ->
                    if (current is OctoState.Ready) current.copy(selectedFanChannel = event.channel) else current
                }
            is OctoEvent.SelectSensor ->
                state.update { current ->
                    if (current is OctoState.Ready) current.copy(selectedSensorChannel = event.channel) else current
                }
            is OctoEvent.RenameFan ->
                appDataRepository.setFanName(deviceId = deviceId, channel = event.channel, name = event.name)
            is OctoEvent.RenameSensor ->
                appDataRepository.setSensorName(deviceId = deviceId, channel = event.channel, name = event.name)
        }
    }
}
