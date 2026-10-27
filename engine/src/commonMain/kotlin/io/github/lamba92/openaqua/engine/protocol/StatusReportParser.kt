package io.github.lamba92.openaqua.engine.protocol

import io.github.lamba92.openaqua.model.CoolingChannelKind
import io.github.lamba92.openaqua.model.CoolingChannelTelemetry
import io.github.lamba92.openaqua.model.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.DeviceRegistry
import io.github.lamba92.openaqua.model.DeviceTelemetry
import io.github.lamba92.openaqua.model.FeatureState
import io.github.lamba92.openaqua.model.SensorChannelKind
import io.github.lamba92.openaqua.model.SensorTelemetry
import kotlin.math.round

object StatusReportParser {
    fun parse(
        device: DetectedHardwareDevice,
        report: UByteArray,
    ): DeviceTelemetry {
        val descriptor = DeviceRegistry.findByType(device.type)

        val fans =
            descriptor.fanChannels.map { fan ->
                parseFan(fan.channel, report, fan.statusOffset, fan.kind)
            }

        val physicalSensors =
            descriptor.tempSensors.map { sensor ->
                parseTemp(sensor.channel, report, sensor.statusOffset, sensor.kind)
            }

        val virtualSensors =
            parseVirtualSensors(
                report = report,
                startOffset = descriptor.virtualSensorsStartOffset,
                count = descriptor.virtualSensorsCount,
                startChannel = descriptor.tempSensors.size,
            )

        val flowOffset = descriptor.flowSensorOffset
        val flowRateDlH =
            if (descriptor.hasFlowSensor && flowOffset != null) {
                val flowRaw = readU16Be(report, flowOffset)
                if (flowRaw in 1..65534) FeatureState.Active(flowRaw) else FeatureState.Disconnected
            } else {
                FeatureState.Unsupported
            }

        return DeviceTelemetry(
            fans = fans,
            sensors = physicalSensors + virtualSensors,
            flowRateDlH = flowRateDlH,
            display = if (descriptor.hasDisplay) FeatureState.Disconnected else FeatureState.Unsupported,
        )
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
        kind: CoolingChannelKind = CoolingChannelKind.FAN,
    ): CoolingChannelTelemetry {
        val rawPwm = readU16Be(report, baseOffset + 0)
        val rawVoltage = readU16Be(report, baseOffset + 2)
        val rawCurrent = readU16Be(report, baseOffset + 4)
        val rawPower = readU16Be(report, baseOffset + 6)
        val rawRpm = readU16Be(report, baseOffset + 8)

        val pwmPercent = round((rawPwm.toDouble() / 100.0) * 10.0) / 10.0
        val voltageVolts = round((rawVoltage.toDouble() / 100.0) * 100.0) / 100.0
        val currentAmps = round((rawCurrent.toDouble() / 1000.0) * 1000.0) / 1000.0
        val powerWatts = round((rawPower.toDouble() / 100.0) * 100.0) / 100.0

        val status = if (baseOffset + 12 < report.size) report[baseOffset + 12].toInt() else 0
        val isDisconnected = rawRpm == 0 && ((status and 0x0C) != 0 || (currentAmps == 0.0 && powerWatts == 0.0))

        return if (isDisconnected) {
            CoolingChannelTelemetry.Disconnected(
                channel = channel,
                kind = kind,
            )
        } else {
            CoolingChannelTelemetry.Connected(
                channel = channel,
                kind = kind,
                rpm = rawRpm,
                powerWatts = powerWatts,
                voltageVolts = voltageVolts,
                currentAmps = currentAmps,
                pwmPercent = pwmPercent,
            )
        }
    }

    private fun parseTemp(
        channel: Int,
        report: UByteArray,
        offset: Int,
        kind: SensorChannelKind = SensorChannelKind.PHYSICAL_NTC,
    ): SensorTelemetry {
        val raw = readI16Be(report, offset).toInt()
        return if (raw == 0x7FFF || raw == -32768) {
            SensorTelemetry.Disconnected(
                channel = channel,
                kind = kind,
            )
        } else {
            SensorTelemetry.Connected(
                channel = channel,
                kind = kind,
                tempCelsius = round((raw.toDouble() / 100.0) * 10.0) / 10.0,
            )
        }
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
                kind = SensorChannelKind.VIRTUAL,
            )
        }
    }
}
