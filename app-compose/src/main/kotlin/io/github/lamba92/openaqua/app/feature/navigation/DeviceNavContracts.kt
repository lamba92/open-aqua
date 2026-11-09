package io.github.lamba92.openaqua.app.feature.navigation

import io.github.lamba92.openaqua.app.feature.about.AboutTab
import io.github.lamba92.openaqua.app.feature.device.common.CommonDeviceTab
import io.github.lamba92.openaqua.app.feature.settings.SettingsTab
import io.github.lamba92.openaqua.app.model.NavigationSelectionStoredData
import io.github.lamba92.openaqua.app.permission.DevicePermissionState
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.model.device.DeviceId

sealed interface NavigationTargets {
    data object Loading : NavigationTargets

    @JvmInline
    value class PermissionError(
        val error: DevicePermissionState.Denied,
    ) : NavigationTargets

    data class Ready(
        val host: DeviceState.Host = DeviceState.Host.Loading,
        val devices: List<DeviceState.Aqua> = emptyList(),
    ) : NavigationTargets
}

data class DeviceNavState(
    val targets: NavigationTargets = NavigationTargets.Loading,
    val devicesWithFeedErrors: Set<DeviceId> = emptySet(),
    val selectedItem: SelectedItem? = null,
    val isExpanded: Boolean = false,
) {
    sealed interface SelectedItem {
        data object Host : SelectedItem

        data class Device(
            val deviceId: DeviceId,
            val selectedTab: CommonDeviceTab = CommonDeviceTab.DEVICE_INFO,
        ) : SelectedItem

        data object PermissionError : SelectedItem

        data object BugReport : SelectedItem

        data class About(
            val selectedTab: AboutTab = AboutTab.OPEN_AQUA,
        ) : SelectedItem

        data class Settings(
            val selectedTab: SettingsTab = SettingsTab.GENERAL,
        ) : SelectedItem
    }
}

fun DeviceNavState.SelectedItem.toStoredData(): NavigationSelectionStoredData? =
    when (this) {
        DeviceNavState.SelectedItem.Host -> NavigationSelectionStoredData.Host
        is DeviceNavState.SelectedItem.Device -> NavigationSelectionStoredData.Device(deviceId, selectedTab)
        DeviceNavState.SelectedItem.BugReport -> NavigationSelectionStoredData.BugReport
        is DeviceNavState.SelectedItem.About -> NavigationSelectionStoredData.About(selectedTab)
        is DeviceNavState.SelectedItem.Settings -> NavigationSelectionStoredData.Settings(selectedTab)
        DeviceNavState.SelectedItem.PermissionError -> null
    }

fun NavigationSelectionStoredData.toSelectedItem(): DeviceNavState.SelectedItem =
    when (this) {
        NavigationSelectionStoredData.Host -> DeviceNavState.SelectedItem.Host
        is NavigationSelectionStoredData.Device -> DeviceNavState.SelectedItem.Device(deviceId, tab)
        NavigationSelectionStoredData.BugReport -> DeviceNavState.SelectedItem.BugReport
        is NavigationSelectionStoredData.About -> DeviceNavState.SelectedItem.About(tab)
        is NavigationSelectionStoredData.Settings -> DeviceNavState.SelectedItem.Settings(tab)
    }

sealed interface DeviceNavEvent {
    data object SelectHost : DeviceNavEvent

    @JvmInline
    value class SelectDevice(
        val deviceId: DeviceId,
    ) : DeviceNavEvent

    @JvmInline
    value class SelectTab(
        val tab: CommonDeviceTab,
    ) : DeviceNavEvent

    data object SelectPermissionError : DeviceNavEvent

    data object SelectBugReport : DeviceNavEvent

    data object SelectAbout : DeviceNavEvent

    @JvmInline
    value class SelectAboutTab(
        val tab: AboutTab,
    ) : DeviceNavEvent

    data object SelectSettings : DeviceNavEvent

    @JvmInline
    value class SelectSettingsTab(
        val tab: SettingsTab,
    ) : DeviceNavEvent

    data object ToggleNavigationRailExpanded : DeviceNavEvent
}

sealed interface DeviceNavEffect
