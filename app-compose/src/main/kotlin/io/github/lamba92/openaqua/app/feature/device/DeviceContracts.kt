package io.github.lamba92.openaqua.app.feature.device

import io.github.lamba92.openaqua.app.feature.device.common.FanItem
import io.github.lamba92.openaqua.app.feature.device.common.SensorItem
import io.github.lamba92.openaqua.app.repository.DeviceErrorType
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.model.DetectedHardwareDevice

sealed interface DeviceUiState {
    data object Loading : DeviceUiState

    data class Error(
        val device: DetectedHardwareDevice,
        val errorType: DeviceErrorType,
    ) : DeviceUiState

    data class Ready(
        val deviceState: DeviceState.Ready,
        val fans: List<FanItem>,
        val selectedFanChannel: Int,
        val sensors: List<SensorItem>,
        val selectedSensorChannel: Int,
    ) : DeviceUiState
}

sealed interface DeviceUiEvent {
    data class SelectFan(
        val channel: Int,
    ) : DeviceUiEvent

    data class SelectSensor(
        val channel: Int,
    ) : DeviceUiEvent

    data class RenameFan(
        val channel: Int,
        val name: String?,
    ) : DeviceUiEvent

    data class RenameSensor(
        val channel: Int,
        val name: String?,
    ) : DeviceUiEvent
}

sealed interface DeviceUiEffect
