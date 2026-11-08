package io.github.lamba92.openaqua.engine.device.protocol

import io.github.lamba92.openaqua.model.FeatureState
import io.github.lamba92.openaqua.model.FeatureState.Active
import io.github.lamba92.openaqua.model.device.CoolingChannelKind
import io.github.lamba92.openaqua.model.device.CoolingChannelTelemetry
import io.github.lamba92.openaqua.model.device.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.device.DeviceCapability
import io.github.lamba92.openaqua.model.device.DeviceTelemetry
import io.github.lamba92.openaqua.model.device.SensorChannelKind
import io.github.lamba92.openaqua.model.device.SensorTelemetry
import io.github.lamba92.openaqua.model.device.descriptor
import io.github.lamba92.openaqua.model.device.findCapability
import kotlin.math.round

/**
 * Parses raw HID status reports (Report ID 0x01 or 0x02 depending on device) into high-level [DeviceTelemetry].
 */
object StatusReportParser {
    /**
     * Parses the binary status report of a detected device.
     *
     * @param device Detected device descriptor holder.
     * @param report Binary payload of the incoming HID status report.
     * @return Fully populated [DeviceTelemetry] snapshot.
     */
    fun parse(
        device: DetectedHardwareDevice,
        report: UByteArray,
    ): DeviceTelemetry {
        val descriptor = device.descriptor()
        val fans = when (val fansCap = descriptor.findCapability<DeviceCapability.Fans>()) {
            null -> emptyList()
            else -> fansCap.channels.map { fan ->
                parseFan(fan.channel, report, fan.statusOffset, fan.kind)
            }
        }
        val tempCap = descriptor.findCapability<DeviceCapability.TemperatureSensors>()
        val physicalSensors = when (tempCap) {
            null -> emptyList()
            else -> tempCap.sensors.map { sensor ->
                parseTemp(sensor.channel, report, sensor.statusOffset, sensor.kind)
            }
        }
        val virtualSensors = when (val virtCap = descriptor.findCapability<DeviceCapability.VirtualSensors>()) {
            null -> emptyList()
            else ->
                parseVirtualSensors(
                    report = report,
                    statusOffset = virtCap.statusOffset,
                    count = virtCap.count,
                    startChannel = tempCap?.sensors?.size ?: 0,
                )
        }
        val flowRateDlH = when (val flowCap = descriptor.findCapability<DeviceCapability.FlowSensor>()) {
            is DeviceCapability.FlowSensor -> {
                when (val flowRaw = readU16Be(report, flowCap.statusOffset)) {
                    in 1..65534 -> Active(flowRaw)
                    else -> FeatureState.Disconnected
                }
            }
            null -> FeatureState.Unsupported
        }
        val display = when (descriptor.findCapability<DeviceCapability.Display>()) {
            is DeviceCapability.Display -> FeatureState.Disconnected
            null -> FeatureState.Unsupported
        }
        return DeviceTelemetry(
            fans = fans,
            sensors = physicalSensors + virtualSensors,
            flowRateDlH = flowRateDlH,
            display = display,
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
        statusOffset: Int,
        count: Int,
        startChannel: Int,
    ): List<SensorTelemetry> {
        if (count == 0) return emptyList()
        return (0 until count).map { i ->
            parseTemp(
                channel = startChannel + i,
                report = report,
                offset = statusOffset + (i * 2),
                kind = SensorChannelKind.VIRTUAL,
            )
        }
    }
}
