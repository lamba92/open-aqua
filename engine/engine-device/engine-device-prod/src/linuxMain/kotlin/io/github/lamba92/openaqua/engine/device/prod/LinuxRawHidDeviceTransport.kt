@file:OptIn(ExperimentalForeignApi::class)

package io.github.lamba92.openaqua.engine.device.prod

import io.github.lamba92.openaqua.engine.device.hardware.RawHidDevice
import io.github.lamba92.openaqua.engine.device.hardware.RawHidDeviceTransport
import io.github.lamba92.openaqua.engine.util.listDirectory
import io.github.lamba92.openaqua.engine.util.readTextOrNull
import io.github.lamba92.openaqua.model.device.DeviceRegistry
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.plus
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.io.files.Path
import platform.posix.EACCES
import platform.posix.EPERM
import platform.posix.O_NONBLOCK
import platform.posix.O_RDWR
import platform.posix.POLLIN
import platform.posix.close
import platform.posix.errno
import platform.posix.free
import platform.posix.ioctl
import platform.posix.open
import platform.posix.poll
import platform.posix.pollfd
import platform.posix.read
import platform.posix.realpath
import platform.posix.usleep

private const val USBDEVFS_CLAIMINTERFACE: ULong = 0x8004550FUL
private const val USBDEVFS_RELEASEINTERFACE: ULong = 0x80045510UL
private const val USBDEVFS_BULK: ULong = 0xC0185502UL
private const val VIRTUAL_SENSORS_BULK_OUT_EP: UInt = 0x02u

object LinuxRawHidDeviceTransport : RawHidDeviceTransport {
    override fun scanDevices(): List<RawHidDevice> {
        val scanned = mutableListOf<RawHidDevice>()
        val hidrawDirs = Path(SYSFS_HIDRAW_DIR).listDirectory()

        for (entry in hidrawDirs.sortedBy { it.name }) {
            val entryName = entry.name
            val ueventPath = hidrawUeventPath(entryName)
            val content = Path(ueventPath).readTextOrNull() ?: continue

            var vendorId: Int? = null
            var productId: Int? = null
            var hidUniq: String? = null

            for (line in content.lines()) {
                val trimmed = line.trim()
                when {
                    trimmed.startsWith(UEVENT_HID_ID_PREFIX) -> {
                        val parts = trimmed.substringAfter(UEVENT_HID_ID_PREFIX).split(UEVENT_DELIMITER)
                        if (parts.size >= 3) {
                            vendorId = parts[1].toIntOrNull(16)
                            productId = parts[2].toIntOrNull(16)
                        }
                    }
                    trimmed.startsWith(UEVENT_HID_UNIQ_PREFIX) -> {
                        hidUniq = trimmed.substringAfter(UEVENT_HID_UNIQ_PREFIX).trim().ifEmpty { null }
                    }
                }
            }

            if (vendorId != DeviceRegistry.AQUACOMPUTER_VENDOR_ID || productId == null) continue
            if (DeviceRegistry.findByProductId(productId) == null) continue

            val devPath = hidrawDevPath(entryName)
            if (scanned.none { it.id == devPath }) {
                scanned.add(
                    RawHidDevice(
                        id = devPath,
                        vendorId = vendorId,
                        productId = productId,
                        serial = hidUniq,
                    ),
                )
            }
        }
        return scanned
    }

    override fun readFeatureReport(
        device: RawHidDevice,
        reportId: Int,
        reportLength: Int,
    ): UByteArray {
        val totalLen = reportLength
        val fd = open(device.id, O_RDWR)
        check(fd >= 0) { "Cannot open ${device.id} for reading feature report" }

        try {
            memScoped {
                val buf = allocArray<UByteVar>(totalLen)
                buf[0] = reportId.toUByte()

                val cmd = hidiocgfeature(totalLen)
                val ret = ioctl(fd, cmd, buf)
                check(ret >= 0) { "ioctl HIDIOCGFEATURE failed on ${device.id} (totalLen=$totalLen)" }

                val result = UByteArray(totalLen)
                for (i in 0 until totalLen) {
                    result[i] = buf[i]
                }
                return result
            }
        } finally {
            close(fd)
        }
    }

    override fun writeFeatureReport(
        device: RawHidDevice,
        report: UByteArray,
    ) {
        val totalLen = report.size
        val fd = open(device.id, O_RDWR)
        check(fd >= 0) { "Cannot open ${device.id} for writing feature report" }

        try {
            memScoped {
                val buf = allocArray<UByteVar>(totalLen)
                for (i in 0 until totalLen) {
                    buf[i] = report[i]
                }

                usleep(200_000u)

                val cmd = hidiocsfeature(totalLen)
                val ret = ioctl(fd, cmd, buf)
                check(ret >= 0) { "ioctl HIDIOCSFEATURE failed on ${device.id}" }
            }
        } finally {
            close(fd)
        }
    }

    override fun writeOutputReport(
        device: RawHidDevice,
        report: UByteArray,
    ) {
        // Linux hidraw does not expose an interrupt OUT endpoint for Aquacomputer devices,
        // so secondary/commit reports are sent via HIDIOCSFEATURE.
        val totalLen = report.size
        val fd = open(device.id, O_RDWR)
        check(fd >= 0) { "Cannot open ${device.id} for writing output report" }

        try {
            memScoped {
                val buf = allocArray<UByteVar>(totalLen)
                for (i in 0 until totalLen) {
                    buf[i] = report[i]
                }

                val cmd = hidiocsfeature(totalLen)
                val ret = ioctl(fd, cmd, buf)
                check(ret >= 0) { "ioctl HIDIOCSFEATURE (output report) failed on ${device.id}" }
            }
        } finally {
            close(fd)
        }
    }

    override fun streamInputReports(
        device: RawHidDevice,
        reportId: Int,
        reportLength: Int,
    ): Flow<UByteArray> =
        flow {
            val fd = open(device.id, O_RDWR or O_NONBLOCK)
            check(fd >= 0) { "Cannot open ${device.id} for streaming input reports" }

            try {
                memScoped {
                    val pfd = alloc<pollfd>()
                    pfd.fd = fd
                    pfd.events = POLLIN.toShort()

                    val buf = allocArray<UByteVar>(512)

                    while (currentCoroutineContext().isActive) {
                        val pollRet = poll(pfd.ptr, 1u, DEFAULT_HID_REPORT_TIMEOUT_MS)
                        if (pollRet > 0 && (pfd.revents.toInt() and POLLIN) != 0) {
                            val bytesRead = read(fd, buf, 512u)
                            if (bytesRead >= reportLength.toLong() && buf[0].toInt() == reportId) {
                                val raw = UByteArray(bytesRead.toInt()) { buf[it] }
                                emit(raw)
                            }
                        }
                    }
                }
            } finally {
                close(fd)
            }
        }

    override suspend fun feedBulkReports(
        device: RawHidDevice,
        reports: Flow<UByteArray>,
    ) {
        val usbPath =
            resolveUsbDevicePath(device.id)
                ?: error("Cannot determine USB device path for ${device.id}")

        val fd = open(usbPath, O_RDWR)
        if (fd < 0) {
            val err = errno
            if (err == EACCES || err == EPERM) {
                error(
                    "Cannot open $usbPath for virtual sensors feeding (Permission denied). " +
                        "Aqua Computer devices require USB bulk write permissions on Interface 0. " +
                        "Add this udev rule to /etc/udev/rules.d/70-open-aqua.rules:\n" +
                        "SUBSYSTEM==\"usb\", ATTRS{idVendor}==\"0c70\", TAG+=\"uaccess\"",
                )
            }
            error("Cannot open $usbPath for virtual sensors feeding (errno $err)")
        }

        try {
            memScoped {
                val iface = alloc<UIntVar>()
                iface.value = 0u // Interface 0 (Vendor Specific)
                val claimRet = ioctl(fd, USBDEVFS_CLAIMINTERFACE, iface.ptr)
                if (claimRet < 0) {
                    val err = errno
                    error("ioctl USBDEVFS_CLAIMINTERFACE failed on $usbPath interface 0 (errno $err)")
                }
            }

            reports.collect { reportData ->
                memScoped {
                    val transfer = allocArray<UByteVar>(24)
                    val uintView = transfer.reinterpret<UIntVar>()
                    uintView[0] = VIRTUAL_SENSORS_BULK_OUT_EP
                    uintView[1] = reportData.size.toUInt()
                    uintView[2] = 2000u // 2000 ms timeout

                    val buf = allocArray<UByteVar>(reportData.size)
                    for (i in reportData.indices) {
                        buf[i] = reportData[i]
                    }

                    val dataPtr = (transfer + 16)!!.reinterpret<COpaquePointerVar>().pointed
                    dataPtr.value = buf

                    val bulkRet = ioctl(fd, USBDEVFS_BULK, transfer)
                    if (bulkRet < 0) {
                        val err = errno
                        error("ioctl USBDEVFS_BULK failed on ${device.id} EP 0x02 OUT (errno $err)")
                    }
                }
            }
        } finally {
            memScoped {
                val iface = alloc<UIntVar>()
                iface.value = 0u
                ioctl(fd, USBDEVFS_RELEASEINTERFACE, iface.ptr)
            }
            close(fd)
        }
    }
}

private fun resolveUsbDevicePath(hidrawPath: String): String? {
    val hidrawName = hidrawPath.substringAfterLast('/')
    val deviceLink = Path("/sys/class/hidraw/$hidrawName/device")
    val realPath =
        realpath(deviceLink.toString(), null)?.let { ptr ->
            val s = ptr.toKString()
            free(ptr)
            s
        } ?: return null

    var cur = realPath
    while (cur.isNotBlank() && cur != "/") {
        val busnum = Path("$cur/busnum").readTextOrNull()?.trim()?.toIntOrNull()
        val devnum = Path("$cur/devnum").readTextOrNull()?.trim()?.toIntOrNull()
        if (busnum != null && devnum != null) {
            val bStr = busnum.toString().padStart(3, '0')
            val dStr = devnum.toString().padStart(3, '0')
            return "/dev/bus/usb/$bStr/$dStr"
        }
        val next = cur.substringBeforeLast('/', "")
        if (next == cur) break
        cur = next
    }
    return null
}
