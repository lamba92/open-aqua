package io.github.lamba92.openaqua.app.feature.device.quadro

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

class QuadroViewModel(
    @Suppress("unused") private val repository: OpenAquaEngineApi,
    cacheService: DeviceDataCacheService,
    private val appDataRepository: AppDataRepository,
    scopeProvider: CoroutineScopeProvider,
    private val deviceId: DeviceId,
) : MVIViewModel<QuadroState, QuadroEvent, QuadroEffect> {
    override val viewModelScope: CoroutineScope = scopeProvider.getScope()

    override val state: StateFlow<QuadroState>
        field = MutableStateFlow<QuadroState>(QuadroState.Loading)

    override val effects: SharedFlow<QuadroEffect>
        field = MutableSharedFlow<QuadroEffect>()

    init {
        combine(
            cacheService.getDeviceStateFlow(deviceId),
            appDataRepository.getDeviceDataFlow(deviceId),
        ) { deviceState, storedData ->
            state.update { current ->
                when (deviceState) {
                    is DeviceState.Connecting -> QuadroState.Loading
                    is DeviceState.Error ->
                        QuadroState.Error(
                            device = deviceState.device,
                            errorType = deviceState.errorType,
                        )
                    is DeviceState.Quadro if current is QuadroState.Ready ->
                        current.copy(
                            deviceState = deviceState,
                            fans = deviceState.telemetry.fans.toPwmFanItems(deviceState.config.fans, storedData),
                            sensors = deviceState.telemetry.sensors.toSensorItems(deviceState.config.sensorOffsets, storedData),
                        )
                    is DeviceState.Quadro ->
                        QuadroState.Ready(
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

    override fun onEvent(event: QuadroEvent) {
        when (event) {
            is QuadroEvent.SelectFan ->
                state.update { current ->
                    if (current is QuadroState.Ready) current.copy(selectedFanChannel = event.channel) else current
                }
            is QuadroEvent.SelectSensor ->
                state.update { current ->
                    if (current is QuadroState.Ready) current.copy(selectedSensorChannel = event.channel) else current
                }
            is QuadroEvent.RenameFan ->
                appDataRepository.setFanName(deviceId = deviceId, channel = event.channel, name = event.name)
            is QuadroEvent.RenameSensor ->
                appDataRepository.setSensorName(deviceId = deviceId, channel = event.channel, name = event.name)
        }
    }
}
