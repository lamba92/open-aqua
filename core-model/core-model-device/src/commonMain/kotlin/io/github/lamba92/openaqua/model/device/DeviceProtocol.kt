@file:OptIn(ExperimentalUnsignedTypes::class)

package io.github.lamba92.openaqua.model.device

import kotlinx.serialization.Serializable

/**
 * Signs raw HID report buffers in-place by computing and writing the checksum/signature
 * into the trailer (the last [signatureSize] bytes of the buffer).
 */
@Serializable
sealed interface ReportSigner {
    /**
     * Number of checksum/signature bytes reserved at the end of the report buffer.
     */
    val signatureSize: Int

    /**
     * Computes the checksum over [report] payload and writes the signature trailer
     * in-place into its final [signatureSize] bytes.
     *
     * In USB HID communication with Aquacomputer hardware, report buffers are allocated
     * to their full wire length up front (or read directly from hardware in a Read-Modify-Write
     * cycle). The signature trailer occupies the final [signatureSize] bytes.
     * This method computes the checksum over the preceding payload bytes and overwrites
     * the trailing bytes in-place.
     *
     * @param report Full-length report buffer whose last [signatureSize] bytes will receive the signature.
     * @return The same [report] instance for fluent chaining.
     */
    fun sign(report: UByteArray): UByteArray

    /**
     * Standard USB CRC-16 checksum trailer used on modern Aquacomputer devices.
     * Computes CRC-16/USB over bytes 1 until end of payload and appends the 16-bit Big-Endian result (2 bytes).
     */
    @Serializable
    data object Crc16Usb : ReportSigner {
        override val signatureSize: Int get() = 2

        override fun sign(report: UByteArray): UByteArray {
            require(report.size >= signatureSize + 1) {
                "Report size (${report.size}) must be at least signatureSize + 1 (${signatureSize + 1})"
            }
            val crcOffset = 1
            val crcLen = report.size - signatureSize - 1
            var crc = 0xFFFF
            val end = crcOffset + crcLen
            for (i in crcOffset until end) {
                crc = crc xor (report[i].toInt() and 0xFF)
                repeat(8) {
                    crc =
                        when {
                            (crc and 1) != 0 -> (crc ushr 1) xor 0xA001
                            else -> crc ushr 1
                        }
                }
            }
            val finalCrc = (crc xor 0xFFFF).toUShort()
            val trailerOffset = report.size - signatureSize
            report[trailerOffset] = ((finalCrc.toInt() ushr 8) and 0xFF).toUByte()
            report[trailerOffset + 1] = (finalCrc.toInt() and 0xFF).toUByte()
            return report
        }
    }
}

/**
 * Strategy employed by the device microcontroller to permanently commit
 * runtime configuration (RAM registers) into non-volatile memory (EEPROM/Flash).
 */
@Serializable
sealed interface CommitStrategy {
    /**
     * Devices that require a secondary HID report to be transmitted after the primary configuration report
     * in order to trigger the firmware EEPROM/Flash write routine (e.g. OCTO, QUADRO, D5 NEXT).
     *
     * @property report Raw bytes of the commit report (including Report ID and CRC checksum).
     */
    @Serializable
    data class SecondaryReport(
        val report: List<UByte> = defaultCommitReport,
    ) : CommitStrategy {
        companion object {
            /**
             * Standard 11-byte commit sequence used by modern Aquacomputer controllers:
             * Report ID 0x02, command 0x00000002 (Commit), 4 padding bytes, and CRC-16-USB 0x34C6.
             */
            val defaultCommitReport: List<UByte> =
                listOf(
                    0x02u.toUByte(),
                    0x00u.toUByte(),
                    0x00u.toUByte(),
                    0x00u.toUByte(),
                    0x02u.toUByte(),
                    0x00u.toUByte(),
                    0x00u.toUByte(),
                    0x00u.toUByte(),
                    0x00u.toUByte(),
                    0x34u.toUByte(),
                    0xC6u.toUByte(),
                )
        }
    }

    /**
     * Devices where transmitting the primary configuration report directly commits configuration to hardware memory
     * without needing a secondary commit report (e.g. Aquaero, Aquastream XT, Poweradjust 3).
     */
    @Serializable
    data object Direct : CommitStrategy
}

/**
 * Low-level USB HID protocol parameters and report layouts for an Aquacomputer device.
 */
@Serializable
sealed interface DeviceProtocol {
    /** Total byte length of the HID status report received with live telemetry. */
    val statusReportLength: Int

    /** HID Report ID for reading status/telemetry (defaults to `0x01`). */
    val statusReportId: Int

    /** Byte offset in the HID status report containing the 16-bit firmware version. */
    val firmwareVersionOffset: Int

    /**
     * Protocol specification for configurable devices that accept configuration writes.
     *
     * @property statusReportLength Total byte length of the HID status report received with live telemetry.
     * @property statusReportId HID Report ID for reading status/telemetry (defaults to `0x01`).
     * @property configReportLength Total byte length of the HID configuration report sent to configure the device.
     * @property configReportId HID Report ID for writing configuration (defaults to `0x03`).
     * @property firmwareVersionOffset Byte offset in the HID status report containing the 16-bit firmware version.
     * @property commitStrategy How configuration writes are persisted to non-volatile flash memory.
     * @property reportSigner The checksum/signing strategy used to finalize configuration reports.
     */
    @Serializable
    data class Flashable(
        override val statusReportLength: Int,
        override val statusReportId: Int = 0x01,
        val configReportLength: Int,
        val configReportId: Int = 0x03,
        override val firmwareVersionOffset: Int = 0x0D,
        val commitStrategy: CommitStrategy = CommitStrategy.SecondaryReport(),
        val reportSigner: ReportSigner = ReportSigner.Crc16Usb,
    ) : DeviceProtocol

    /**
     * Protocol specification for read-only telemetry devices (e.g. High Flow USB).
     *
     * @property statusReportLength Total byte length of the HID status report received with live telemetry.
     * @property statusReportId HID Report ID for reading status/telemetry (defaults to `0x01`).
     * @property firmwareVersionOffset Byte offset in the HID status report containing the 16-bit firmware version.
     */
    @Serializable
    data class ReadOnly(
        override val statusReportLength: Int,
        override val statusReportId: Int = 0x01,
        override val firmwareVersionOffset: Int = 0x0D,
    ) : DeviceProtocol
}

/**
 * Returns this descriptor's [DeviceProtocol.Flashable] protocol or throws [IllegalStateException] if the device is read-only.
 */
val DeviceDescriptor.flashableProtocol: DeviceProtocol.Flashable
    get() =
        protocol as? DeviceProtocol.Flashable
            ?: error("Device $name is read-only and does not support configuration writes")
