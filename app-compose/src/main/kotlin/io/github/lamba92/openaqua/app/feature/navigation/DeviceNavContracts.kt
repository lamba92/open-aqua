package io.github.lamba92.openaqua.app.feature.navigation

import io.github.lamba92.openaqua.model.DetectedHardwareDevice

enum class DeviceNavComponent {
    DEVICE_INFO,
    FANS,
    SENSORS,
}

sealed interface DeviceNavState {
    data object Loading : DeviceNavState

    data class Error(
        val message: String,
    ) : DeviceNavState

    data class Ready(
        val devices: List<DetectedHardwareDevice>,
        val selectedDevice: DetectedHardwareDevice,
        val selectedComponent: DeviceNavComponent = DeviceNavComponent.DEVICE_INFO,
        val isRefreshing: Boolean = false,
    ) : DeviceNavState
}

sealed interface DeviceNavEvent {
    data object LoadDevices : DeviceNavEvent

    data object RefreshDevices : DeviceNavEvent

    data class SelectDevice(
        val device: DetectedHardwareDevice,
    ) : DeviceNavEvent

    data class SelectComponent(
        val component: DeviceNavComponent,
    ) : DeviceNavEvent
}

sealed interface DeviceNavEffect {
    data class DeviceChanged(
        val device: DetectedHardwareDevice,
    ) : DeviceNavEffect

    data class ComponentChanged(
        val component: DeviceNavComponent,
    ) : DeviceNavEffect

    data class ErrorNotification(
        val message: String,
    ) : DeviceNavEffect
}
