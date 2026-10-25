package io.github.lamba92.openaqua.engine.mock

import io.github.lamba92.openaqua.engine.hardware.HardwareController
import io.github.lamba92.openaqua.model.AutoCurveFanConfig
import io.github.lamba92.openaqua.model.CurveFanConfig
import io.github.lamba92.openaqua.model.CurvePoint
import io.github.lamba92.openaqua.model.D5NextConfig
import io.github.lamba92.openaqua.model.D5NextTelemetry
import io.github.lamba92.openaqua.model.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.DeviceConfig
import io.github.lamba92.openaqua.model.DeviceTelemetry
import io.github.lamba92.openaqua.model.DeviceType
import io.github.lamba92.openaqua.model.FanChannelConfig
import io.github.lamba92.openaqua.model.FanTelemetry
import io.github.lamba92.openaqua.model.FixedFanConfig
import io.github.lamba92.openaqua.model.FlashConfigRequest
import io.github.lamba92.openaqua.model.FlashD5NextConfig
import io.github.lamba92.openaqua.model.FlashOctoConfig
import io.github.lamba92.openaqua.model.FlashQuadroConfig
import io.github.lamba92.openaqua.model.OctoConfig
import io.github.lamba92.openaqua.model.OctoTelemetry
import io.github.lamba92.openaqua.model.QuadroConfig
import io.github.lamba92.openaqua.model.QuadroTelemetry
import io.github.lamba92.openaqua.model.RgbpxPortConfig
import io.github.lamba92.openaqua.model.SensorOffset
import io.github.lamba92.openaqua.model.SensorTelemetry
import io.github.lamba92.openaqua.model.TargetTempFanConfig
import io.github.lamba92.openaqua.model.TwoPointFanConfig
import io.github.lamba92.openaqua.model.UnconfiguredFanConfig
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

        val updatedConfig: DeviceConfig =
            when (request) {
                is FlashOctoConfig -> {
                    check(current is OctoConfig) { "Expected OctoConfig for device ${device.alias}, got ${current::class.simpleName}" }
                    OctoConfig(
                        fans = current.fans.applyFanUpdates(request.fans),
                        sensorOffsets = current.sensorOffsets.applyOffsetUpdates(request.sensorOffsets),
                        rgbpx = current.rgbpx.applyRgbpxUpdates(request.rgbpx),
                    )
                }

                is FlashQuadroConfig -> {
                    check(current is QuadroConfig) { "Expected QuadroConfig for device ${device.alias}, got ${current::class.simpleName}" }
                    QuadroConfig(
                        fans = current.fans.applyFanUpdates(request.fans),
                        sensorOffsets = current.sensorOffsets.applyOffsetUpdates(request.sensorOffsets),
                        flowPulsesPerLiter = request.flowPulsesPerLiter ?: current.flowPulsesPerLiter,
                        rgbpx = current.rgbpx.applyRgbpxUpdates(request.rgbpx),
                    )
                }

                is FlashD5NextConfig -> {
                    check(current is D5NextConfig) { "Expected D5NextConfig for device ${device.alias}, got ${current::class.simpleName}" }
                    val newCoolantOffset = request.coolantOffset
                    D5NextConfig(
                        pump = request.pump ?: current.pump,
                        fan = request.fan ?: current.fan,
                        coolantOffset =
                            if (newCoolantOffset != null) {
                                SensorOffset(0, newCoolantOffset)
                            } else {
                                current.coolantOffset
                            },
                        display = request.display ?: current.display,
                        rgbpx = current.rgbpx.applyRgbpxUpdates(request.rgbpx),
                    )
                }
            }

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
            while (currentCoroutineContext().isActive) {
                val config = readConfig(device)
                val allSensorsStorage = loadMockVirtualSensorsStorage()
                val virtualSensorsMap =
                    allSensorsStorage[device.alias.value] ?: allSensorsStorage[device.id.value] ?: emptyMap()

                when (device.type) {
                    DeviceType.OCTO -> {
                        check(config is OctoConfig)
                        emit(generateOctoTelemetry(config, virtualSensorsMap))
                    }

                    DeviceType.QUADRO -> {
                        check(config is QuadroConfig)
                        emit(generateQuadroTelemetry(config, virtualSensorsMap))
                    }

                    DeviceType.D5_NEXT -> {
                        check(config is D5NextConfig)
                        emit(generateD5NextTelemetry(config, virtualSensorsMap))
                    }

                    else -> error("Telemetry simulation for ${device.type} is not implemented")
                }
                delay(1.seconds)
            }
        }

    private fun generateOctoTelemetry(
        config: OctoConfig,
        virtualSensorsMap: Map<Int, Double?>,
    ): OctoTelemetry {
        val physicalSensors =
            listOf(0 to 32.5, 1 to 22.8, 2 to 27.4, 3 to 43.1).map { (ch, baseTemp) ->
                val offset = config.sensorOffsets.firstOrNull { it.channel == ch }?.offsetCelsius ?: 0.0
                SensorTelemetry(channel = ch, tempCelsius = round((baseTemp + offset) * 10.0) / 10.0, isVirtual = false)
            }
        val virtualSensors =
            (4..19).map { ch ->
                SensorTelemetry(channel = ch, tempCelsius = virtualSensorsMap[ch], isVirtual = true)
            }
        val allSensors = (physicalSensors + virtualSensors).associate { it.channel to it.tempCelsius }

        val fans =
            (0..7).map { ch ->
                val fanConfig = config.fans.firstOrNull { it.channel == ch }
                simulateFan(
                    ch,
                    fanConfig,
                    allSensors,
                    maxRpm = if (ch == 0) 2500 else 1800,
                    maxAmps = if (ch == 0) 0.70 else 0.30
                )
            }

        val flowRateDlH =
            if (fans.any { it.rpm > 0 }) {
                val pumpPwm = fans.firstOrNull { it.channel == 0 }?.pwmPercent ?: 50.0
                round(60.0 + (pumpPwm / 100.0) * 140.0).toInt()
            } else {
                null
            }

        return OctoTelemetry(fans = fans, sensors = physicalSensors + virtualSensors, flowRateDlH = flowRateDlH)
    }

    private fun generateQuadroTelemetry(
        config: QuadroConfig,
        virtualSensorsMap: Map<Int, Double?>,
    ): QuadroTelemetry {
        val physicalSensors =
            listOf(0 to 29.5, 1 to 24.0, 2 to 26.2, 3 to 31.0).map { (ch, baseTemp) ->
                val offset = config.sensorOffsets.firstOrNull { it.channel == ch }?.offsetCelsius ?: 0.0
                SensorTelemetry(channel = ch, tempCelsius = round((baseTemp + offset) * 10.0) / 10.0, isVirtual = false)
            }
        val virtualSensors =
            (4..19).map { ch ->
                SensorTelemetry(channel = ch, tempCelsius = virtualSensorsMap[ch], isVirtual = true)
            }
        val allSensors = (physicalSensors + virtualSensors).associate { it.channel to it.tempCelsius }

        val fans =
            (0..3).map { ch ->
                val fanConfig = config.fans.firstOrNull { it.channel == ch }
                simulateFan(ch, fanConfig, allSensors, maxRpm = 2000, maxAmps = 0.35)
            }

        val flowRateDlH =
            if (fans.any { it.rpm > 0 }) {
                val fan0Pwm = fans.firstOrNull { it.channel == 0 }?.pwmPercent ?: 50.0
                round(50.0 + (fan0Pwm / 100.0) * 120.0).toInt()
            } else {
                null
            }

        return QuadroTelemetry(fans = fans, sensors = physicalSensors + virtualSensors, flowRateDlH = flowRateDlH)
    }

    private fun generateD5NextTelemetry(
        config: D5NextConfig,
        virtualSensorsMap: Map<Int, Double?>,
    ): D5NextTelemetry {
        val baseCoolant = 28.5
        val coolantOffset = config.coolantOffset.offsetCelsius
        val coolantTemp = round((baseCoolant + coolantOffset) * 10.0) / 10.0

        val virtualSensors =
            (1..8).map { ch ->
                SensorTelemetry(channel = ch, tempCelsius = virtualSensorsMap[ch], isVirtual = true)
            }
        val allSensors =
            (listOf(SensorTelemetry(0, coolantTemp)) + virtualSensors).associate { it.channel to it.tempCelsius }

        val pump = simulateFan(0, config.pump, allSensors, maxRpm = 4800, maxAmps = 1.8)
        val fan = simulateFan(1, config.fan, allSensors, maxRpm = 2000, maxAmps = 0.5)

        return D5NextTelemetry(
            pump = pump,
            fan = fan,
            coolantTempCelsius = coolantTemp,
            virtualSensors = virtualSensors,
        )
    }

    private fun simulateFan(
        channel: Int,
        config: FanChannelConfig?,
        allSensors: Map<Int, Double?>,
        maxRpm: Int,
        maxAmps: Double,
    ): FanTelemetry {
        val pwm = calculatePwm(config, allSensors)
        val finalPwm = round(pwm * 10.0) / 10.0

        return if (finalPwm == 0.0) {
            FanTelemetry(
                channel = channel,
                rpm = 0,
                powerWatts = 0.0,
                voltageVolts = 0.0,
                currentAmps = 0.0,
                pwmPercent = 0.0,
            )
        } else {
            val voltage = 12.0
            val rpm = round(maxRpm * (finalPwm / 100.0)).toInt()
            val current = round((maxAmps * (finalPwm / 100.0)) * 1000.0) / 1000.0
            val power = round(voltage * current * 100.0) / 100.0
            FanTelemetry(
                channel = channel,
                rpm = rpm,
                powerWatts = power,
                voltageVolts = voltage,
                currentAmps = current,
                pwmPercent = finalPwm,
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
