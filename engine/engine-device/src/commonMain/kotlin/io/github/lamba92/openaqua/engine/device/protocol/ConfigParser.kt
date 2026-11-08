package io.github.lamba92.openaqua.engine.device.protocol

import io.github.lamba92.openaqua.model.FeatureState
import io.github.lamba92.openaqua.model.device.CopyFanConfig
import io.github.lamba92.openaqua.model.device.CurveFanConfig
import io.github.lamba92.openaqua.model.device.CurvePoint
import io.github.lamba92.openaqua.model.device.DeviceCapability
import io.github.lamba92.openaqua.model.device.DeviceConfig
import io.github.lamba92.openaqua.model.device.DeviceDescriptor
import io.github.lamba92.openaqua.model.device.FanCapability
import io.github.lamba92.openaqua.model.device.FanChannelConfig
import io.github.lamba92.openaqua.model.device.FanChannelDescriptor
import io.github.lamba92.openaqua.model.device.FanLimits
import io.github.lamba92.openaqua.model.device.FixedFanConfig
import io.github.lamba92.openaqua.model.device.RgbEffectMode
import io.github.lamba92.openaqua.model.device.RgbpxPortConfig
import io.github.lamba92.openaqua.model.device.SensorOffset
import io.github.lamba92.openaqua.model.device.TargetTempFanConfig
import io.github.lamba92.openaqua.model.device.TwoPointFanConfig
import io.github.lamba92.openaqua.model.device.UnconfiguredFanConfig
import io.github.lamba92.openaqua.model.device.findCapability
import io.github.lamba92.openaqua.model.device.findFanCapability
import io.github.lamba92.openaqua.model.device.hardwareToLogicalSensorIndex
import kotlin.math.round

object ConfigParser {
    fun parseCurvePoints(
        report: UByteArray,
        baseOffset: Int,
    ): List<CurvePoint> =
        (0 until 16).map { i ->
            val tOffset = baseOffset + 0x15 + (i * 2)
            val pOffset = baseOffset + 0x35 + (i * 2)

            val tRaw = ((report[tOffset].toInt() shl 8) or report[tOffset + 1].toInt()).toShort()
            val pRaw = ((report[pOffset].toInt() shl 8) or report[pOffset + 1].toInt()).toUShort()

            val temp = round((tRaw.toDouble() / 100.0) * 10.0) / 10.0
            val pwm = round((pRaw.toDouble() / 100.0) * 10.0) / 10.0

            CurvePoint(temp = temp, pwm = pwm)
        }

    fun parseFanChannelConfig(
        report: UByteArray,
        baseOffset: Int,
        channel: Int,
        descriptor: DeviceDescriptor,
        limits: FanLimits = FanLimits.Standard,
    ): FanChannelConfig {
        val mode = report[baseOffset].toInt() and 0xFF
        val rawSensor = ((report[baseOffset + 3].toInt() and 0xFF shl 8) or (report[baseOffset + 4].toInt() and 0xFF))

        val totalSensors = descriptor.findCapability<DeviceCapability.TemperatureSensors>()?.sensors?.size ?: 0
        val virtualCount = descriptor.findCapability<DeviceCapability.VirtualSensors>()?.count ?: 0
        val flowSensorSlotCount = when (descriptor.findCapability<DeviceCapability.FlowSensor>()) {
            is DeviceCapability.FlowSensor.Calibrated -> 1
            else -> 0
        }
        val maxHwSensorIndex = totalSensors + flowSensorSlotCount + virtualCount - 1

        val sensorIndex =
            when {
                rawSensor in 0..maxHwSensorIndex -> descriptor.hardwareToLogicalSensorIndex(rawSensor)
                else -> null
            }

        val followCap = descriptor.findFanCapability<FanCapability.Follow>()
        val fixedByte = descriptor.findFanCapability<FanCapability.Fixed>()?.modeByte
        val curveByte = descriptor.findFanCapability<FanCapability.Curve>()?.modeByte
        val targetTempByte = descriptor.findFanCapability<FanCapability.TargetTemp>()?.modeByte
        val twoPointByte = descriptor.findFanCapability<FanCapability.TwoPoint>()?.modeByte

        return when {
            followCap != null && mode in followCap.startModeByteInclusive..followCap.endModeByteInclusive -> {
                when (val sourceChannel = mode - followCap.startModeByteInclusive) {
                    channel -> {
                        UnconfiguredFanConfig(
                            channel = channel,
                            rawMode = mode,
                            limits = limits,
                        )
                    }
                    else -> {
                        CopyFanConfig(
                            channel = channel,
                            sourceChannel = sourceChannel,
                            limits = limits,
                        )
                    }
                }
            }

            fixedByte != null && mode == fixedByte -> {
                val pRaw = ((report[baseOffset + 1].toInt() and 0xFF shl 8) or (report[baseOffset + 2].toInt() and 0xFF))
                FixedFanConfig(
                    channel = channel,
                    powerPercent = round((pRaw.toDouble() / 100.0) * 10.0) / 10.0,
                    limits = limits,
                )
            }

            curveByte != null && mode == curveByte -> {
                when (sensorIndex) {
                    null -> {
                        UnconfiguredFanConfig(
                            channel = channel,
                            rawMode = mode,
                            limits = limits,
                        )
                    }
                    else -> {
                        CurveFanConfig(
                            channel = channel,
                            sensorIndex = sensorIndex,
                            points = parseCurvePoints(report, baseOffset),
                            limits = limits,
                        )
                    }
                }
            }

            targetTempByte != null && mode == targetTempByte -> {
                when (sensorIndex) {
                    null -> {
                        UnconfiguredFanConfig(
                            channel = channel,
                            rawMode = mode,
                            limits = limits,
                        )
                    }
                    else -> {
                        val tRaw = ((report[baseOffset + 5].toInt() shl 8) or (report[baseOffset + 6].toInt() and 0xFF)).toShort()
                        TargetTempFanConfig(
                            channel = channel,
                            sensorIndex = sensorIndex,
                            targetTempCelsius = round((tRaw.toDouble() / 100.0) * 10.0) / 10.0,
                            limits = limits,
                        )
                    }
                }
            }

            twoPointByte != null && mode == twoPointByte -> {
                when (sensorIndex) {
                    null -> {
                        UnconfiguredFanConfig(
                            channel = channel,
                            rawMode = mode,
                            limits = limits,
                        )
                    }
                    else -> {
                        TwoPointFanConfig(
                            channel = channel,
                            sensorIndex = sensorIndex,
                            limits = limits,
                        )
                    }
                }
            }

            else -> {
                UnconfiguredFanConfig(
                    channel = channel,
                    rawMode = mode,
                    limits = limits,
                )
            }
        }
    }

    fun parseSensorOffset(
        report: UByteArray,
        baseOffset: Int,
        channel: Int,
    ): Double {
        val offsetPos = baseOffset + (channel * 2)
        val raw = ((report[offsetPos].toInt() shl 8) or (report[offsetPos + 1].toInt() and 0xFF)).toShort()
        return round((raw.toDouble() / 100.0) * 10.0) / 10.0
    }

    private fun parseFans(
        report: UByteArray,
        descriptor: DeviceDescriptor,
        fansCap: DeviceCapability.Fans,
    ): List<FanChannelConfig> =
        fansCap.channels.map { fan ->
            val limits = when (fan) {
                is FanChannelDescriptor.WithLimits -> parseFanLimits(report, fan.setupOffset)
                is FanChannelDescriptor.Basic -> FanLimits.Standard
            }
            parseFanChannelConfig(report, fan.ctrlOffset, fan.channel, descriptor, limits)
        }

    private fun parseOffsets(
        report: UByteArray,
        tempCap: DeviceCapability.TemperatureSensors.Calibrated,
    ): List<SensorOffset> =
        tempCap.sensors.map { sensor ->
            SensorOffset(sensor.channel, parseSensorOffset(report, tempCap.ctrlOffset, sensor.channel))
        }

    private fun defaultRgbpxPorts(rgbpxCap: DeviceCapability.Lighting.Rgbpx?): List<RgbpxPortConfig> =
        when (rgbpxCap) {
            null -> emptyList()
            else ->
                (0 until rgbpxCap.portCount).map { port ->
                    RgbpxPortConfig(
                        port = port,
                        enabled = true,
                        ledCount = 30,
                        brightnessPercent = 100,
                        mode = RgbEffectMode.STATIC,
                    )
                }
        }

    fun parseFanLimits(
        report: UByteArray,
        setupOffset: Int,
    ): FanLimits {
        val flags = report[setupOffset].toInt() and 0xFF
        val holdMin = (flags and 0x01) != 0
        val startBoost = (flags and 0x02) != 0
        val rawMin = ((report[setupOffset + 1].toInt() and 0xFF shl 8) or (report[setupOffset + 2].toInt() and 0xFF))
        val rawMax = ((report[setupOffset + 3].toInt() and 0xFF shl 8) or (report[setupOffset + 4].toInt() and 0xFF))
        val rawFallback = ((report[setupOffset + 5].toInt() and 0xFF shl 8) or (report[setupOffset + 6].toInt() and 0xFF))

        val minPower = (round((rawMin.toDouble() / 100.0) * 10.0) / 10.0).coerceIn(0.0, 100.0)
        val maxPower = (round((rawMax.toDouble() / 100.0) * 10.0) / 10.0).coerceIn(0.0, 100.0)
        val fallbackPower = (round((rawFallback.toDouble() / 100.0) * 10.0) / 10.0).coerceIn(0.0, 100.0)

        return FanLimits(
            minPowerPercent = minPower.coerceAtMost(maxPower),
            maxPowerPercent = maxPower,
            startBoost = startBoost,
            holdMinPower = holdMin,
            fallbackPowerPercent = fallbackPower,
        )
    }

    fun parseConfig(
        descriptor: DeviceDescriptor,
        report: UByteArray,
    ): DeviceConfig {
        val fans = when (val fansCap = descriptor.findCapability<DeviceCapability.Fans>()) {
            null -> emptyList()
            else -> parseFans(report, descriptor, fansCap)
        }
        val offsets = when (val tempCap = descriptor.findCapability<DeviceCapability.TemperatureSensors>()) {
            is DeviceCapability.TemperatureSensors.Calibrated -> parseOffsets(report, tempCap)
            else -> emptyList()
        }
        val flowPulses = when (val flowCap = descriptor.findCapability<DeviceCapability.FlowSensor>()) {
            is DeviceCapability.FlowSensor.Calibrated -> {
                val rawPulses = ((report[flowCap.pulsesCtrlOffset].toInt() and 0xFF shl 8) or (report[flowCap.pulsesCtrlOffset + 1].toInt() and 0xFF))
                when {
                    rawPulses > 0 -> FeatureState.Active(rawPulses)
                    else -> FeatureState.Disconnected
                }
            }
            else -> FeatureState.Unsupported
        }
        val display = when (descriptor.findCapability<DeviceCapability.Display>()) {
            is DeviceCapability.Display -> FeatureState.Disconnected
            null -> FeatureState.Unsupported
        }

        return DeviceConfig(
            fans = fans,
            sensorOffsets = offsets,
            flowPulsesPerLiter = flowPulses,
            display = display,
            rgbpx = defaultRgbpxPorts(descriptor.findCapability<DeviceCapability.Lighting.Rgbpx>()),
        )
    }
}
