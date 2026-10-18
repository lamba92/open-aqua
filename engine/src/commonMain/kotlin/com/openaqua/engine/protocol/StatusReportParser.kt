package com.openaqua.engine.protocol

import com.openaqua.model.D5NextTelemetry
import com.openaqua.model.DetectedHardwareDevice
import com.openaqua.model.DeviceRegistry
import com.openaqua.model.DeviceTelemetry
import com.openaqua.model.DeviceType
import com.openaqua.model.FanTelemetry
import com.openaqua.model.OctoTelemetry
import com.openaqua.model.QuadroTelemetry
import com.openaqua.model.SensorTelemetry
import kotlin.math.round

object StatusReportParser {
    fun parse(
        device: DetectedHardwareDevice,
        report: UByteArray,
    ): DeviceTelemetry =
        when (device.type) {
            DeviceType.OCTO -> parseOcto(report)
            DeviceType.QUADRO -> parseQuadro(report)
            DeviceType.D5_NEXT -> parseD5Next(report)
            else -> error("Unsupported device type for telemetry parsing: ${device.type}")
        }

    private fun readU16Be(
        report: UByteArray,
        offset: Int,
    ): Int {
        if (offset + 1 >= report.size) return 0
        return ((report[offset].toInt() and 0xFF) shl 8) or (report[offset + 1].toInt() and 0xFF)
    }

    private fun readI16Be(
        report: UByteArray,
        offset: Int,
    ): Short {
        if (offset + 1 >= report.size) return 0
        return (((report[offset].toInt() and 0xFF) shl 8) or (report[offset + 1].toInt() and 0xFF)).toShort()
    }

    private fun parseFan(
        channel: Int,
        report: UByteArray,
        baseOffset: Int,
    ): FanTelemetry {
        val rawPwm = readU16Be(report, baseOffset + 0)
        val rawVoltage = readU16Be(report, baseOffset + 2)
        val rawCurrent = readU16Be(report, baseOffset + 4)
        val rawPower = readU16Be(report, baseOffset + 6)
        val rawRpm = readU16Be(report, baseOffset + 8)

        val pwmPercent = round((rawPwm.toDouble() / 100.0) * 10.0) / 10.0
        val voltageVolts = round((rawVoltage.toDouble() / 100.0) * 100.0) / 100.0
        val currentAmps = round((rawCurrent.toDouble() / 1000.0) * 1000.0) / 1000.0
        val powerWatts = round((rawPower.toDouble() / 100.0) * 100.0) / 100.0

        return FanTelemetry(
            channel = channel,
            rpm = rawRpm,
            powerWatts = powerWatts,
            voltageVolts = voltageVolts,
            currentAmps = currentAmps,
            pwmPercent = pwmPercent,
        )
    }

    private fun parseTemp(
        channel: Int,
        report: UByteArray,
        offset: Int,
        isVirtual: Boolean = false,
    ): SensorTelemetry {
        val raw = readI16Be(report, offset).toInt()
        val tempCelsius =
            if (raw == 0x7FFF || raw == -32768) {
                null
            } else {
                round((raw.toDouble() / 100.0) * 10.0) / 10.0
            }

        return SensorTelemetry(
            channel = channel,
            tempCelsius = tempCelsius,
            isVirtual = isVirtual,
        )
    }

    private fun parseVirtualSensors(
        report: UByteArray,
        startOffset: Int?,
        count: Int,
        startChannel: Int,
    ): List<SensorTelemetry> {
        if (startOffset == null || count == 0) return emptyList()
        return (0 until count).map { i ->
            parseTemp(
                channel = startChannel + i,
                report = report,
                offset = startOffset + (i * 2),
                isVirtual = true,
            )
        }
    }

    private fun parseOcto(report: UByteArray): OctoTelemetry {
        val octoFanOffsets = intArrayOf(0x7D, 0x8A, 0x97, 0xA4, 0xB1, 0xBE, 0xCB, 0xD8)
        val descriptor = DeviceRegistry.OCTO

        val fans =
            descriptor.fanChannels.mapIndexed { index, fan ->
                val base = octoFanOffsets.getOrElse(index) { 0x7D + (index * 0x0D) }
                parseFan(fan.channel, report, base)
            }

        val tempStart = 0x3D
        val physicalSensors =
            descriptor.tempSensors.mapIndexed { index, sensor ->
                parseTemp(sensor.channel, report, tempStart + (index * 2), isVirtual = false)
            }

        val virtualSensors =
            parseVirtualSensors(
                report = report,
                startOffset = descriptor.virtualSensorsStartOffset,
                count = descriptor.virtualSensorsCount,
                startChannel = 4,
            )

        return OctoTelemetry(
            fans = fans,
            sensors = physicalSensors + virtualSensors,
        )
    }

    private fun parseQuadro(report: UByteArray): QuadroTelemetry {
        val quadroFanOffsets = intArrayOf(0x70, 0x7D, 0x8A, 0x97)
        val descriptor = DeviceRegistry.QUADRO

        val fans =
            descriptor.fanChannels.mapIndexed { index, fan ->
                val base = quadroFanOffsets.getOrElse(index) { 0x70 + (index * 0x0D) }
                parseFan(fan.channel, report, base)
            }

        val tempStart = 0x34
        val physicalSensors =
            descriptor.tempSensors.mapIndexed { index, sensor ->
                parseTemp(sensor.channel, report, tempStart + (index * 2), isVirtual = false)
            }

        val virtualSensors =
            parseVirtualSensors(
                report = report,
                startOffset = descriptor.virtualSensorsStartOffset,
                count = descriptor.virtualSensorsCount,
                startChannel = 4,
            )

        val flowRaw = readU16Be(report, 0x6E)
        val flowRateDlH = if (flowRaw in 1..65534) flowRaw else null

        return QuadroTelemetry(
            fans = fans,
            sensors = physicalSensors + virtualSensors,
            flowRateDlH = flowRateDlH,
        )
    }

    private fun parseD5Next(report: UByteArray): D5NextTelemetry {
        val descriptor = DeviceRegistry.D5_NEXT
        val pump = parseFan(channel = 0, report = report, baseOffset = 0x6C)
        val fan = parseFan(channel = 1, report = report, baseOffset = 0x5F)
        val coolantTemp = parseTemp(channel = 0, report = report, offset = 0x57).tempCelsius

        val virtualSensors =
            parseVirtualSensors(
                report = report,
                startOffset = descriptor.virtualSensorsStartOffset,
                count = descriptor.virtualSensorsCount,
                startChannel = 1,
            )

        return D5NextTelemetry(
            pump = pump,
            fan = fan,
            coolantTempCelsius = coolantTemp,
            virtualSensors = virtualSensors,
        )
    }
}
