package com.openaqua.engine.protocol

import com.openaqua.engine.util.crc16Usb
import com.openaqua.model.CurveFanConfig
import com.openaqua.model.CurvePoint
import com.openaqua.model.D5NextConfig
import com.openaqua.model.DeviceConfig
import com.openaqua.model.DeviceDescriptor
import com.openaqua.model.FanChannelConfig
import com.openaqua.model.FixedFanConfig
import com.openaqua.model.FlashConfigRequest
import com.openaqua.model.FlashD5NextConfig
import com.openaqua.model.FlashOctoConfig
import com.openaqua.model.FlashQuadroConfig
import com.openaqua.model.OctoConfig
import com.openaqua.model.QuadroConfig
import com.openaqua.model.SensorOffset
import com.openaqua.model.TargetTempFanConfig
import com.openaqua.model.TwoPointFanConfig
import com.openaqua.model.UnconfiguredFanConfig
import kotlin.math.abs
import kotlin.math.roundToInt

object ConfigWriter {
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
                report[baseOffset + 0] = ConfigParser.MODE_FIXED.toUByte()
                val rawPwm = (config.powerPercent * 100.0).roundToInt().coerceIn(0, 10000).toUShort()
                report[baseOffset + 1] = ((rawPwm.toInt() ushr 8) and 0xFF).toUByte()
                report[baseOffset + 2] = (rawPwm.toInt() and 0xFF).toUByte()
            }
            is CurveFanConfig -> {
                report[baseOffset + 0] = ConfigParser.MODE_CURVE.toUByte()
                report[baseOffset + 3] = ((config.sensorIndex ushr 8) and 0xFF).toUByte()
                report[baseOffset + 4] = (config.sensorIndex and 0xFF).toUByte()
                encodeCurvePoints(report, baseOffset, config.points)
            }
            is TargetTempFanConfig -> {
                report[baseOffset + 0] = ConfigParser.MODE_TARGET_TEMP.toUByte()
                report[baseOffset + 3] = ((config.sensorIndex ushr 8) and 0xFF).toUByte()
                report[baseOffset + 4] = (config.sensorIndex and 0xFF).toUByte()
                val rawTemp = (config.targetTempCelsius * 100.0).roundToInt().coerceIn(-5000, 15000).toShort()
                report[baseOffset + 5] = ((rawTemp.toInt() ushr 8) and 0xFF).toUByte()
                report[baseOffset + 6] = (rawTemp.toInt() and 0xFF).toUByte()
            }
            is TwoPointFanConfig -> {
                report[baseOffset + 0] = ConfigParser.MODE_TWO_POINT.toUByte()
                report[baseOffset + 3] = ((config.sensorIndex ushr 8) and 0xFF).toUByte()
                report[baseOffset + 4] = (config.sensorIndex and 0xFF).toUByte()
            }
            is UnconfiguredFanConfig -> {
                report[baseOffset + 0] = config.rawMode.toUByte()
                report[baseOffset + 3] = 0xFFu
                report[baseOffset + 4] = 0xFFu
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

    private fun applyFanAndSensorOffsets(
        report: UByteArray,
        descriptor: DeviceDescriptor,
        fans: Map<Int, FanChannelConfig>?,
        sensorOffsets: Map<Int, Double>?,
    ) {
        fans?.forEach { (channel, config) ->
            val fan =
                descriptor.fanChannels.firstOrNull { it.channel == channel }
                    ?: error("Unknown fan channel $channel for ${descriptor.name}")
            encodeFanChannelConfig(report, fan.ctrlOffset, config)
        }
        if (sensorOffsets != null) {
            val baseOffset =
                descriptor.tempCtrlOffset
                    ?: error("${descriptor.name} does not support temperature offset configuration")
            sensorOffsets.forEach { (channel, offset) ->
                descriptor.tempSensors.firstOrNull { it.channel == channel }
                    ?: error("Unknown sensor channel $channel for ${descriptor.name}")
                encodeSensorOffset(report, baseOffset, channel, offset)
            }
        }
    }

    fun applyFlashConfig(
        report: UByteArray,
        descriptor: DeviceDescriptor,
        request: FlashConfigRequest,
    ) {
        when (request) {
            is FlashOctoConfig -> {
                applyFanAndSensorOffsets(report, descriptor, request.fans, request.sensorOffsets)
            }
            is FlashQuadroConfig -> {
                applyFanAndSensorOffsets(report, descriptor, request.fans, request.sensorOffsets)
            }
            is FlashD5NextConfig -> {
                request.pump?.let { pumpConfig ->
                    val fan =
                        descriptor.fanChannels.firstOrNull { it.channel == 0 }
                            ?: error("Pump channel not found for ${descriptor.name}")
                    encodeFanChannelConfig(report, fan.ctrlOffset, pumpConfig)
                }
                request.fan?.let { fanConfig ->
                    val fan =
                        descriptor.fanChannels.firstOrNull { it.channel == 1 }
                            ?: error("Fan channel not found for ${descriptor.name}")
                    encodeFanChannelConfig(report, fan.ctrlOffset, fanConfig)
                }
                request.coolantOffset?.let { offset ->
                    val baseOffset =
                        descriptor.tempCtrlOffset
                            ?: error("${descriptor.name} does not support temperature offset configuration")
                    encodeSensorOffset(report, baseOffset, 0, offset)
                }
            }
        }
    }

    private fun verifyFansAndOffsets(
        expectedFans: Map<Int, FanChannelConfig>?,
        expectedOffsets: Map<Int, Double>?,
        actualFans: List<FanChannelConfig>,
        actualOffsets: List<SensorOffset>,
    ) {
        expectedFans?.forEach { (channel, expConfig) ->
            val actConfig =
                actualFans.getOrNull(channel)
                    ?: error("Readback missing fan channel $channel")
            verifyFanChannelConfig(expected = expConfig, actual = actConfig)
        }
        expectedOffsets?.forEach { (channel, expOffset) ->
            val actOffset =
                actualOffsets.firstOrNull { it.channel == channel }?.offsetCelsius
                    ?: error("Readback missing sensor channel $channel")
            check(abs(actOffset - expOffset) <= 0.1) {
                "Verification failed on sensor $channel: expected offset $expOffset°C, read $actOffset°C"
            }
        }
    }

    fun verifyFlashConfig(
        expected: FlashConfigRequest,
        actual: DeviceConfig,
    ) {
        when (expected) {
            is FlashOctoConfig -> {
                check(actual is OctoConfig) {
                    "Verification failed: expected OctoConfig, got ${actual::class.simpleName}"
                }
                verifyFansAndOffsets(expected.fans, expected.sensorOffsets, actual.fans, actual.sensorOffsets)
            }
            is FlashQuadroConfig -> {
                check(actual is QuadroConfig) {
                    "Verification failed: expected QuadroConfig, got ${actual::class.simpleName}"
                }
                verifyFansAndOffsets(expected.fans, expected.sensorOffsets, actual.fans, actual.sensorOffsets)
            }
            is FlashD5NextConfig -> {
                check(actual is D5NextConfig) {
                    "Verification failed: expected D5NextConfig, got ${actual::class.simpleName}"
                }
                expected.pump?.let { expPump ->
                    verifyFanChannelConfig(expected = expPump, actual = actual.pump)
                }
                expected.fan?.let { expFan ->
                    verifyFanChannelConfig(expected = expFan, actual = actual.fan)
                }
                expected.coolantOffset?.let { expOffset ->
                    check(abs(actual.coolantOffset.offsetCelsius - expOffset) <= 0.1) {
                        "Verification failed on coolant offset: expected $expOffset°C, read ${actual.coolantOffset.offsetCelsius}°C"
                    }
                }
            }
        }
    }

    fun verifyFanChannelConfig(
        expected: FanChannelConfig,
        actual: FanChannelConfig,
    ) {
        when (expected) {
            is FixedFanConfig -> {
                check(actual is FixedFanConfig) {
                    "Verification failed: expected FixedFanConfig, got ${actual::class.simpleName}"
                }
                check(abs(actual.powerPercent - expected.powerPercent) <= 0.2) {
                    "Verification failed: expected power ${expected.powerPercent}%, read ${actual.powerPercent}%"
                }
            }
            is CurveFanConfig -> {
                check(actual is CurveFanConfig) {
                    "Verification failed: expected CurveFanConfig, got ${actual::class.simpleName}"
                }
                check(actual.sensorIndex == expected.sensorIndex) {
                    "Verification failed: expected sensorIndex ${expected.sensorIndex}, read ${actual.sensorIndex}"
                }
                for (i in 0 until 16) {
                    val expPt = expected.points[i]
                    val actPt = actual.points[i]
                    check(abs(actPt.temp - expPt.temp) <= 0.2 && abs(actPt.pwm - expPt.pwm) <= 0.2) {
                        "Verification failed at point $i: expected (${expPt.temp}°C, ${expPt.pwm}%), read (${actPt.temp}°C, ${actPt.pwm}%)"
                    }
                }
            }
            is TargetTempFanConfig -> {
                check(actual is TargetTempFanConfig) {
                    "Verification failed: expected TargetTempFanConfig, got ${actual::class.simpleName}"
                }
                check(actual.sensorIndex == expected.sensorIndex) {
                    "Verification failed: expected sensorIndex ${expected.sensorIndex}, read ${actual.sensorIndex}"
                }
                check(abs(actual.targetTempCelsius - expected.targetTempCelsius) <= 0.2) {
                    "Verification failed: expected targetTemp ${expected.targetTempCelsius}°C, read ${actual.targetTempCelsius}°C"
                }
            }
            is TwoPointFanConfig -> {
                check(actual is TwoPointFanConfig) {
                    "Verification failed: expected TwoPointFanConfig, got ${actual::class.simpleName}"
                }
                check(actual.sensorIndex == expected.sensorIndex) {
                    "Verification failed: expected sensorIndex ${expected.sensorIndex}, read ${actual.sensorIndex}"
                }
            }
            is UnconfiguredFanConfig -> Unit
        }
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
