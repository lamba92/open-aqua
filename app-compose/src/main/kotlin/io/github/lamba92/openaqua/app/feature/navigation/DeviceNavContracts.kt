package io.github.lamba92.openaqua.app.feature.navigation

import io.github.lamba92.openaqua.app.feature.device.common.CommonDeviceTab
import io.github.lamba92.openaqua.app.feature.settings.SettingsTab
import io.github.lamba92.openaqua.app.model.NavigationSelectionStoredData
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.model.device.DeviceId

data class DeviceNavState(
    val host: DeviceState.Host = DeviceState.Host.Loading,
    val devices: List<DeviceState.Aqua> = emptyList(),
    val devicesWithFeedErrors: Set<DeviceId> = emptySet(),
    val selectedItem: SelectedItem? = null,
) {
    sealed interface SelectedItem {
        data object Host : SelectedItem

        data class Device(
            val deviceId: DeviceId,
            val selectedTab: CommonDeviceTab = CommonDeviceTab.DEVICE_INFO,
        ) : SelectedItem

        data object BugReport : SelectedItem

        data object About : SelectedItem

        data class Settings(
            val selectedTab: SettingsTab = SettingsTab.GENERAL,
        ) : SelectedItem
    }
}

fun DeviceNavState.SelectedItem.toStoredData(): NavigationSelectionStoredData =
    when (this) {
        DeviceNavState.SelectedItem.Host -> NavigationSelectionStoredData.Host
        is DeviceNavState.SelectedItem.Device -> NavigationSelectionStoredData.Device(deviceId, selectedTab)
        DeviceNavState.SelectedItem.BugReport -> NavigationSelectionStoredData.BugReport
        DeviceNavState.SelectedItem.About -> NavigationSelectionStoredData.About
        is DeviceNavState.SelectedItem.Settings -> NavigationSelectionStoredData.Settings(selectedTab)
    }

fun NavigationSelectionStoredData.toSelectedItem(): DeviceNavState.SelectedItem =
    when (this) {
        NavigationSelectionStoredData.Host -> DeviceNavState.SelectedItem.Host
        is NavigationSelectionStoredData.Device -> DeviceNavState.SelectedItem.Device(deviceId, tab)
        NavigationSelectionStoredData.BugReport -> DeviceNavState.SelectedItem.BugReport
        NavigationSelectionStoredData.About -> DeviceNavState.SelectedItem.About
        is NavigationSelectionStoredData.Settings -> DeviceNavState.SelectedItem.Settings(tab)
    }

sealed interface DeviceNavEvent {
    data object SelectHost : DeviceNavEvent

    data class SelectDevice(
        val deviceId: DeviceId,
    ) : DeviceNavEvent

    data class SelectTab(
        val tab: CommonDeviceTab,
    ) : DeviceNavEvent

    data object SelectBugReport : DeviceNavEvent

    data object SelectAbout : DeviceNavEvent

    data object SelectSettings : DeviceNavEvent

    data class SelectSettingsTab(
        val tab: SettingsTab,
    ) : DeviceNavEvent
}

sealed interface DeviceNavEffect
