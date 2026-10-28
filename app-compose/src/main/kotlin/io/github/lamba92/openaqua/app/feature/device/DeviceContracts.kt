package io.github.lamba92.openaqua.app.feature.device

import io.github.lamba92.openaqua.app.repository.DeviceErrorType
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.model.device.DetectedHardwareDevice

sealed interface DeviceUiState {
    data object Loading : DeviceUiState

    data class Error(
        val device: DetectedHardwareDevice,
        val errorType: DeviceErrorType,
    ) : DeviceUiState

    data class Ready(
        val deviceState: DeviceState.Ready,
    ) : DeviceUiState
}

sealed interface DeviceUiEvent

sealed interface DeviceUiEffect
