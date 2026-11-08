package io.github.lamba92.openaqua.model.device

import kotlinx.serialization.Serializable

/**
 * Strategy employed by the device microcontroller to permanently commit
 * runtime configuration (RAM registers) into non-volatile memory (EEPROM/Flash).
 */
@Serializable
sealed interface CommitStrategy {

    /**
     * Devices that require a secondary HID report to be transmitted after the primary control report
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
     * Devices where transmitting the primary control report directly commits configuration to hardware memory
     * without needing a secondary commit report (e.g. Aquaero, Aquastream XT, Poweradjust 3).
     */
    @Serializable
    data object Direct : CommitStrategy

    /**
     * Read-only devices that do not support configuration writes (e.g. High Flow USB).
     */
    @Serializable
    data object None : CommitStrategy
}

/**
 * Low-level USB HID protocol parameters and report layouts for an Aquacomputer device.
 *
 * Encapsulates packet sizes, report IDs, firmware version offset, and commit strategy.
 *
 * @property statusReportLength Total byte length of the HID status report received with live telemetry.
 * @property statusReportId HID Report ID for reading status/telemetry (defaults to `0x01`).
 * @property ctrlReportLength Total byte length of the HID control report sent to configure the device (0 if read-only).
 * @property ctrlReportId HID Report ID for writing configuration (defaults to `0x03`).
 * @property firmwareVersionOffset Byte offset in the HID status report containing the 16-bit firmware version.
 * @property commitStrategy How configuration writes are persisted to non-volatile flash memory.
 */
@Serializable
data class DeviceProtocol(
    val statusReportLength: Int,
    val statusReportId: Int = 0x01,
    val ctrlReportLength: Int = 0,
    val ctrlReportId: Int = 0x03,
    val firmwareVersionOffset: Int = 0x0D,
    val commitStrategy: CommitStrategy = CommitStrategy.SecondaryReport(),
)
