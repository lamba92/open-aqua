package io.github.lamba92.openaqua.engine.device.protocol

import io.github.lamba92.openaqua.model.device.DeviceCapability
import io.github.lamba92.openaqua.model.device.DeviceDescriptor
import io.github.lamba92.openaqua.model.device.requireCapability
import io.github.lamba92.openaqua.model.device.virtualSensorsStartChannel
import kotlin.math.roundToInt

/**
 * Firmware sensor classification codes encoded into the type table of Report 0x04.
 *
 * @property byteValue Raw byte written to device memory.
 */
enum class VirtualSensorTypeCode(val byteValue: UByte) {
    /** Slot is unconfigured / inactive. */
    DISABLED(0x00u),

    /** Slot represents a live temperature in centidegrees Celsius. */
    TEMPERATURE(0x03u),
}

/**
 * Encodes software temperature sensor feeds into binary USB Bulk Report 0x04 packets.
 *
 * Transmitted over Endpoint 0x02 OUT to provide real-time software thermal feeds
 * (e.g. CPU package, GPU hotspot) to hardware fan curve engines.
 */
object VirtualSensorsWriter {
    /** 16-bit signed integer sentinel value (32767) indicating a disconnected or inactive sensor. */
    const val DISCONNECTED_TEMP_RAW: Short = 0x7FFF

    /**
     * Builds a finalized Report 0x04 buffer for the specified device and slot-to-temperature map.
     *
     * @param descriptor Target device descriptor with [DeviceCapability.VirtualSensors].
     * @param temperaturesBySlot Map of slot index (0 until capability.count) to temperature in Celsius.
     * @return Finalized, CRC-16 checksummed byte array ready for USB bulk transfer.
     */
    fun buildReport(
        descriptor: DeviceDescriptor,
        temperaturesBySlot: Map<Int, Double>,
    ): UByteArray {
        val virtCap = descriptor.requireCapability<DeviceCapability.VirtualSensors>()
        val report = UByteArray(virtCap.reportSize)
        report[0] = virtCap.reportId.toUByte()

        // Initialize all slots in the packet as disconnected and disabled
        for (slot in 0 until virtCap.maxSlots) {
            val valOffset = virtCap.valuesOffset + (slot * 2)
            report[valOffset] = ((DISCONNECTED_TEMP_RAW.toInt() ushr 8) and 0xFF).toUByte()
            report[valOffset + 1] = (DISCONNECTED_TEMP_RAW.toInt() and 0xFF).toUByte()

            val typeOffset = virtCap.typesOffset + slot
            report[typeOffset] = VirtualSensorTypeCode.DISABLED.byteValue
        }

        // Apply device-specific padding if the format requires it
        when (virtCap) {
            is DeviceCapability.VirtualSensors.Padded67 -> {
                for (i in 0 until 16) {
                    report[virtCap.paddingOffset + i] = virtCap.paddingByte
                }
            }
            is DeviceCapability.VirtualSensors.Standard51 -> Unit
        }

        // Encode active temperature readings
        temperaturesBySlot.forEach { (slot, tempCelsius) ->
            require(slot in 0 until virtCap.count) {
                "Virtual sensor slot $slot is out of bounds for ${descriptor.name} (max ${virtCap.count})"
            }
            val valOffset = virtCap.valuesOffset + (slot * 2)
            val rawTemp = (tempCelsius * 100.0).roundToInt().coerceIn(-5000, 15000).toShort()
            report[valOffset] = ((rawTemp.toInt() ushr 8) and 0xFF).toUByte()
            report[valOffset + 1] = (rawTemp.toInt() and 0xFF).toUByte()

            val typeOffset = virtCap.typesOffset + slot
            report[typeOffset] = VirtualSensorTypeCode.TEMPERATURE.byteValue
        }

        ConfigWriter.finalizeReport(report, virtCap.reportSize)
        return report
    }

    /**
     * Converts a user-visible logical temperature channel index into the 0-based virtual sensor slot index.
     *
     * @param descriptor Target device descriptor.
     * @param channel Logical sensor channel index.
     * @return 0-based slot index (0 until capability.count).
     * @throws IllegalArgumentException If [channel] does not fall in the device's virtual sensors range.
     */
    fun channelToSlot(
        descriptor: DeviceDescriptor,
        channel: Int,
    ): Int {
        val virtCap = descriptor.requireCapability<DeviceCapability.VirtualSensors>()
        val startChannel = descriptor.virtualSensorsStartChannel
        val slot = channel - startChannel
        require(slot in 0 until virtCap.count) {
            "Channel $channel is not a valid virtual sensor for ${descriptor.name} (valid channels: $startChannel..${startChannel + virtCap.count - 1})"
        }
        return slot
    }

    /**
     * Builds a finalized Report 0x04 buffer using unified logical channel indices.
     *
     * @param descriptor Target device descriptor.
     * @param temperaturesByChannel Map of logical channel index to temperature in Celsius.
     * @return Finalized, CRC-16 checksummed byte array ready for USB bulk transfer.
     */
    fun buildReportFromChannels(
        descriptor: DeviceDescriptor,
        temperaturesByChannel: Map<Int, Double>,
    ): UByteArray {
        val slotMap = temperaturesByChannel.mapKeys { (ch, _) -> channelToSlot(descriptor, ch) }
        return buildReport(descriptor, slotMap)
    }
}
