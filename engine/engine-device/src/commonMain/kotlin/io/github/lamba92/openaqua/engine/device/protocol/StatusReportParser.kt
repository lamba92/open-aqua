package io.github.lamba92.openaqua.engine.device.protocol

import io.github.lamba92.openaqua.model.FeatureState
import io.github.lamba92.openaqua.model.FeatureState.Active
import io.github.lamba92.openaqua.model.device.CoolingChannelKind
import io.github.lamba92.openaqua.model.device.CoolingChannelTelemetry
import io.github.lamba92.openaqua.model.device.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.device.DeviceCapability
import io.github.lamba92.openaqua.model.device.DeviceTelemetry
import io.github.lamba92.openaqua.model.device.FanTelemetryLayout
import io.github.lamba92.openaqua.model.device.SensorChannelKind
import io.github.lamba92.openaqua.model.device.SensorTelemetry
import io.github.lamba92.openaqua.model.device.descriptor
import io.github.lamba92.openaqua.model.device.findCapability
import kotlin.math.round

/**
 * Parses raw HID status reports into high-level [DeviceTelemetry].
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
        val fans =
            when (val fansCap = descriptor.findCapability<DeviceCapability.Fans>()) {
                null -> emptyList()
                else ->
                    fansCap.channels.map { fan ->
                        parseFan(fan.channel, report, fan.statusOffset, fansCap.telemetryLayout, fan.kind)
                    }
            }
        val tempCap = descriptor.findCapability<DeviceCapability.TemperatureSensors>()
        val physicalSensors =
            when (tempCap) {
                null -> emptyList()
                else ->
                    tempCap.sensors.map { sensor ->
                        parseTemp(sensor.channel, report, sensor.statusOffset, tempCap.disconnectedSentinelsWhenReading, sensor.kind)
                    }
            }
        val virtualSensors =
            when (val virtCap = descriptor.findCapability<DeviceCapability.VirtualSensors>()) {
                null -> emptyList()
                else ->
                    parseVirtualSensors(
                        report = report,
                        statusOffset = virtCap.statusOffset,
                        count = virtCap.count,
                        startChannel = tempCap?.sensors?.size ?: 0,
                        disconnectedSentinels = virtCap.disconnectedSentinelsWhenReading,
                    )
            }
        val flowRateDlH =
            when (val flowCap = descriptor.findCapability<DeviceCapability.FlowSensor>()) {
                is DeviceCapability.FlowSensor -> {
                    when (val flowRaw = readU16Be(report, flowCap.statusOffset)) {
                        in 1..65534 -> Active(flowRaw)
                        else -> FeatureState.Disconnected
                    }
                }
                null -> FeatureState.Unsupported
            }
        val display =
            when (descriptor.findCapability<DeviceCapability.Display>()) {
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

    fun parseFan(
        channel: Int,
        report: UByteArray,
        baseOffset: Int,
        layout: FanTelemetryLayout = FanTelemetryLayout.Standard,
        kind: CoolingChannelKind = CoolingChannelKind.FAN,
    ): CoolingChannelTelemetry {
        val rawPwm = readU16Be(report, baseOffset + layout.pwmOffset)
        val rawVoltage = readU16Be(report, baseOffset + layout.voltageOffset)
        val rawCurrent = readU16Be(report, baseOffset + layout.currentOffset)
        val rawPower = readU16Be(report, baseOffset + layout.powerOffset)
        val rawRpm = readU16Be(report, baseOffset + layout.rpmOffset)

        val pwmPercent = round((rawPwm.toDouble() / 100.0) * 10.0) / 10.0
        val voltageVolts = round((rawVoltage.toDouble() / 100.0) * 100.0) / 100.0
        val currentAmps = round((rawCurrent.toDouble() / 1000.0) * 1000.0) / 1000.0
        val powerWatts = round((rawPower.toDouble() / 100.0) * 100.0) / 100.0

        val status = if (baseOffset + layout.statusOffset < report.size) report[baseOffset + layout.statusOffset].toInt() else 0
        val isDisconnected = rawRpm == 0 && ((status and layout.statusDisconnectedMask) != 0 || (currentAmps == 0.0 && powerWatts == 0.0))

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
        disconnectedSentinels: Set<Short>,
        kind: SensorChannelKind = SensorChannelKind.PHYSICAL_NTC,
    ): SensorTelemetry {
        val raw = readI16Be(report, offset)
        return when (raw) {
            in disconnectedSentinels ->
                SensorTelemetry.Disconnected(
                    channel = channel,
                    kind = kind,
                )
            else ->
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
        disconnectedSentinels: Set<Short>,
    ): List<SensorTelemetry> =
        (0 until count).map { i ->
            val channel = startChannel + i
            val offset = statusOffset + (i * 2)
            val raw = readI16Be(report, offset)
            when (raw) {
                in disconnectedSentinels ->
                    SensorTelemetry.Disconnected(
                        channel = channel,
                        kind = SensorChannelKind.VIRTUAL,
                    )
                else ->
                    SensorTelemetry.Connected(
                        channel = channel,
                        kind = SensorChannelKind.VIRTUAL,
                        tempCelsius = round((raw.toDouble() / 100.0) * 10.0) / 10.0,
                    )
            }
        }
}
