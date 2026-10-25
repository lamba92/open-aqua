package io.github.lamba92.openaqua.app.feature.device.d5next

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.feature.device.common.FanItem
import io.github.lamba92.openaqua.app.feature.device.common.SensorChannelKind
import io.github.lamba92.openaqua.app.feature.device.common.SensorItem
import io.github.lamba92.openaqua.app.feature.device.common.toSensorItems
import io.github.lamba92.openaqua.app.model.DeviceStoredData
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import io.github.lamba92.openaqua.app.repository.DeviceDataCacheService
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.app.repository.OpenAquaEngineApi
import io.github.lamba92.openaqua.model.DeviceId
import io.github.lamba92.openaqua.model.SensorTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update

class D5NextViewModel(
    @Suppress("unused") private val repository: OpenAquaEngineApi,
    cacheService: DeviceDataCacheService,
    private val appDataRepository: AppDataRepository,
    scopeProvider: CoroutineScopeProvider,
    private val deviceId: DeviceId,
) : MVIViewModel<D5NextState, D5NextEvent, D5NextEffect> {
    override val viewModelScope: CoroutineScope = scopeProvider.getScope()

    override val state: StateFlow<D5NextState>
        field = MutableStateFlow<D5NextState>(D5NextState.Loading)

    override val effects: SharedFlow<D5NextEffect>
        field = MutableSharedFlow<D5NextEffect>()

    init {
        combine(
            cacheService.getDeviceStateFlow(deviceId),
            appDataRepository.getDeviceDataFlow(deviceId),
        ) { deviceState, storedData ->
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
                            pump = deviceState.toPumpItem(storedData),
                            fan = deviceState.toFanItem(storedData),
                            sensors = deviceState.toSensorItems(storedData),
                        )
                    is DeviceState.D5Next ->
                        D5NextState.Ready(
                            deviceState = deviceState,
                            pump = deviceState.toPumpItem(storedData),
                            fan = deviceState.toFanItem(storedData),
                            selectedCoolingTarget = D5NextCoolingTarget.PUMP,
                            sensors = deviceState.toSensorItems(storedData),
                            selectedSensorChannel = 0,
                        )
                    else -> current
                }
            }
        }.launchIn(viewModelScope)
    }

    override fun onEvent(event: D5NextEvent) {
        when (event) {
            is D5NextEvent.SelectCoolingTarget ->
                state.update { current ->
                    if (current is D5NextState.Ready) current.copy(selectedCoolingTarget = event.target) else current
                }
            is D5NextEvent.SelectSensor ->
                state.update { current ->
                    if (current is D5NextState.Ready) current.copy(selectedSensorChannel = event.channel) else current
                }
            is D5NextEvent.RenameFan ->
                appDataRepository.setFanName(deviceId = deviceId, channel = event.channel, name = event.name)
            is D5NextEvent.RenameSensor ->
                appDataRepository.setSensorName(deviceId = deviceId, channel = event.channel, name = event.name)
        }
    }
}

private fun DeviceState.D5Next.toPumpItem(storedData: DeviceStoredData) =
    FanItem(
        channel = telemetry.pump.channel,
        customName = storedData.fans[telemetry.pump.channel]?.customName,
        telemetry = telemetry.pump,
        config = config.pump,
    )

private fun DeviceState.D5Next.toFanItem(storedData: DeviceStoredData) =
    FanItem(
        channel = telemetry.fan.channel,
        customName = storedData.fans[telemetry.fan.channel]?.customName,
        telemetry = telemetry.fan,
        config = config.fan,
    )

private fun DeviceState.D5Next.toSensorItems(storedData: DeviceStoredData) =
    listOf(
        SensorItem(
            channel = 0,
            customName = storedData.sensors[0]?.customName,
            kind = SensorChannelKind.INTEGRATED_COOLANT,
            telemetry = SensorTelemetry(channel = 0, tempCelsius = telemetry.coolantTempCelsius, isVirtual = false),
            calibrationOffset = config.coolantOffset.offsetCelsius,
        ),
    ) + telemetry.virtualSensors.toSensorItems(emptyList(), storedData)
