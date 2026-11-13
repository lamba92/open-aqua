package io.github.lamba92.openaqua.app.mock

import io.github.lamba92.openaqua.app.repository.OpenAquaDeviceEngineApi
import io.github.lamba92.openaqua.model.FeatureState
import io.github.lamba92.openaqua.model.device.AutoCurveFanConfig
import io.github.lamba92.openaqua.model.device.CoolingChannelKind
import io.github.lamba92.openaqua.model.device.CoolingChannelTelemetry
import io.github.lamba92.openaqua.model.device.CurveFanConfig
import io.github.lamba92.openaqua.model.device.DescriptorsResponse
import io.github.lamba92.openaqua.model.device.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.device.DeviceAlias
import io.github.lamba92.openaqua.model.device.DeviceCapability
import io.github.lamba92.openaqua.model.device.DeviceConfig
import io.github.lamba92.openaqua.model.device.DeviceDescriptor
import io.github.lamba92.openaqua.model.device.DeviceId
import io.github.lamba92.openaqua.model.device.DeviceRegistry
import io.github.lamba92.openaqua.model.device.DeviceTelemetry
import io.github.lamba92.openaqua.model.device.DeviceType
import io.github.lamba92.openaqua.model.device.DevicesResponse
import io.github.lamba92.openaqua.model.device.DisplayConfig
import io.github.lamba92.openaqua.model.device.DisplayTelemetry
import io.github.lamba92.openaqua.model.device.FanLimits
import io.github.lamba92.openaqua.model.device.FeedVirtualSensorsRequest
import io.github.lamba92.openaqua.model.device.FixedFanConfig
import io.github.lamba92.openaqua.model.device.FlashConfigRequest
import io.github.lamba92.openaqua.model.device.RgbEffectMode
import io.github.lamba92.openaqua.model.device.RgbpxPortConfig
import io.github.lamba92.openaqua.model.device.SensorChannelKind
import io.github.lamba92.openaqua.model.device.SensorOffset
import io.github.lamba92.openaqua.model.device.SensorTelemetry
import io.github.lamba92.openaqua.model.device.descriptor
import io.github.lamba92.openaqua.model.device.findCapability
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class MockOpenAquaDeviceEngineApi : OpenAquaDeviceEngineApi {
    private val standardDevices: List<DetectedHardwareDevice> =
        DeviceType.entries.mapIndexed { index, type ->
            val desc = DeviceRegistry[type]
            val typeName = type.name.lowercase().replace('_', '-')
            DetectedHardwareDevice(
                id = DeviceId.from("mock_${type.name.lowercase()}_0"),
                alias = DeviceAlias.from("$typeName-1"),
                type = type,
                name = desc.name,
                serial = "0000-0000-${(index + 1).toString().padStart(4, '0')}",
                firmwareVersion = 1020,
            )
        }

    private val brokenDevice =
        DetectedHardwareDevice(
            id = DeviceId.from("mock_broken_0"),
            alias = DeviceAlias.from("broken-1"),
            type = DeviceType.OCTO,
            name = "Aqua Computer OCTO (Broken)",
            serial = "BROKEN-0001",
            firmwareVersion = 1020,
        )

    private val devices: List<DetectedHardwareDevice> = standardDevices + brokenDevice

    private val configs =
        ConcurrentHashMap<DeviceId, DeviceConfig>().apply {
            standardDevices.forEach { dev ->
                val desc = DeviceRegistry[dev.type]
                put(dev.id, desc.createDefaultConfig())
            }
            put(brokenDevice.id, DeviceRegistry[brokenDevice.type].createDefaultConfig())
        }

    private val virtualSensorValues = ConcurrentHashMap<DeviceId, Map<Int, Double>>()

    override suspend fun listDevices(): DevicesResponse = DevicesResponse(devices)

    override suspend fun listDescriptors(): DescriptorsResponse = DescriptorsResponse(DeviceRegistry.ALL)

    override suspend fun getDeviceConfig(deviceId: DeviceId): DeviceConfig {
        if (deviceId.value.contains("broken", ignoreCase = true)) {
            throw IOException("Cannot open /dev/hidraw for reading")
        }
        return configs[deviceId] ?: DeviceRegistry.OCTO.createDefaultConfig()
    }

    override suspend fun flashConfig(
        deviceId: DeviceId,
        config: FlashConfigRequest,
    ): DeviceConfig {
        if (deviceId.value.contains("broken", ignoreCase = true)) {
            throw IOException("Cannot write to /dev/hidraw")
        }
        val current = getDeviceConfig(deviceId)
        val reqFans = config.fans
        val updatedFans =
            if (reqFans != null) {
                current.fans.map { fan -> reqFans[fan.channel] ?: fan }
            } else {
                current.fans
            }
        val reqOffsets = config.sensorOffsets
        val updatedOffsets =
            if (reqOffsets != null) {
                current.sensorOffsets.map { offset ->
                    val newOffset = reqOffsets[offset.channel]
                    if (newOffset != null) offset.copy(offsetCelsius = newOffset) else offset
                }
            } else {
                current.sensorOffsets
            }
        val reqRgb = config.rgbpx
        val updatedRgb =
            if (reqRgb != null) {
                current.rgbpx.map { rgb -> reqRgb[rgb.port] ?: rgb }
            } else {
                current.rgbpx
            }
        val newPulses = config.flowPulsesPerLiter
        val updatedPulses =
            if (newPulses != null) {
                FeatureState.Active(newPulses)
            } else {
                current.flowPulsesPerLiter
            }
        val newDisplay = config.display
        val updatedDisplay =
            if (newDisplay != null) {
                FeatureState.Active(newDisplay)
            } else {
                current.display
            }
        val updated =
            current.copy(
                fans = updatedFans,
                sensorOffsets = updatedOffsets,
                flowPulsesPerLiter = updatedPulses,
                display = updatedDisplay,
                rgbpx = updatedRgb,
            )
        configs[deviceId] = updated
        return updated
    }

    override suspend fun feedVirtualSensors(
        deviceId: DeviceId,
        source: Flow<FeedVirtualSensorsRequest>,
    ) {
        if (deviceId.value.contains("broken", ignoreCase = true)) {
            throw IOException("Cannot feed virtual sensors to broken device")
        }
        source.collect { request ->
            virtualSensorValues[deviceId] = request.sensors
        }
    }

    override fun getTelemetryStream(deviceId: DeviceId): Flow<DeviceTelemetry> =
        flow {
            if (deviceId.value.contains("broken", ignoreCase = true)) {
                throw IOException("Cannot open /dev/hidraw for reading")
            }
            val device = devices.firstOrNull { it.id == deviceId } ?: devices.first()
            val desc = device.descriptor()

            while (currentCoroutineContext().isActive) {
                val currentConfig = getDeviceConfig(deviceId)

                val fansTelemetry =
                    desc.findCapability<DeviceCapability.Fans>()?.channels?.map { channelSpec ->
                        val fanConfig = currentConfig.fans.firstOrNull { it.channel == channelSpec.channel }
                        val pwm =
                            when (fanConfig) {
                                is FixedFanConfig -> fanConfig.powerPercent
                                is CurveFanConfig -> 55.0
                                is AutoCurveFanConfig -> 55.0
                                else -> 50.0
                            }
                        if (pwm > 0.0) {
                            val baseRpm = (pwm / 100.0 * 2000.0).toInt()
                            val jitter = Random.nextInt(-15, 16)
                            val rpm = (baseRpm + jitter).coerceAtLeast(0)
                            val voltage = 12.0
                            val current = 0.15 + (pwm / 100.0 * 0.1)
                            CoolingChannelTelemetry.Connected(
                                channel = channelSpec.channel,
                                kind = channelSpec.kind,
                                rpm = rpm,
                                powerWatts = voltage * current,
                                voltageVolts = voltage,
                                currentAmps = current,
                                pwmPercent = pwm,
                            )
                        } else {
                            CoolingChannelTelemetry.Disconnected(
                                channel = channelSpec.channel,
                                kind = channelSpec.kind,
                            )
                        }
                    } ?: emptyList()

                val physicalSensors =
                    desc.findCapability<DeviceCapability.TemperatureSensors>()?.sensors?.map { sensorSpec ->
                        val offset =
                            currentConfig.sensorOffsets
                                .firstOrNull { it.channel == sensorSpec.channel }
                                ?.offsetCelsius ?: 0.0
                        val jitter = Random.nextDouble(-0.3, 0.3)
                        val temp = 28.5 + (sensorSpec.channel * 2.5) + offset + jitter
                        SensorTelemetry.Connected(
                            channel = sensorSpec.channel,
                            kind = SensorChannelKind.PHYSICAL_NTC,
                            tempCelsius = kotlin.math.round(temp * 10.0) / 10.0,
                        )
                    } ?: emptyList()

                val virtualSensors =
                    desc.findCapability<DeviceCapability.VirtualSensors>()?.let { virtCap ->
                        val startCh = physicalSensors.size
                        val virtMap = virtualSensorValues[deviceId] ?: emptyMap()
                        (0 until virtCap.count).map { i ->
                            val ch = startCh + i
                            val value = virtMap[ch]
                            if (value != null) {
                                SensorTelemetry.Connected(
                                    channel = ch,
                                    kind = SensorChannelKind.VIRTUAL,
                                    tempCelsius = value,
                                )
                            } else {
                                SensorTelemetry.Disconnected(
                                    channel = ch,
                                    kind = SensorChannelKind.VIRTUAL,
                                )
                            }
                        }
                    } ?: emptyList()

                val flowRate =
                    if (desc.findCapability<DeviceCapability.FlowSensor>() != null) {
                        FeatureState.Active(1750 + Random.nextInt(-20, 21))
                    } else {
                        FeatureState.Unsupported
                    }

                val display =
                    if (desc.findCapability<DeviceCapability.Display>() != null) {
                        FeatureState.Active(DisplayTelemetry(brightnessPercent = 80))
                    } else {
                        FeatureState.Unsupported
                    }

                emit(
                    DeviceTelemetry(
                        fans = fansTelemetry,
                        sensors = physicalSensors + virtualSensors,
                        flowRateDlH = flowRate,
                        display = display,
                    ),
                )
                delay(1000.milliseconds)
            }
        }

    companion object {
        fun DeviceDescriptor.createDefaultConfig(): DeviceConfig {
            val fanCap = findCapability<DeviceCapability.Fans>()
            val tempCap = findCapability<DeviceCapability.TemperatureSensors>()
            val flowCap = findCapability<DeviceCapability.FlowSensor>()
            val dispCap = findCapability<DeviceCapability.Display>()
            val rgbCap = findCapability<DeviceCapability.Lighting.Rgbpx>()

            val fans =
                fanCap?.channels?.map { ch ->
                    FixedFanConfig(
                        channel = ch.channel,
                        powerPercent = if (ch.channel == 0) 60.0 else 40.0,
                        limits = FanLimits.Standard,
                    )
                } ?: emptyList()

            val sensorOffsets =
                tempCap?.sensors?.map { s ->
                    SensorOffset(channel = s.channel, offsetCelsius = 0.0)
                } ?: emptyList()

            val flowPulses =
                when (flowCap) {
                    null -> FeatureState.Unsupported
                    is DeviceCapability.FlowSensor.Calibrated -> FeatureState.Active(169)
                    is DeviceCapability.FlowSensor.Direct -> FeatureState.Active(169)
                }

            val display =
                if (dispCap != null) {
                    FeatureState.Active(DisplayConfig(brightnessPercent = 80, orientationDegrees = 0))
                } else {
                    FeatureState.Unsupported
                }

            val rgbpx =
                if (rgbCap != null) {
                    (0 until rgbCap.portCount).map { port ->
                        RgbpxPortConfig(
                            port = port,
                            enabled = true,
                            ledCount = 30,
                            brightnessPercent = 100,
                            mode = RgbEffectMode.STATIC,
                        )
                    }
                } else {
                    emptyList()
                }

            return DeviceConfig(
                fans = fans,
                sensorOffsets = sensorOffsets,
                flowPulsesPerLiter = flowPulses,
                display = display,
                rgbpx = rgbpx,
            )
        }
    }
}
