package io.github.lamba92.openaqua.engine.device.protocol

import io.github.lamba92.openaqua.model.FeatureState
import io.github.lamba92.openaqua.model.device.CurveFanConfig
import io.github.lamba92.openaqua.model.device.CurvePoint
import io.github.lamba92.openaqua.model.device.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.device.DeviceConfig
import io.github.lamba92.openaqua.model.device.DeviceDescriptor
import io.github.lamba92.openaqua.model.device.DeviceType
import io.github.lamba92.openaqua.model.device.FanChannelConfig
import io.github.lamba92.openaqua.model.device.FixedFanConfig
import io.github.lamba92.openaqua.model.device.RgbEffectMode
import io.github.lamba92.openaqua.model.device.RgbpxPortConfig
import io.github.lamba92.openaqua.model.device.SensorOffset
import io.github.lamba92.openaqua.model.device.TargetTempFanConfig
import io.github.lamba92.openaqua.model.device.TwoPointFanConfig
import io.github.lamba92.openaqua.model.device.UnconfiguredFanConfig
import kotlin.math.round

object ConfigParser {
    const val MODE_FIXED = 0x00
    const val MODE_TARGET_TEMP = 0x01
    const val MODE_CURVE = 0x02
    const val MODE_TWO_POINT = 0x05

    // Physical sensors (0..3 on OCTO/QUADRO) + Flow (4) + Virtual sensors (5..20)
    const val MAX_SUPPORTED_SENSOR_INDEX = 20

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
        descriptor: DeviceDescriptor? = null,
    ): FanChannelConfig {
        val mode = report[baseOffset].toInt() and 0xFF
        val rawSensor = ((report[baseOffset + 3].toInt() and 0xFF shl 8) or (report[baseOffset + 4].toInt() and 0xFF))

        val maxHwSensorIndex =
            if (descriptor != null && (descriptor.type == DeviceType.OCTO || descriptor.type == DeviceType.QUADRO)) {
                descriptor.tempSensors.size + 1 + descriptor.virtualSensorsCount - 1
            } else {
                MAX_SUPPORTED_SENSOR_INDEX
            }

        val sensorIndex =
            if (rawSensor in 0..maxHwSensorIndex) {
                if (descriptor != null && (descriptor.type == DeviceType.OCTO || descriptor.type == DeviceType.QUADRO)) {
                    when {
                        rawSensor < 4 -> rawSensor
                        rawSensor == 4 -> null
                        else -> rawSensor - 1
                    }
                } else {
                    rawSensor
                }
            } else {
                null
            }

        return when (mode) {
            MODE_CURVE -> {
                if (sensorIndex == null) {
                    UnconfiguredFanConfig(
                        channel = channel,
                        rawMode = mode,
                    )
                } else {
                    CurveFanConfig(
                        channel = channel,
                        sensorIndex = sensorIndex,
                        points = parseCurvePoints(report, baseOffset),
                    )
                }
            }
            MODE_FIXED -> {
                val pRaw = ((report[baseOffset + 1].toInt() and 0xFF shl 8) or (report[baseOffset + 2].toInt() and 0xFF))
                FixedFanConfig(
                    channel = channel,
                    powerPercent = round((pRaw.toDouble() / 100.0) * 10.0) / 10.0,
                )
            }
            MODE_TARGET_TEMP -> {
                if (sensorIndex == null) {
                    UnconfiguredFanConfig(
                        channel = channel,
                        rawMode = mode,
                    )
                } else {
                    val tRaw = ((report[baseOffset + 5].toInt() shl 8) or (report[baseOffset + 6].toInt() and 0xFF)).toShort()
                    TargetTempFanConfig(
                        channel = channel,
                        sensorIndex = sensorIndex,
                        targetTempCelsius = round((tRaw.toDouble() / 100.0) * 10.0) / 10.0,
                    )
                }
            }
            MODE_TWO_POINT -> {
                if (sensorIndex == null) {
                    UnconfiguredFanConfig(
                        channel = channel,
                        rawMode = mode,
                    )
                } else {
                    TwoPointFanConfig(
                        channel = channel,
                        sensorIndex = sensorIndex,
                    )
                }
            }
            else -> {
                error("Unknown fan mode $mode (0x${mode.toString(16)}) for channel $channel at offset 0x${baseOffset.toString(16)}")
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
    ): List<FanChannelConfig> =
        descriptor.fanChannels.map { fan ->
            parseFanChannelConfig(report, fan.ctrlOffset, fan.channel, descriptor)
        }

    private fun parseOffsets(
        report: UByteArray,
        descriptor: DeviceDescriptor,
    ): List<SensorOffset> {
        val baseOffset = descriptor.tempCtrlOffset ?: error("No tempCtrlOffset for ${descriptor.name}")
        return descriptor.tempSensors.map { sensor ->
            SensorOffset(sensor.channel, parseSensorOffset(report, baseOffset, sensor.channel))
        }
    }

    private fun defaultRgbpxPorts(descriptor: DeviceDescriptor): List<RgbpxPortConfig> =
        (0 until descriptor.rgbpxPortCount).map { port ->
            RgbpxPortConfig(
                port = port,
                enabled = true,
                ledCount = 30,
                brightnessPercent = 100,
                mode = RgbEffectMode.STATIC,
            )
        }

    fun parseConfig(
        device: DetectedHardwareDevice,
        descriptor: DeviceDescriptor,
        report: UByteArray,
    ): DeviceConfig {
        val pulsesOffset = descriptor.flowPulsesCtrlOffset
        val flowPulses =
            if (descriptor.hasFlowSensor && pulsesOffset != null) {
                val rawPulses = ((report[pulsesOffset].toInt() and 0xFF shl 8) or (report[pulsesOffset + 1].toInt() and 0xFF))
                if (rawPulses > 0) FeatureState.Active(rawPulses) else FeatureState.Disconnected
            } else {
                FeatureState.Unsupported
            }

        return DeviceConfig(
            fans = parseFans(report, descriptor),
            sensorOffsets = parseOffsets(report, descriptor),
            flowPulsesPerLiter = flowPulses,
            display = if (descriptor.hasDisplay) FeatureState.Disconnected else FeatureState.Unsupported,
            rgbpx = defaultRgbpxPorts(descriptor),
        )
    }
}
