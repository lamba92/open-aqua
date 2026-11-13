package io.github.lamba92.openaqua.app.mock

import io.github.lamba92.openaqua.app.repository.OpenAquaHostEngineApi
import io.github.lamba92.openaqua.model.host.HostDevice
import io.github.lamba92.openaqua.model.host.HostDeviceId
import io.github.lamba92.openaqua.model.host.HostDeviceType
import io.github.lamba92.openaqua.model.host.HostFanControlRequest
import io.github.lamba92.openaqua.model.host.HostFanDescriptor
import io.github.lamba92.openaqua.model.host.HostFanId
import io.github.lamba92.openaqua.model.host.HostFanTelemetry
import io.github.lamba92.openaqua.model.host.HostInfo
import io.github.lamba92.openaqua.model.host.HostSensorDescriptor
import io.github.lamba92.openaqua.model.host.HostSensorId
import io.github.lamba92.openaqua.model.host.HostSensorTelemetry
import io.github.lamba92.openaqua.model.host.HostTelemetry
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.round
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class MockOpenAquaHostEngineApi : OpenAquaHostEngineApi {
    private val baseTemperatures =
        mapOf(
            "mock-cpu:package" to 58.0,
            "mock-cpu:core-avg" to 52.0,
            "mock-cpu:ccd1" to 55.0,
            "mock-cpu:ccd2" to 51.0,
            "mock-gpu:edge" to 41.0,
            "mock-gpu:hotspot" to 44.0,
            "mock-gpu:memory" to 42.0,
            "mock-mb:chipset" to 48.0,
            "mock-mb:vrm" to 39.5,
            "mock-mb:ambient" to 31.0,
            "mock-nvme1:composite" to 40.0,
            "mock-nvme1:flash" to 46.0,
        )

    private val fanPwmLevels = ConcurrentHashMap<HostFanId, Double>()

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
                                HostSensorDescriptor(HostSensorId("mock-gpu:edge"), "GPU Edge"),
                                HostSensorDescriptor(HostSensorId("mock-gpu:hotspot"), "GPU Hotspot"),
                                HostSensorDescriptor(HostSensorId("mock-gpu:memory"), "GPU Memory"),
                            ),
                        fans =
                            listOf(
                                HostFanDescriptor.Controllable(HostFanId("mock-gpu:fan1"), "GPU Fan 1"),
                                HostFanDescriptor.Controllable(HostFanId("mock-gpu:fan2"), "GPU Fan 2"),
                            ),
                    ),
                    HostDevice(
                        id = HostDeviceId("mock-mb"),
                        name = "Mock ASUS ROG CROSSHAIR X670E HERO",
                        type = HostDeviceType.MOTHERBOARD,
                        sensors =
                            listOf(
                                HostSensorDescriptor(HostSensorId("mock-mb:chipset"), "Motherboard Chipset"),
                                HostSensorDescriptor(HostSensorId("mock-mb:vrm"), "Motherboard VRM"),
                                HostSensorDescriptor(HostSensorId("mock-mb:ambient"), "Motherboard Ambient"),
                            ),
                        fans =
                            listOf(
                                HostFanDescriptor.Controllable(HostFanId("mock-mb:cpu-fan"), "CPU Fan Header"),
                                HostFanDescriptor.Controllable(HostFanId("mock-mb:cha-fan1"), "Chassis Fan 1"),
                                HostFanDescriptor.Controllable(HostFanId("mock-mb:cha-fan2"), "Chassis Fan 2"),
                                HostFanDescriptor.ReadOnly(HostFanId("mock-mb:pwr-fan"), "Power Fan Header"),
                            ),
                    ),
                    HostDevice(
                        id = HostDeviceId("mock-nvme1"),
                        name = "Mock Samsung SSD 990 PRO 2TB",
                        type = HostDeviceType.STORAGE,
                        sensors =
                            listOf(
                                HostSensorDescriptor(HostSensorId("mock-nvme1:composite"), "Composite"),
                                HostSensorDescriptor(HostSensorId("mock-nvme1:flash"), "Flash Memory"),
                            ),
                    ),
                ),
        )

    override suspend fun getHostInfo(): HostInfo = cachedInfo

    override fun getTelemetryStream(): Flow<HostTelemetry> =
        flow {
            while (currentCoroutineContext().isActive) {
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

                val fanTelemetryList =
                    cachedInfo.devices
                        .flatMap { it.fans }
                        .map { fan ->
                            when (fan) {
                                is HostFanDescriptor.Controllable -> {
                                    val pwm = fanPwmLevels[fan.id] ?: 50.0
                                    val baseRpm = (pwm / 100.0 * 1800.0).toInt()
                                    val jitter = Random.nextInt(-15, 16)
                                    HostFanTelemetry.Controllable(
                                        id = fan.id,
                                        rpm = (baseRpm + jitter).coerceAtLeast(0),
                                        pwmPercent = pwm,
                                        isFollowingBiosCurve = !fanPwmLevels.containsKey(fan.id),
                                    )
                                }

                                is HostFanDescriptor.ReadOnly -> {
                                    val jitter = Random.nextInt(-15, 16)
                                    HostFanTelemetry.ReadOnly(
                                        id = fan.id,
                                        rpm = (950 + jitter).coerceAtLeast(0),
                                    )
                                }
                            }
                        }

                emit(
                    HostTelemetry(
                        sensors = telemetryList,
                        fans = fanTelemetryList,
                    ),
                )
                delay(1000.milliseconds)
            }
        }

    override suspend fun streamFanControl(requests: Flow<HostFanControlRequest>) {
        requests.collect { request ->
            fanPwmLevels[request.channel] = request.pwmPercent
        }
    }

    override suspend fun restoreFanDefaults() {
        fanPwmLevels.clear()
    }
}
