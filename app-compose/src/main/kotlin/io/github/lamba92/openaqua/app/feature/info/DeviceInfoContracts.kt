package io.github.lamba92.openaqua.app.feature.info

import io.github.lamba92.openaqua.model.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.DeviceDescriptor

sealed interface DeviceInfoState {
    data object Idle : DeviceInfoState

    data class Loaded(
        val device: DetectedHardwareDevice,
        val descriptor: DeviceDescriptor,
    ) : DeviceInfoState

    data class Error(
        val message: String,
    ) : DeviceInfoState
}

sealed interface DeviceInfoEvent {
    data class SetDevice(
        val device: DetectedHardwareDevice,
    ) : DeviceInfoEvent

    data object RefreshInfo : DeviceInfoEvent
}

sealed interface DeviceInfoEffect {
    data class Notification(
        val message: String,
    ) : DeviceInfoEffect
}
