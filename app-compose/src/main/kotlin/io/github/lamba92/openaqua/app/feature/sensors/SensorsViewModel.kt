package io.github.lamba92.openaqua.app.feature.sensors

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.repository.OpenAquaEngineApi
import io.github.lamba92.openaqua.model.D5NextTelemetry
import io.github.lamba92.openaqua.model.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.DeviceRegistry
import io.github.lamba92.openaqua.model.DeviceTelemetry
import io.github.lamba92.openaqua.model.OctoTelemetry
import io.github.lamba92.openaqua.model.QuadroTelemetry
import io.github.lamba92.openaqua.model.SensorTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SensorsViewModel(
    private val repository: OpenAquaEngineApi,
    scopeProvider: CoroutineScopeProvider,
) : MVIViewModel<SensorsState, SensorsEvent, SensorsEffect> {
    override val viewModelScope: CoroutineScope = scopeProvider.getScope()

    override val state: StateFlow<SensorsState>
        field = MutableStateFlow<SensorsState>(SensorsState.Loading)

    override val effects: SharedFlow<SensorsEffect>
        field = MutableSharedFlow<SensorsEffect>(extraBufferCapacity = 64)

    private var telemetryJob: Job? = null

    override fun onEvent(event: SensorsEvent) {
        when (event) {
            is SensorsEvent.LoadSensors -> loadSensors(event.device)
            is SensorsEvent.SelectSensor -> selectSensor(event.channel)
            SensorsEvent.RefreshTelemetry -> refreshTelemetry()
        }
    }

    private fun loadSensors(device: DetectedHardwareDevice) {
        telemetryJob?.cancel()
        val descriptor = DeviceRegistry.findByType(device.type)

        val sensors =
            descriptor.tempSensors.map { sensorDesc ->
                val ch = sensorDesc.channel
                SensorItem(
                    channel = ch,
                    name = "Sensor ${ch + 1}",
                    pinLabel = "T${ch + 1}",
                    isVirtual = sensorDesc.isVirtual,
                    telemetry = null,
                )
            }

        val currentSelected = (state.value as? SensorsState.Ready)?.selectedChannel ?: 0
        val validSelected =
            if (sensors.any { it.channel == currentSelected }) currentSelected else (sensors.firstOrNull()?.channel ?: 0)

        state.update {
            SensorsState.Ready(
                device = device,
                sensors = sensors,
                selectedChannel = validSelected,
            )
        }

        telemetryJob =
            viewModelScope.launch {
                repository
                    .getTelemetryStream(device.id)
                    .collect { telemetry ->
                        val telemetrySensors = extractSensorTelemetry(telemetry)
                        state.update { current ->
                            if (current is SensorsState.Ready) {
                                val updatedSensors =
                                    current.sensors.map { sensorItem ->
                                        val itemTelemetry =
                                            telemetrySensors.firstOrNull { it.channel == sensorItem.channel }
                                                ?: sensorItem.telemetry
                                        sensorItem.copy(telemetry = itemTelemetry)
                                    }
                                current.copy(sensors = updatedSensors, isRefreshing = false)
                            } else {
                                current
                            }
                        }
                    }
            }
    }

    private fun selectSensor(channel: Int) {
        val current = state.value
        if (current is SensorsState.Ready) {
            state.update { current.copy(selectedChannel = channel) }
            viewModelScope.launch {
                effects.emit(SensorsEffect.SensorSelected(channel))
            }
        }
    }

    private fun refreshTelemetry() {
        val current = state.value
        if (current is SensorsState.Ready) {
            state.update { current.copy(isRefreshing = true) }
        }
    }

    private fun extractSensorTelemetry(telemetry: DeviceTelemetry?): List<SensorTelemetry> =
        when (telemetry) {
            is OctoTelemetry -> telemetry.sensors
            is QuadroTelemetry -> telemetry.sensors
            is D5NextTelemetry ->
                listOfNotNull(
                    telemetry.coolantTempCelsius?.let { SensorTelemetry(0, it, false) },
                ) + telemetry.virtualSensors
            null -> emptyList()
        }
}
