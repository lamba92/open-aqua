package io.github.lamba92.openaqua.app.feature.device.d5next

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.feature.device.common.CommonDeviceTab
import io.github.lamba92.openaqua.app.feature.device.common.FanItem
import io.github.lamba92.openaqua.app.feature.device.common.SensorChannelKind
import io.github.lamba92.openaqua.app.feature.device.common.SensorItem
import io.github.lamba92.openaqua.app.feature.device.common.toSensorItems
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.repository.DeviceDataCacheService
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.app.repository.OpenAquaEngineApi
import io.github.lamba92.openaqua.model.SensorTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class D5NextViewModel(
    @Suppress("unused") private val repository: OpenAquaEngineApi,
    cacheService: DeviceDataCacheService,
    scopeProvider: CoroutineScopeProvider,
    deviceId: String,
) : MVIViewModel<D5NextState, D5NextEvent, D5NextEffect> {
    override val viewModelScope: CoroutineScope = scopeProvider.getScope()

    override val state: StateFlow<D5NextState>
        field = MutableStateFlow<D5NextState>(D5NextState.Loading)

    override val effects: SharedFlow<D5NextEffect>
        field = MutableSharedFlow<D5NextEffect>()

    init {
        cacheService
            .getDeviceStateFlow(deviceId)
            .onEach { deviceState ->
                state.update { current ->
                    when (deviceState) {
                        is DeviceState.Connecting -> D5NextState.Loading
                        is DeviceState.Error ->
                            D5NextState.Error(
                                device = deviceState.device,
                                errorType = deviceState.errorType,
                            )
                        is DeviceState.D5Next if current is D5NextState.Ready ->
                            current.copy(
                                deviceState = deviceState,
                                pump = deviceState.toPumpItem(),
                                fan = deviceState.toFanItem(),
                                sensors = deviceState.toSensorItems(),
                            )
                        is DeviceState.D5Next ->
                            D5NextState.Ready(
                                deviceState = deviceState,
                                selectedTab = CommonDeviceTab.DEVICE_INFO,
                                pump = deviceState.toPumpItem(),
                                fan = deviceState.toFanItem(),
                                selectedCoolingTarget = D5NextCoolingTarget.PUMP,
                                sensors = deviceState.toSensorItems(),
                                selectedSensorChannel = 0,
                            )
                        else -> current
                    }
                }
            }.launchIn(viewModelScope)
    }

    override fun onEvent(event: D5NextEvent) {
        state.update { current ->
            if (current !is D5NextState.Ready) return@update current
            when (event) {
                is D5NextEvent.SelectTab -> current.copy(selectedTab = event.tab)
                is D5NextEvent.SelectCoolingTarget -> current.copy(selectedCoolingTarget = event.target)
                is D5NextEvent.SelectSensor -> current.copy(selectedSensorChannel = event.channel)
            }
        }
    }
}

private fun DeviceState.D5Next.toPumpItem() =
    FanItem(
        channel = telemetry.pump.channel,
        telemetry = telemetry.pump,
        config = config.pump,
    )

private fun DeviceState.D5Next.toFanItem() =
    FanItem(
        channel = telemetry.fan.channel,
        telemetry = telemetry.fan,
        config = config.fan,
    )

private fun DeviceState.D5Next.toSensorItems() =
    listOf(
        SensorItem(
            channel = 0,
            kind = SensorChannelKind.INTEGRATED_COOLANT,
            telemetry = SensorTelemetry(channel = 0, tempCelsius = telemetry.coolantTempCelsius, isVirtual = false),
            calibrationOffset = config.coolantOffset.offsetCelsius,
        ),
    ) + telemetry.virtualSensors.toSensorItems(emptyList())
