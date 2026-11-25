package io.github.lamba92.openaqua.screenshots.mock

import io.github.lamba92.openaqua.app.feature.device.common.CommonDeviceTab
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavState
import io.github.lamba92.openaqua.app.feature.navigation.NavigationTargets
import io.github.lamba92.openaqua.app.repository.DeviceState
import io.github.lamba92.openaqua.model.device.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.device.DeviceAlias
import io.github.lamba92.openaqua.model.device.DeviceConfig
import io.github.lamba92.openaqua.model.device.DeviceId
import io.github.lamba92.openaqua.model.device.DeviceTelemetry
import io.github.lamba92.openaqua.model.device.DeviceType
import io.github.lamba92.openaqua.model.device.descriptor
import io.github.lamba92.openaqua.model.host.HostInfo
import io.github.lamba92.openaqua.model.host.HostTelemetry

val mockOctoId = DeviceId("octo-01")

val mockOctoHardwareDevice =
    DetectedHardwareDevice(
        id = mockOctoId,
        alias = DeviceAlias("Main Rig OCTO"),
        type = DeviceType.OCTO,
        name = "Aquacomputer OCTO",
        firmwareVersion = 1024,
    )

val mockAquaDeviceState =
    DeviceState.Aqua.Ready(
        device = mockOctoHardwareDevice,
        descriptor = mockOctoHardwareDevice.descriptor(),
        telemetry =
            DeviceTelemetry(
                fans = mockOctoFans.map { it.telemetry },
                sensors = mockOctoSensors.map { it.telemetry },
            ),
        config = DeviceConfig(fans = mockOctoFans.map { it.config }),
    )

val mockHostState =
    DeviceState.Host.Ready(
        info = HostInfo(devices = emptyList()),
        telemetry = HostTelemetry(sensors = emptyList()),
    )

fun createMockNavState(
    selectedItem: DeviceNavState.SelectedItem =
        DeviceNavState.SelectedItem.Device(
            deviceId = mockOctoId,
            selectedTab = CommonDeviceTab.FANS,
        ),
    isExpanded: Boolean = true,
): DeviceNavState =
    DeviceNavState(
        targets =
            NavigationTargets.Ready(
                host = mockHostState,
                devices = listOf(mockAquaDeviceState),
            ),
        selectedItem = selectedItem,
        isExpanded = isExpanded,
    )
