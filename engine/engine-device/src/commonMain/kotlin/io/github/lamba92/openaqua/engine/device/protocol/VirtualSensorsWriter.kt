package io.github.lamba92.openaqua.engine.device.protocol

import io.github.lamba92.openaqua.model.device.DeviceCapability
import io.github.lamba92.openaqua.model.device.DeviceDescriptor
import io.github.lamba92.openaqua.model.device.requireCapability
import io.github.lamba92.openaqua.model.device.virtualSensorsChannelRange
import io.github.lamba92.openaqua.model.device.virtualSensorsStartChannel
import kotlin.math.roundToInt

/**
 * Firmware sensor classification codes encoded into the type table of Report 0x04.
 */
enum class VirtualSensorTypeCode(
    val byteValue: UByte,
) {
    DISABLED(0x00u),
    TEMPERATURE(0x03u),
}

/**
 * Encodes software temperature sensor feeds into binary USB Bulk Report 0x04 packets.
 *
 * Transmitted over Endpoint 0x02 OUT to provide real-time software thermal feeds
 * (CPU package, GPU core, ambient ambient probes) to the device's hardware fan curve PID loops.
 */
object VirtualSensorsWriter {
    /**
     * Builds a finalized Report 0x04 buffer for the specified device and slot-to-temperature map.
     *
     * @param descriptor Target device descriptor (must declare [DeviceCapability.VirtualSensors]).
     * @param temperaturesBySlot Map of 0-based virtual sensor slot index (0 until capability.count)
     *                            to temperature in degrees Celsius.
     * @return Fully formatted binary buffer ready for USB transmission, finalized with CRC-16 trailer.
     */
    fun buildReport(
        descriptor: DeviceDescriptor,
        temperaturesBySlot: Map<Int, Double>,
    ): UByteArray {
        val virtCap = descriptor.requireCapability<DeviceCapability.VirtualSensors>()
        val report = UByteArray(virtCap.reportSize)

        report[0] = virtCap.reportId.toUByte()

        // Initialize all slots to disconnected/inactive
        for (slot in 0 until virtCap.maxSlots) {
            val valOffset = virtCap.valuesOffset + (slot * 2)
            report[valOffset] = ((virtCap.disconnectedSentinelWhenWriting.toInt() ushr 8) and 0xFF).toUByte()
            report[valOffset + 1] = (virtCap.disconnectedSentinelWhenWriting.toInt() and 0xFF).toUByte()

            val typeOffset = virtCap.typesOffset + slot
            report[typeOffset] = VirtualSensorTypeCode.DISABLED.byteValue
        }

        // Apply device-specific padding block if required
        when (virtCap) {
            is DeviceCapability.VirtualSensors.Padded67 -> {
                for (i in 0 until virtCap.paddingLength) {
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

        return virtCap.reportSigner.sign(report)
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
            "Channel $channel is not a valid virtual sensor for ${descriptor.name} (valid channels: ${descriptor.virtualSensorsChannelRange})"
        }
        return slot
    }

    /**
     * Builds a finalized Report 0x04 buffer using logical sensor channel indexes.
     *
     * @param descriptor Target device descriptor.
     * @param temperaturesByChannel Map of logical channel index (e.g. 4..19 on OCTO) to temperature in Celsius.
     * @return Fully formatted binary buffer ready for USB transmission, finalized with CRC-16 trailer.
     */
    fun buildReportFromChannels(
        descriptor: DeviceDescriptor,
        temperaturesByChannel: Map<Int, Double>,
    ): UByteArray {
        val temperaturesBySlot =
            temperaturesByChannel.mapKeys { (channel, _) ->
                channelToSlot(descriptor, channel)
            }
        return buildReport(descriptor, temperaturesBySlot)
    }
}
