package io.github.lamba92.openaqua.app.feature.device.sensors

import io.github.lamba92.openaqua.app.feature.device.common.SensorItem

sealed interface SensorsUiState {
    data object Loading : SensorsUiState

    data class Ready(
        val sensors: List<SensorItem>,
        val selectedSensorChannel: Int,
    ) : SensorsUiState
}

sealed interface SensorsUiEvent {
    data class SelectSensor(
        val channel: Int,
    ) : SensorsUiEvent

    data class RenameSensor(
        val channel: Int,
        val name: String?,
    ) : SensorsUiEvent
}

sealed interface SensorsUiEffect
