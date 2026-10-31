package io.github.lamba92.openaqua.app.feature.device

import io.github.lamba92.openaqua.app.repository.DeviceErrorType
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.app.ui.components.FeederStatus
import io.github.lamba92.openaqua.model.device.DetectedHardwareDevice

sealed interface DeviceUiState {
    val status: FeederStatus

    data object Loading : DeviceUiState {
        override val status: FeederStatus
            get() = FeederStatus.NONE
    }

    data class Error(
        val device: DetectedHardwareDevice,
        val errorType: DeviceErrorType,
    ) : DeviceUiState {
        override val status: FeederStatus
            get() = FeederStatus.ERROR
    }

    data class Ready(
        val deviceState: DeviceState.Aqua,
        override val status: FeederStatus,
    ) : DeviceUiState
}

sealed interface DeviceUiEvent

sealed interface DeviceUiEffect
