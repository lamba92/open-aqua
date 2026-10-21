package io.github.lamba92.openaqua.app.feature.sensors

import io.github.lamba92.openaqua.model.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.SensorTelemetry

data class SensorItem(
    val channel: Int,
    val name: String,
    val pinLabel: String,
    val isVirtual: Boolean,
    val telemetry: SensorTelemetry? = null,
    val calibrationOffset: Double = 0.0,
)

sealed interface SensorsState {
    data object Loading : SensorsState

    data class Ready(
        val device: DetectedHardwareDevice,
        val sensors: List<SensorItem>,
        val selectedChannel: Int,
        val isRefreshing: Boolean = false,
    ) : SensorsState {
        val selectedSensor: SensorItem?
            get() = sensors.firstOrNull { it.channel == selectedChannel } ?: sensors.firstOrNull()
    }

    data class Error(
        val message: String,
    ) : SensorsState
}

sealed interface SensorsEvent {
    data class LoadSensors(
        val device: DetectedHardwareDevice,
    ) : SensorsEvent

    data class SelectSensor(
        val channel: Int,
    ) : SensorsEvent

    data object RefreshTelemetry : SensorsEvent
}

sealed interface SensorsEffect {
    data class SensorSelected(
        val channel: Int,
    ) : SensorsEffect

    data class Notification(
        val message: String,
    ) : SensorsEffect
}
