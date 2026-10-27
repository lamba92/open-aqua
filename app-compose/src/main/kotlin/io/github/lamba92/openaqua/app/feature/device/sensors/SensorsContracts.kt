package io.github.lamba92.openaqua.app.feature.device.sensors

import io.github.lamba92.openaqua.app.feature.device.common.SensorItem

sealed interface SensorsUiState {
    data object Loading : SensorsUiState

    data class Ready(
        val sensors: List<SensorItem>,
        val selectedSensorChannel: Int,
        val renamingChannel: Int? = null,
        val hideDisconnectedSensors: Boolean = false,
        val hideUnconfiguredVirtualSensors: Boolean = false,
    ) : SensorsUiState {
        val renamingSensor: SensorItem?
            get() = renamingChannel?.let { ch -> sensors.firstOrNull { it.channel == ch } }
    }
}

sealed interface SensorsUiEvent {
    data class SelectSensor(
        val channel: Int,
    ) : SensorsUiEvent

    data class RenameSensor(
        val channel: Int,
        val name: String?,
    ) : SensorsUiEvent

    data class StartRenamingSensor(
        val channel: Int,
    ) : SensorsUiEvent

    data object DismissRenameDialog : SensorsUiEvent

    data object ToggleShowAllSensors : SensorsUiEvent
}

sealed interface SensorsUiEffect
