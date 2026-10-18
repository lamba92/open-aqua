package com.openaqua.engine.protocol

import com.openaqua.model.CurveFanConfig
import com.openaqua.model.CurvePoint
import com.openaqua.model.FanChannelConfig
import com.openaqua.model.FixedFanConfig
import com.openaqua.model.TargetTempFanConfig
import com.openaqua.model.TwoPointFanConfig
import com.openaqua.model.UnconfiguredFanConfig
import com.openaqua.model.UnknownFanConfig
import kotlin.math.round

object ControlReportParser {
    const val MODE_FIXED = 0x00
    const val MODE_TARGET_TEMP = 0x01
    const val MODE_CURVE = 0x02
    const val MODE_TWO_POINT = 0x05

    // Physical sensors (0..3 on OCTO/QUADRO) + Virtual sensors (4..19)
    const val MAX_SUPPORTED_SENSOR_INDEX = 19

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
    ): FanChannelConfig {
        val mode = report[baseOffset].toInt() and 0xFF
        val rawSensor = ((report[baseOffset + 3].toInt() and 0xFF shl 8) or (report[baseOffset + 4].toInt() and 0xFF))
        val sensorIndex = if (rawSensor in 0..MAX_SUPPORTED_SENSOR_INDEX) rawSensor else null

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
                UnknownFanConfig(
                    channel = channel,
                    rawMode = mode,
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
}
