package io.github.lamba92.openaqua.engine.device.protocol

import io.github.lamba92.openaqua.model.device.DeviceDescriptor
import io.github.lamba92.openaqua.model.device.virtualSensorsStartChannel
import kotlin.math.roundToInt

object VirtualSensorsWriter {
    const val REPORT_ID = 0x04
    const val REPORT_SIZE = 0x43 // 67 bytes
    const val VALUES_OFFSET = 0x01
    const val TYPES_OFFSET = 0x21
    const val UNKNOWN_OFFSET = 0x31
    const val UNKNOWN_BYTE: UByte = 0x64u
    const val DISABLED_VALUE: UShort = 0x7FFFu
    const val TYPE_DISABLED: UByte = 0u
    const val TYPE_TEMPERATURE: UByte = 3u

    fun buildReport(
        descriptor: DeviceDescriptor,
        temperaturesBySlot: Map<Int, Double>,
    ): UByteArray {
        val reportSize = descriptor.virtualSensorsReportSize
        val report = UByteArray(reportSize)
        report[0] = REPORT_ID.toUByte()

        // Initialize all 16 slots as disabled
        for (i in 0 until 16) {
            val valOffset = VALUES_OFFSET + (i * 2)
            if (valOffset + 1 < reportSize) {
                report[valOffset] = ((DISABLED_VALUE.toInt() ushr 8) and 0xFF).toUByte()
                report[valOffset + 1] = (DISABLED_VALUE.toInt() and 0xFF).toUByte()
            }
            if (TYPES_OFFSET + i < reportSize - 2) {
                report[TYPES_OFFSET + i] = TYPE_DISABLED
            }
            if (reportSize >= 0x43 && UNKNOWN_OFFSET + i < reportSize - 2) {
                report[UNKNOWN_OFFSET + i] = UNKNOWN_BYTE
            }
        }

        temperaturesBySlot.forEach { (slot, tempCelsius) ->
            require(slot in 0 until descriptor.virtualSensorsCount) {
                "Virtual sensor slot $slot is out of bounds for ${descriptor.name} (max ${descriptor.virtualSensorsCount})"
            }
            val valOffset = VALUES_OFFSET + (slot * 2)
            val rawTemp = (tempCelsius * 100.0).roundToInt().coerceIn(-5000, 15000).toShort()
            report[valOffset] = ((rawTemp.toInt() ushr 8) and 0xFF).toUByte()
            report[valOffset + 1] = (rawTemp.toInt() and 0xFF).toUByte()
            if (TYPES_OFFSET + slot < reportSize - 2) {
                report[TYPES_OFFSET + slot] = TYPE_TEMPERATURE
            }
        }

        ConfigWriter.finalizeReport(report, reportSize)
        return report
    }

    fun channelToSlot(
        descriptor: DeviceDescriptor,
        channel: Int,
    ): Int {
        val startChannel = descriptor.virtualSensorsStartChannel
        val slot = channel - startChannel
        require(slot in 0 until descriptor.virtualSensorsCount) {
            "Channel $channel is not a valid virtual sensor for ${descriptor.name} (valid channels: $startChannel..${startChannel + descriptor.virtualSensorsCount - 1})"
        }
        return slot
    }

    fun buildReportFromChannels(
        descriptor: DeviceDescriptor,
        temperaturesByChannel: Map<Int, Double>,
    ): UByteArray {
        val slotMap = temperaturesByChannel.mapKeys { (ch, _) -> channelToSlot(descriptor, ch) }
        return buildReport(descriptor, slotMap)
    }
}
