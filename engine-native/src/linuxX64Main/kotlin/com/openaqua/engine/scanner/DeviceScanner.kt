package com.openaqua.engine.scanner

import com.openaqua.engine.util.listDirectory
import com.openaqua.engine.util.readFile
import com.openaqua.model.DetectedHardwareDevice
import com.openaqua.model.DeviceRegistry

object DeviceScanner {
    fun scanDevices(): List<DetectedHardwareDevice> {
        val hidrawDirs = listDirectory("/sys/class/hidraw")
        val devices = mutableListOf<DetectedHardwareDevice>()

        for (entry in hidrawDirs.sorted()) {
            val ueventPath = "/sys/class/hidraw/$entry/device/uevent"
            val content = readFile(ueventPath) ?: continue

            var vendorId: Int? = null
            var productId: Int? = null
            var hidUniq: String? = null

            for (line in content.lines()) {
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("HID_ID=") -> {
                        val parts = trimmed.substringAfter("HID_ID=").split(":")
                        if (parts.size >= 3) {
                            vendorId = parts[1].toIntOrNull(16)
                            productId = parts[2].toIntOrNull(16)
                        }
                    }
                    trimmed.startsWith("HID_UNIQ=") -> {
                        hidUniq = trimmed.substringAfter("HID_UNIQ=").trim().ifEmpty { null }
                    }
                }
            }

            if (vendorId != DeviceRegistry.AQUACOMPUTER_VENDOR_ID || productId == null) {
                continue
            }

            val descriptor = DeviceRegistry.findByProductId(productId) ?: continue
            val id = hidUniq ?: entry

            val hwmonSubdir = "/sys/class/hidraw/$entry/device/hwmon"
            val hwmonEntries = listDirectory(hwmonSubdir)
            val hwmonName = hwmonEntries.firstOrNull { it.startsWith("hwmon") }
            val hwmonPath = hwmonName?.let { "$hwmonSubdir/$it" }

            devices.add(
                DetectedHardwareDevice(
                    id = id,
                    type = descriptor.type,
                    name = descriptor.name,
                    serial = hidUniq,
                    firmwareVersion = null,
                    hidrawPath = "/dev/$entry",
                    hwmonPath = hwmonPath,
                ),
            )
        }

        return devices
    }
}
