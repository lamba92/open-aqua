package io.github.lamba92.openaqua.engine.host.mock

import io.github.lamba92.openaqua.engine.host.hardware.HostHardwareController
import io.github.lamba92.openaqua.model.every
import io.github.lamba92.openaqua.model.host.HostDevice
import io.github.lamba92.openaqua.model.host.HostDeviceId
import io.github.lamba92.openaqua.model.host.HostDeviceType
import io.github.lamba92.openaqua.model.host.HostInfo
import io.github.lamba92.openaqua.model.host.HostSensorDescriptor
import io.github.lamba92.openaqua.model.host.HostSensorId
import io.github.lamba92.openaqua.model.host.HostSensorTelemetry
import io.github.lamba92.openaqua.model.host.HostTelemetry
import kotlinx.coroutines.flow.Flow
import kotlin.math.round
import kotlin.random.Random
import kotlin.time.Duration

class MockHostHardwareController : HostHardwareController {
    private val baseTemperatures =
        mapOf(
            "mock-cpu:package" to 45.0,
            "mock-cpu:core-avg" to 41.5,
            "mock-cpu:ccd1" to 43.0,
            "mock-cpu:ccd2" to 42.0,
            "mock-gpu:core" to 36.0,
            "mock-gpu:hotspot" to 44.0,
            "mock-gpu:memory" to 42.0,
            "mock-mb:chipset" to 48.0,
            "mock-mb:vrm" to 39.5,
            "mock-mb:ambient" to 31.0,
            "mock-nvme1:composite" to 40.0,
            "mock-nvme1:flash" to 46.0,
        )

    private val cachedInfo =
        HostInfo(
            devices =
                listOf(
                    HostDevice(
                        id = HostDeviceId("mock-cpu"),
                        name = "Mock AMD Ryzen 9 7950X",
                        type = HostDeviceType.CPU,
                        sensors =
                            listOf(
                                HostSensorDescriptor(HostSensorId("mock-cpu:package"), "CPU Package"),
                                HostSensorDescriptor(HostSensorId("mock-cpu:core-avg"), "CPU Core Average"),
                                HostSensorDescriptor(HostSensorId("mock-cpu:ccd1"), "CPU CCD1"),
                                HostSensorDescriptor(HostSensorId("mock-cpu:ccd2"), "CPU CCD2"),
                            ),
                    ),
                    HostDevice(
                        id = HostDeviceId("mock-gpu"),
                        name = "Mock NVIDIA GeForce RTX 4090",
                        type = HostDeviceType.GPU,
                        sensors =
                            listOf(
                                HostSensorDescriptor(HostSensorId("mock-gpu:core"), "GPU Core"),
                                HostSensorDescriptor(HostSensorId("mock-gpu:hotspot"), "GPU Hotspot"),
                                HostSensorDescriptor(HostSensorId("mock-gpu:memory"), "GPU Memory"),
                            ),
                    ),
                    HostDevice(
                        id = HostDeviceId("mock-mb"),
                        name = "Mock ASUS ROG Crosshair X670E",
                        type = HostDeviceType.MOTHERBOARD,
                        sensors =
                            listOf(
                                HostSensorDescriptor(HostSensorId("mock-mb:chipset"), "Chipset"),
                                HostSensorDescriptor(HostSensorId("mock-mb:vrm"), "VRM"),
                                HostSensorDescriptor(HostSensorId("mock-mb:ambient"), "Motherboard Ambient"),
                            ),
                    ),
                    HostDevice(
                        id = HostDeviceId("mock-nvme1"),
                        name = "Mock Samsung 990 PRO 2TB",
                        type = HostDeviceType.STORAGE,
                        sensors =
                            listOf(
                                HostSensorDescriptor(HostSensorId("mock-nvme1:composite"), "Composite"),
                                HostSensorDescriptor(HostSensorId("mock-nvme1:flash"), "Flash Memory"),
                            ),
                    ),
                ),
        )

    override fun getHostInfo(): HostInfo = cachedInfo

    override fun getTelemetryFlow(pollingInterval: Duration): Flow<HostTelemetry> =
        every(pollingInterval) {
            val telemetryList =
                cachedInfo.devices
                    .flatMap { it.sensors }
                    .map { sensor ->
                        val base = baseTemperatures[sensor.id.value] ?: 40.0
                        val jitter = Random.nextDouble(-0.7, 0.7)
                        val temp = round((base + jitter) * 10.0) / 10.0
                        HostSensorTelemetry.Connected(
                            id = sensor.id,
                            tempCelsius = temp,
                        )
                    }
            HostTelemetry(sensors = telemetryList)
        }
}
