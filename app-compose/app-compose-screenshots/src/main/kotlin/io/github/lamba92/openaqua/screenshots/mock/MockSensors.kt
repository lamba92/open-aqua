package io.github.lamba92.openaqua.screenshots.mock

import io.github.lamba92.openaqua.app.feature.device.common.ChannelName
import io.github.lamba92.openaqua.app.feature.device.common.SensorItem
import io.github.lamba92.openaqua.app.feature.device.sensors.AquaDeviceOption
import io.github.lamba92.openaqua.app.feature.device.sensors.DeviceSensorOption
import io.github.lamba92.openaqua.app.feature.device.sensors.HostSensorOption
import io.github.lamba92.openaqua.app.feature.device.sensors.SensorsUiState
import io.github.lamba92.openaqua.app.model.SensorTargetType
import io.github.lamba92.openaqua.app.model.VirtualSensorFeedSource
import io.github.lamba92.openaqua.model.device.SensorChannelKind
import io.github.lamba92.openaqua.model.device.SensorTelemetry
import io.github.lamba92.openaqua.model.device.tempCelsiusOrNull
import io.github.lamba92.openaqua.model.host.HostSensorId

val mockOctoSensors: List<SensorItem> =
    listOf(
        SensorItem(
            channel = 0,
            name = ChannelName.Custom("Coolant In"),
            telemetry =
                SensorTelemetry.Connected(
                    channel = 0,
                    kind = SensorChannelKind.PHYSICAL_NTC,
                    tempCelsius = 31.8,
                ),
        ),
        SensorItem(
            channel = 1,
            name = ChannelName.Custom("Coolant Out"),
            telemetry =
                SensorTelemetry.Connected(
                    channel = 1,
                    kind = SensorChannelKind.PHYSICAL_NTC,
                    tempCelsius = 29.5,
                ),
        ),
        SensorItem(
            channel = 2,
            name = ChannelName.Custom("Ambient Air"),
            telemetry =
                SensorTelemetry.Connected(
                    channel = 2,
                    kind = SensorChannelKind.PHYSICAL_NTC,
                    tempCelsius = 22.4,
                ),
        ),
        SensorItem(
            channel = 3,
            name = ChannelName.Custom("Radiator Exhaust"),
            telemetry =
                SensorTelemetry.Connected(
                    channel = 3,
                    kind = SensorChannelKind.PHYSICAL_NTC,
                    tempCelsius = 26.1,
                ),
        ),
    )

val mockOctoVirtualSensor1 =
    SensorItem(
        channel = 4,
        name = ChannelName.Custom("CPU Package"),
        telemetry =
            SensorTelemetry.Connected(
                channel = 4,
                kind = SensorChannelKind.VIRTUAL,
                tempCelsius = 54.2,
            ),
        virtualFeedSource = VirtualSensorFeedSource.Host(HostSensorId("cpu_package_temp")),
        feedValue = 54.2,
        targetType = SensorTargetType.DIE,
    )

val mockOctoVirtualSensor2 =
    SensorItem(
        channel = 5,
        name = ChannelName.Custom("GPU Core"),
        telemetry =
            SensorTelemetry.Connected(
                channel = 5,
                kind = SensorChannelKind.VIRTUAL,
                tempCelsius = 42.0,
            ),
        virtualFeedSource = VirtualSensorFeedSource.Host(HostSensorId("gpu_core_temp")),
        feedValue = 42.0,
        targetType = SensorTargetType.DIE,
    )

val mockOctoVirtualSensor3 =
    SensorItem(
        channel = 6,
        name = ChannelName.Default,
        telemetry =
            SensorTelemetry.Disconnected(
                channel = 6,
                kind = SensorChannelKind.VIRTUAL,
            ),
    )

val mockOctoVirtualSensors = listOf(mockOctoVirtualSensor1, mockOctoVirtualSensor2, mockOctoVirtualSensor3)

val mockHostSensors =
    listOf(
        HostSensorOption(
            id = HostSensorId("cpu_package_temp"),
            name = "AMD Ryzen 9 7950X - CPU Package",
            liveTemp = 54.2,
        ),
        HostSensorOption(
            id = HostSensorId("gpu_core_temp"),
            name = "NVIDIA GeForce RTX 4090 - GPU Temperature",
            liveTemp = 42.0,
        ),
        HostSensorOption(
            id = HostSensorId("motherboard_temp"),
            name = "ASUS ROG Crosshair X670E - Motherboard",
            liveTemp = 36.0,
        ),
    )

val mockOctoSensorsUiState =
    SensorsUiState.Ready(
        sensors = mockOctoSensors,
        selectedSensorChannel = 0,
        availableHostSensors = mockHostSensors,
        availableAquaDevices =
            listOf(
                AquaDeviceOption(
                    deviceId = mockOctoId,
                    name = "Aquacomputer OCTO",
                    sensors =
                        mockOctoSensors.map {
                            DeviceSensorOption(
                                channel = it.channel,
                                kind = it.telemetry.kind,
                                name = it.name,
                                liveTemp = it.telemetry.tempCelsiusOrNull,
                            )
                        },
                ),
            ),
    )

val mockOctoVirtualSensorFeedingUiState =
    SensorsUiState.Ready(
        sensors = mockOctoSensors + mockOctoVirtualSensors,
        selectedSensorChannel = 4,
        availableHostSensors = mockHostSensors,
        availableAquaDevices =
            listOf(
                AquaDeviceOption(
                    deviceId = mockOctoId,
                    name = "Aquacomputer OCTO",
                    sensors =
                        mockOctoSensors.map {
                            DeviceSensorOption(
                                channel = it.channel,
                                kind = it.telemetry.kind,
                                name = it.name,
                                liveTemp = it.telemetry.tempCelsiusOrNull,
                            )
                        },
                ),
            ),
    )
