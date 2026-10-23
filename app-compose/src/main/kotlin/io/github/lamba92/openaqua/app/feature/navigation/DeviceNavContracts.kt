package io.github.lamba92.openaqua.app.feature.navigation

import io.github.lamba92.openaqua.app.repository.DeviceState

sealed interface DeviceNavState {
    data object Loading : DeviceNavState

    data object NoDevices : DeviceNavState

    data class Ready(
        val devices: List<DeviceState>,
        val selectedDeviceId: String,
    ) : DeviceNavState {
        val selectedDeviceState: DeviceState
            get() = devices.firstOrNull { it.device.id == selectedDeviceId } ?: devices.first()
    }
}

sealed interface DeviceNavEvent {
    data class SelectDevice(
        val deviceId: String,
    ) : DeviceNavEvent
}

sealed interface DeviceNavEffect
