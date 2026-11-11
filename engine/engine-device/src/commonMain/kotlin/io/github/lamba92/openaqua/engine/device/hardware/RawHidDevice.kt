package io.github.lamba92.openaqua.engine.device.hardware

import kotlinx.serialization.Serializable

/**
 * Pure data model representing a physical USB HID device node discovered by the OS.
 * Contains only hardware identity and properties without any attached behavior or handles.
 *
 * @property id Platform-specific unique address or identifier for the device (e.g. hidraw path or device ID).
 * @property vendorId USB Vendor ID (e.g. `0x0C70` for Aquacomputer).
 * @property productId USB Product ID (e.g. `0xF011` for OCTO).
 * @property serial Optional factory serial number string read from USB descriptor.
 */
@Serializable
data class RawHidDevice(
    val id: String,
    val vendorId: Int,
    val productId: Int,
    val serial: String? = null,
)
