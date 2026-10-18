package com.openaqua.engine.protocol

import com.openaqua.engine.util.crc16Usb
import com.openaqua.model.CurveFanConfig
import com.openaqua.model.CurvePoint
import com.openaqua.model.FanChannelConfig
import com.openaqua.model.FixedFanConfig
import com.openaqua.model.TargetTempFanConfig
import com.openaqua.model.TwoPointFanConfig
import com.openaqua.model.UnconfiguredFanConfig
import com.openaqua.model.UnknownFanConfig
import kotlin.math.roundToInt

object ControlReportWriter {
    /**
     * Secondary HID Feature report sent after writing Report 0x03 to commit
     * changes to hardware memory (same report sent by official Aquasuite and Linux kernel driver).
     */
    val SECONDARY_COMMIT_REPORT: UByteArray =
        ubyteArrayOf(
            0x02u,
            0x00u,
            0x00u,
            0x00u,
            0x02u,
            0x00u,
            0x00u,
            0x00u,
            0x00u,
            0x34u,
            0xC6u,
        )

    fun encodeFanChannelConfig(
        report: UByteArray,
        baseOffset: Int,
        config: FanChannelConfig,
    ) {
        when (config) {
            is FixedFanConfig -> {
                report[baseOffset + 0] = ControlReportParser.MODE_FIXED.toUByte()
                val rawPwm = (config.powerPercent * 100.0).roundToInt().coerceIn(0, 10000).toUShort()
                report[baseOffset + 1] = ((rawPwm.toInt() ushr 8) and 0xFF).toUByte()
                report[baseOffset + 2] = (rawPwm.toInt() and 0xFF).toUByte()
            }
            is CurveFanConfig -> {
                report[baseOffset + 0] = ControlReportParser.MODE_CURVE.toUByte()
                report[baseOffset + 3] = ((config.sensorIndex ushr 8) and 0xFF).toUByte()
                report[baseOffset + 4] = (config.sensorIndex and 0xFF).toUByte()
                encodeCurvePoints(report, baseOffset, config.points)
            }
            is TargetTempFanConfig -> {
                report[baseOffset + 0] = ControlReportParser.MODE_TARGET_TEMP.toUByte()
                report[baseOffset + 3] = ((config.sensorIndex ushr 8) and 0xFF).toUByte()
                report[baseOffset + 4] = (config.sensorIndex and 0xFF).toUByte()
                val rawTemp = (config.targetTempCelsius * 100.0).roundToInt().coerceIn(-5000, 15000).toShort()
                report[baseOffset + 5] = ((rawTemp.toInt() ushr 8) and 0xFF).toUByte()
                report[baseOffset + 6] = (rawTemp.toInt() and 0xFF).toUByte()
            }
            is TwoPointFanConfig -> {
                report[baseOffset + 0] = ControlReportParser.MODE_TWO_POINT.toUByte()
                report[baseOffset + 3] = ((config.sensorIndex ushr 8) and 0xFF).toUByte()
                report[baseOffset + 4] = (config.sensorIndex and 0xFF).toUByte()
            }
            is UnconfiguredFanConfig -> {
                report[baseOffset + 0] = config.rawMode.toUByte()
                report[baseOffset + 3] = 0xFFu
                report[baseOffset + 4] = 0xFFu
            }
            is UnknownFanConfig -> {
                report[baseOffset + 0] = config.rawMode.toUByte()
            }
        }
    }

    fun encodeCurvePoints(
        report: UByteArray,
        baseOffset: Int,
        points: List<CurvePoint>,
    ) {
        require(points.size == 16) {
            "Expected exactly 16 curve points, got ${points.size}"
        }

        for (i in 0 until 16) {
            val pt = points[i]
            val tRaw = (pt.temp * 100.0).roundToInt().coerceIn(-5000, 15000).toShort()
            val pRaw = (pt.pwm * 100.0).roundToInt().coerceIn(0, 10000).toUShort()

            val tOffset = baseOffset + 0x15 + (i * 2)
            val pOffset = baseOffset + 0x35 + (i * 2)

            report[tOffset] = ((tRaw.toInt() ushr 8) and 0xFF).toUByte()
            report[tOffset + 1] = (tRaw.toInt() and 0xFF).toUByte()

            report[pOffset] = ((pRaw.toInt() ushr 8) and 0xFF).toUByte()
            report[pOffset + 1] = (pRaw.toInt() and 0xFF).toUByte()
        }
    }

    fun encodeSensorOffset(
        report: UByteArray,
        baseOffset: Int,
        channel: Int,
        offsetCelsius: Double,
    ) {
        val offsetPos = baseOffset + (channel * 2)
        val centiCelsius = (offsetCelsius * 100.0).roundToInt().coerceIn(-1500, 1500).toShort()

        report[offsetPos] = ((centiCelsius.toInt() ushr 8) and 0xFF).toUByte()
        report[offsetPos + 1] = (centiCelsius.toInt() and 0xFF).toUByte()
    }

    /**
     * Calculates CRC-16/USB over bytes 1 until reportLength - 2 and appends the
     * Big-Endian checksum at trailer [reportLength - 2].
     */
    fun finalizeReport(
        report: UByteArray,
        reportLength: Int,
    ) {
        val crcOffset = 1
        val crcLen = reportLength - 3
        val crc = crc16Usb(report, crcOffset, crcLen)
        val crcPos = reportLength - 2
        report[crcPos] = ((crc.toInt() ushr 8) and 0xFF).toUByte()
        report[crcPos + 1] = (crc.toInt() and 0xFF).toUByte()
    }
}
