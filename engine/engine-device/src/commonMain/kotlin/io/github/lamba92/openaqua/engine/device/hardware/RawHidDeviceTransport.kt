package io.github.lamba92.openaqua.engine.device.hardware

import kotlinx.coroutines.flow.Flow

/**
 * Low-level transport interface providing stateless, raw byte I/O operations
 * against physical USB HID devices.
 *
 * Decouples operating system communication primitives (sysfs/hidraw, Win32/WinUSB)
 * from high-level domain models, protocol encoding, and device orchestration.
 */
interface RawHidDeviceTransport {
    /**
     * Enumerates connected USB HID devices recognized by the operating system.
     *
     * @return List of raw HID device endpoints.
     */
    fun scanDevices(): List<RawHidDevice>

    /**
     * Synchronously transfers a Feature Report from the device via USB control endpoint.
     *
     * @param device Target physical device.
     * @param reportId Report ID requested by the protocol.
     * @param reportLength Total byte size of the report buffer including the 1-byte Report ID.
     * @return Raw byte array containing the report payload with Report ID at index 0.
     */
    fun readFeatureReport(
        device: RawHidDevice,
        reportId: Int,
        reportLength: Int,
    ): UByteArray

    /**
     * Synchronously writes a Feature Report to the device via USB control endpoint.
     *
     * @param device Target physical device.
     * @param report Raw report buffer to send (with Report ID at index 0).
     */
    fun writeFeatureReport(
        device: RawHidDevice,
        report: UByteArray,
    )

    /**
     * Synchronously writes an Output Report to the device.
     *
     * @param device Target physical device.
     * @param report Raw output report buffer to send.
     */
    fun writeOutputReport(
        device: RawHidDevice,
        report: UByteArray,
    )

    /**
     * Continuously streams incoming Interrupt Input Reports from the device.
     *
     * @param device Target physical device.
     * @param reportId Expected Report ID.
     * @param reportLength Minimum expected byte length of the report.
     * @return Cold flow of raw input report byte arrays.
     */
    fun streamInputReports(
        device: RawHidDevice,
        reportId: Int,
        reportLength: Int,
    ): Flow<UByteArray>

    /**
     * Transmits a stream of raw USB Bulk reports to the device (e.g. for feeding virtual sensors).
     *
     * Manages low-level interface claiming and releasing across the flow lifecycle.
     *
     * @param device Target physical device.
     * @param reports Flow of raw bulk report buffers to write.
     */
    suspend fun feedBulkReports(
        device: RawHidDevice,
        reports: Flow<UByteArray>,
    )
}
