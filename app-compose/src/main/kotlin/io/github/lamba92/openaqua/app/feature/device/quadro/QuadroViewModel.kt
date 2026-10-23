package io.github.lamba92.openaqua.app.feature.device.quadro

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.feature.device.common.CommonDeviceTab
import io.github.lamba92.openaqua.app.feature.device.common.toPwmFanItems
import io.github.lamba92.openaqua.app.feature.device.common.toSensorItems
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.repository.DeviceDataCacheService
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.app.repository.OpenAquaEngineApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class QuadroViewModel(
    @Suppress("unused") private val repository: OpenAquaEngineApi,
    cacheService: DeviceDataCacheService,
    scopeProvider: CoroutineScopeProvider,
    deviceId: String,
) : MVIViewModel<QuadroState, QuadroEvent, QuadroEffect> {
    override val viewModelScope: CoroutineScope = scopeProvider.getScope()

    override val state: StateFlow<QuadroState>
        field = MutableStateFlow<QuadroState>(QuadroState.Loading)

    override val effects: SharedFlow<QuadroEffect>
        field = MutableSharedFlow<QuadroEffect>()

    init {
        cacheService
            .getDeviceStateFlow(deviceId)
            .onEach { deviceState ->
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
                                fans = deviceState.telemetry.fans.toPwmFanItems(deviceState.config.fans),
                                sensors = deviceState.telemetry.sensors.toSensorItems(deviceState.config.sensorOffsets),
                            )
                        is DeviceState.Quadro ->
                            QuadroState.Ready(
                                deviceState = deviceState,
                                selectedTab = CommonDeviceTab.DEVICE_INFO,
                                fans = deviceState.telemetry.fans.toPwmFanItems(deviceState.config.fans),
                                selectedFanChannel = 0,
                                sensors = deviceState.telemetry.sensors.toSensorItems(deviceState.config.sensorOffsets),
                                selectedSensorChannel = 0,
                            )
                        else -> current
                    }
                }
            }.launchIn(viewModelScope)
    }

    override fun onEvent(event: QuadroEvent) {
        state.update { current ->
            if (current !is QuadroState.Ready) return@update current
            when (event) {
                is QuadroEvent.SelectTab -> current.copy(selectedTab = event.tab)
                is QuadroEvent.SelectFan -> current.copy(selectedFanChannel = event.channel)
                is QuadroEvent.SelectSensor -> current.copy(selectedSensorChannel = event.channel)
            }
        }
    }
}
