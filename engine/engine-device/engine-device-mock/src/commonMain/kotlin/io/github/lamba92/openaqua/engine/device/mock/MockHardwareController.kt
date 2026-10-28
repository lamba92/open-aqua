package io.github.lamba92.openaqua.engine.device.mock

import io.github.lamba92.openaqua.engine.device.hardware.HardwareController
import io.github.lamba92.openaqua.model.AutoCurveFanConfig
import io.github.lamba92.openaqua.model.CoolingChannelKind
import io.github.lamba92.openaqua.model.CoolingChannelTelemetry
import io.github.lamba92.openaqua.model.CurveFanConfig
import io.github.lamba92.openaqua.model.CurvePoint
import io.github.lamba92.openaqua.model.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.DeviceConfig
import io.github.lamba92.openaqua.model.DeviceDescriptor
import io.github.lamba92.openaqua.model.DeviceRegistry
import io.github.lamba92.openaqua.model.DeviceTelemetry
import io.github.lamba92.openaqua.model.DeviceType
import io.github.lamba92.openaqua.model.DisplayTelemetry
import io.github.lamba92.openaqua.model.FanChannelConfig
import io.github.lamba92.openaqua.model.FeatureState
import io.github.lamba92.openaqua.model.FixedFanConfig
import io.github.lamba92.openaqua.model.FlashConfigRequest
import io.github.lamba92.openaqua.model.RgbpxPortConfig
import io.github.lamba92.openaqua.model.SensorChannelKind
import io.github.lamba92.openaqua.model.SensorOffset
import io.github.lamba92.openaqua.model.SensorTelemetry
import io.github.lamba92.openaqua.model.TargetTempFanConfig
import io.github.lamba92.openaqua.model.TwoPointFanConfig
import io.github.lamba92.openaqua.model.UnconfiguredFanConfig
import io.github.lamba92.openaqua.model.tempCelsiusOrNull
import io.github.lamba92.openaqua.model.valueOrNull
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlin.math.round
import kotlin.time.Duration.Companion.seconds

class MockHardwareController : HardwareController {
    override fun scanDevices(): List<DetectedHardwareDevice> = loadMockStorage().devices

    override fun readConfig(device: DetectedHardwareDevice): DeviceConfig {
        val storage = loadMockStorage()
        return storage.configs[device.alias.value]
            ?: storage.configs[device.id.value]
            ?: error("No configuration found for device ${device.alias} (${device.id})")
    }

    override fun flashConfig(
        device: DetectedHardwareDevice,
        request: FlashConfigRequest,
    ): DeviceConfig {
        val storage = loadMockStorage()
        val current =
            storage.configs[device.alias.value]
                ?: storage.configs[device.id.value]
                ?: error("No configuration found for device ${device.alias} (${device.id})")

        val updatedConfig =
            DeviceConfig(
                fans = current.fans.applyFanUpdates(request.fans),
                sensorOffsets = current.sensorOffsets.applyOffsetUpdates(request.sensorOffsets),
                flowPulsesPerLiter = request.flowPulsesPerLiter?.let { FeatureState.Active(it) } ?: current.flowPulsesPerLiter,
                display = request.display?.let { FeatureState.Active(it) } ?: current.display,
                rgbpx = current.rgbpx.applyRgbpxUpdates(request.rgbpx),
            )

        val updatedMap = storage.configs.toMutableMap()
        val key = if (storage.configs.containsKey(device.alias.value)) device.alias.value else device.id.value
        updatedMap[key] = updatedConfig
        saveMockStorage(storage.copy(configs = updatedMap))

        return updatedConfig
    }

    private fun List<FanChannelConfig>.applyFanUpdates(updates: Map<Int, FanChannelConfig>?): List<FanChannelConfig> {
        val updatedFans = toMutableList()
        updates?.forEach { (channel, config) ->
            val idx = updatedFans.indexOfFirst { it.channel == channel }
            if (idx >= 0) updatedFans[idx] = config
        }
        return updatedFans
    }

    private fun List<SensorOffset>.applyOffsetUpdates(updates: Map<Int, Double>?): List<SensorOffset> {
        val updatedOffsets = toMutableList()
        updates?.forEach { (channel, offset) ->
            val idx = updatedOffsets.indexOfFirst { it.channel == channel }
            if (idx >= 0) updatedOffsets[idx] = SensorOffset(channel, offset)
        }
        return updatedOffsets
    }

    private fun List<RgbpxPortConfig>.applyRgbpxUpdates(updates: Map<Int, RgbpxPortConfig>?): List<RgbpxPortConfig> {
        val updatedRgbpx = associateBy { it.port }.toMutableMap()
        updates?.forEach { (port, config) ->
            updatedRgbpx[port] = config
        }
        return updatedRgbpx.values.toList()
    }

    override fun feedVirtualSensors(
        device: DetectedHardwareDevice,
        sensors: Map<Int, Double>,
    ) {
        val storage = loadMockVirtualSensorsStorage().toMutableMap()
        val key = if (storage.containsKey(device.alias.value)) device.alias.value else device.id.value
        val deviceSensors = (storage[key] ?: emptyMap()).toMutableMap()

        val allowedRange =
            when (device.type) {
                DeviceType.D5_NEXT -> 1..8
                else -> 4..19
            }

        sensors.forEach { (channel, temp) ->
            if (channel in allowedRange) {
                deviceSensors[channel] = temp
            }
        }
        storage[key] = deviceSensors
        saveMockVirtualSensorsStorage(storage)
    }

    override fun streamTelemetry(device: DetectedHardwareDevice): Flow<DeviceTelemetry> =
        flow {
            val descriptor = DeviceRegistry.findByType(device.type)
            while (currentCoroutineContext().isActive) {
                val config = readConfig(device)
                val allSensorsStorage = loadMockVirtualSensorsStorage()
                val virtualSensorsMap =
                    allSensorsStorage[device.alias.value] ?: allSensorsStorage[device.id.value] ?: emptyMap()

                emit(generateTelemetry(descriptor, config, virtualSensorsMap))
                delay(1.seconds)
            }
        }

    private fun generateTelemetry(
        descriptor: DeviceDescriptor,
        config: DeviceConfig,
        virtualSensorsMap: Map<Int, Double?>,
    ): DeviceTelemetry {
        val physicalSensors =
            descriptor.tempSensors.map { sensor ->
                val baseTemp =
                    when (sensor.channel) {
                        0 -> if (sensor.kind == SensorChannelKind.INTEGRATED_COOLANT) 28.5 else 32.5
                        1 -> 22.8
                        2 -> 27.4
                        3 -> 43.1
                        else -> 30.0
                    }
                val offset = config.sensorOffsets.firstOrNull { it.channel == sensor.channel }?.offsetCelsius ?: 0.0
                val totalTemp = round((baseTemp + offset) * 10.0) / 10.0
                SensorTelemetry.Connected(
                    channel = sensor.channel,
                    tempCelsius = totalTemp,
                    kind = sensor.kind,
                )
            }

        val virtualSensors =
            (0 until descriptor.virtualSensorsCount).map { i ->
                val ch = descriptor.tempSensors.size + i
                val temp = virtualSensorsMap[ch]
                if (temp != null) {
                    SensorTelemetry.Connected(
                        channel = ch,
                        tempCelsius = temp,
                        kind = SensorChannelKind.VIRTUAL,
                    )
                } else {
                    SensorTelemetry.Disconnected(
                        channel = ch,
                        kind = SensorChannelKind.VIRTUAL,
                    )
                }
            }

        val allSensors = (physicalSensors + virtualSensors).associate { it.channel to it.tempCelsiusOrNull }

        val fans =
            descriptor.fanChannels.map { fan ->
                val fanConfig = config.fans.firstOrNull { it.channel == fan.channel }
                val (maxRpm, maxAmps) =
                    when (fan.kind) {
                        CoolingChannelKind.PUMP -> 4800 to 1.8
                        CoolingChannelKind.FAN -> if (fan.channel == 0) 2500 to 0.70 else 1800 to 0.30
                    }
                simulateFan(
                    channel = fan.channel,
                    config = fanConfig,
                    allSensors = allSensors,
                    maxRpm = maxRpm,
                    maxAmps = maxAmps,
                    kind = fan.kind,
                )
            }

        val flowRateDlH =
            if (descriptor.hasFlowSensor && descriptor.flowSensorOffset != null) {
                val activeFans = fans.filterIsInstance<CoolingChannelTelemetry.Connected>()
                if (activeFans.any { it.rpm > 0 }) {
                    val pwm = activeFans.firstOrNull()?.pwmPercent ?: 50.0
                    FeatureState.Active(round(60.0 + (pwm / 100.0) * 140.0).toInt())
                } else {
                    FeatureState.Disconnected
                }
            } else {
                FeatureState.Unsupported
            }

        val display =
            if (descriptor.hasDisplay) {
                FeatureState.Active(
                    DisplayTelemetry(
                        brightnessPercent = config.display.valueOrNull?.brightnessPercent ?: 80,
                        orientationDegrees = config.display.valueOrNull?.orientationDegrees ?: 0,
                    ),
                )
            } else {
                FeatureState.Unsupported
            }

        return DeviceTelemetry(
            fans = fans,
            sensors = physicalSensors + virtualSensors,
            flowRateDlH = flowRateDlH,
            display = display,
        )
    }

    private fun simulateFan(
        channel: Int,
        config: FanChannelConfig?,
        allSensors: Map<Int, Double?>,
        maxRpm: Int,
        maxAmps: Double,
        kind: CoolingChannelKind = CoolingChannelKind.FAN,
    ): CoolingChannelTelemetry {
        val pwm = calculatePwm(config, allSensors)
        val finalPwm = round(pwm * 10.0) / 10.0

        return if (finalPwm == 0.0) {
            CoolingChannelTelemetry.Disconnected(
                channel = channel,
                kind = kind,
            )
        } else {
            val voltage = 12.0
            val rpm = round(maxRpm * (finalPwm / 100.0)).toInt()
            val current = round((maxAmps * (finalPwm / 100.0)) * 1000.0) / 1000.0
            val power = round(voltage * current * 100.0) / 100.0
            CoolingChannelTelemetry.Connected(
                channel = channel,
                rpm = rpm,
                powerWatts = power,
                voltageVolts = voltage,
                currentAmps = current,
                pwmPercent = finalPwm,
                kind = kind,
            )
        }
    }

    private fun calculatePwm(
        config: FanChannelConfig?,
        sensorTemps: Map<Int, Double?>,
    ): Double =
        when (config) {
            is FixedFanConfig -> config.powerPercent
            is CurveFanConfig -> {
                val temp = sensorTemps[config.sensorIndex]
                if (temp != null) evaluateCurve(config.points, temp) else 100.0
            }

            is AutoCurveFanConfig -> {
                val temp = sensorTemps[config.sensorIndex]
                if (temp != null) evaluateCurve(config.toCurveFanConfig().points, temp) else 100.0
            }

            is TargetTempFanConfig -> {
                val temp = sensorTemps[config.sensorIndex]
                if (temp != null) {
                    val diff = temp - config.targetTempCelsius
                    (50.0 + diff * 10.0).coerceIn(0.0, 100.0)
                } else {
                    100.0
                }
            }

            is TwoPointFanConfig -> {
                val temp = sensorTemps[config.sensorIndex]
                if (temp != null) {
                    if (temp < 30.0) 30.0 else 100.0
                } else {
                    100.0
                }
            }

            is UnconfiguredFanConfig, null -> 0.0
        }.coerceIn(0.0, 100.0)

    private fun evaluateCurve(
        points: List<CurvePoint>,
        temp: Double,
    ): Double {
        if (points.isEmpty()) return 0.0
        val sorted = points.sortedBy { it.temp }
        if (temp <= sorted.first().temp) return sorted.first().pwm
        if (temp >= sorted.last().temp) return sorted.last().pwm

        for (i in 0 until sorted.size - 1) {
            val p1 = sorted[i]
            val p2 = sorted[i + 1]
            if (temp in p1.temp..p2.temp) {
                val spanT = p2.temp - p1.temp
                if (spanT <= 1e-6) return p1.pwm
                val factor = (temp - p1.temp) / spanT
                return p1.pwm + factor * (p2.pwm - p1.pwm)
            }
        }
        return sorted.last().pwm
    }
}
