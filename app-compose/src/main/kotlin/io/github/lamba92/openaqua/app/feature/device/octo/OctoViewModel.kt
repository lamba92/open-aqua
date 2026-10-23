package io.github.lamba92.openaqua.app.feature.device.octo

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

class OctoViewModel(
    @Suppress("unused") private val repository: OpenAquaEngineApi,
    cacheService: DeviceDataCacheService,
    scopeProvider: CoroutineScopeProvider,
    deviceId: String,
) : MVIViewModel<OctoState, OctoEvent, OctoEffect> {
    override val viewModelScope: CoroutineScope = scopeProvider.getScope()

    override val state: StateFlow<OctoState>
        field = MutableStateFlow<OctoState>(OctoState.Loading)

    override val effects: SharedFlow<OctoEffect>
        field = MutableSharedFlow<OctoEffect>()

    init {
        cacheService
            .getDeviceStateFlow(deviceId)
            .onEach { deviceState ->
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
                                fans = deviceState.telemetry.fans.toPwmFanItems(deviceState.config.fans),
                                sensors = deviceState.telemetry.sensors.toSensorItems(deviceState.config.sensorOffsets),
                            )
                        is DeviceState.Octo ->
                            OctoState.Ready(
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

    override fun onEvent(event: OctoEvent) {
        state.update { current ->
            if (current !is OctoState.Ready) return@update current
            when (event) {
                is OctoEvent.SelectTab -> current.copy(selectedTab = event.tab)
                is OctoEvent.SelectFan -> current.copy(selectedFanChannel = event.channel)
                is OctoEvent.SelectSensor -> current.copy(selectedSensorChannel = event.channel)
            }
        }
    }
}
