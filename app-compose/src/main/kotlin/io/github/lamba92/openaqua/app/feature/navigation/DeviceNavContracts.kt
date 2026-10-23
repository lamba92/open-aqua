package io.github.lamba92.openaqua.app.feature.navigation

import io.github.lamba92.openaqua.app.feature.device.common.CommonDeviceTab
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.model.DeviceId

sealed interface DeviceNavState {
    data object Loading : DeviceNavState

    data object NoDevices : DeviceNavState

    data class Ready(
        val devices: List<DeviceState>,
        val selectedDeviceId: DeviceId,
        val selectedTab: CommonDeviceTab = CommonDeviceTab.DEVICE_INFO,
    ) : DeviceNavState {
        val selectedDeviceState: DeviceState
            get() = devices.firstOrNull { it.device.id == selectedDeviceId } ?: devices.first()
    }
}

sealed interface DeviceNavEvent {
    data class SelectDevice(
        val deviceId: DeviceId,
    ) : DeviceNavEvent

    data class SelectTab(
        val tab: CommonDeviceTab,
    ) : DeviceNavEvent
}

sealed interface DeviceNavEffect
