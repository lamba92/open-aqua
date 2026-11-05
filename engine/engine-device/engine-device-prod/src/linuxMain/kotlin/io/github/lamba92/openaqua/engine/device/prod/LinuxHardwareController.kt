@file:OptIn(ExperimentalForeignApi::class)

package io.github.lamba92.openaqua.engine.device.prod

import io.github.lamba92.openaqua.engine.device.hardware.HardwareController
import io.github.lamba92.openaqua.engine.device.protocol.ConfigParser
import io.github.lamba92.openaqua.engine.device.protocol.ConfigWriter
import io.github.lamba92.openaqua.engine.device.protocol.StatusReportParser
import io.github.lamba92.openaqua.engine.device.protocol.VirtualSensorsWriter
import io.github.lamba92.openaqua.engine.util.listDirectory
import io.github.lamba92.openaqua.engine.util.readTextOrNull
import io.github.lamba92.openaqua.model.device.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.device.DeviceAlias
import io.github.lamba92.openaqua.model.device.DeviceConfig
import io.github.lamba92.openaqua.model.device.DeviceDescriptor
import io.github.lamba92.openaqua.model.device.DeviceId
import io.github.lamba92.openaqua.model.device.DeviceRegistry
import io.github.lamba92.openaqua.model.device.DeviceTelemetry
import io.github.lamba92.openaqua.model.device.FlashConfigRequest
import io.github.lamba92.openaqua.model.device.validateRequestMatchesDevice
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

private data class ScannedHardwareNode(
    val id: DeviceId,
    val descriptor: DeviceDescriptor,
    val serial: String?,
    val hidrawPath: String,
)

object LinuxHardwareController : HardwareController {
    private fun getScannedNode(device: DetectedHardwareDevice): ScannedHardwareNode =
        scanNodes().firstOrNull { it.id == device.id }
            ?: error("Hardware device '${device.id}' (${device.name}) not found")

    private fun scanNodes(): List<ScannedHardwareNode> {
        val scanned = mutableListOf<ScannedHardwareNode>()
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

            if (vendorId != DeviceRegistry.AQUACOMPUTER_VENDOR_ID || productId == null) {
                continue
            }

            val descriptor = DeviceRegistry.findByProductId(productId) ?: continue
            val id = DeviceId.from(hidUniq ?: entryName)

            scanned.add(
                ScannedHardwareNode(
                    id = id,
                    descriptor = descriptor,
                    serial = hidUniq,
                    hidrawPath = hidrawDevPath(entryName),
                ),
            )
        }
        return scanned
    }

    override fun scanDevices(): List<DetectedHardwareDevice> =
        scanNodes()
            .sortedWith(
                compareBy<ScannedHardwareNode> { it.descriptor.type.name }
                    .thenBy { it.serial ?: it.hidrawPath },
            ).groupBy { it.descriptor.type }
            .flatMap { (type, nodes) ->
                val typePrefix = type.name.lowercase().replace('_', '-')
                nodes.mapIndexed { index, node ->
                    val fwVersion =
                        node.descriptor.firmwareVersionOffset?.let { offset ->
                            readFirmwareVersion(node.hidrawPath, offset)
                        }
                    DetectedHardwareDevice(
                        id = node.id,
                        alias = DeviceAlias.from("$typePrefix-${index + 1}"),
                        type = node.descriptor.type,
                        name = node.descriptor.name,
                        serial = node.serial,
                        firmwareVersion = fwVersion,
                    )
                }
            }

    override fun streamTelemetry(device: DetectedHardwareDevice): Flow<DeviceTelemetry> =
        flow {
            val descriptor =
                DeviceRegistry.ALL.firstOrNull { it.type == device.type }
                    ?: error("Unsupported device type: ${device.type}")

            val node = getScannedNode(device)
            val fd = open(node.hidrawPath, O_RDWR or O_NONBLOCK)
            check(fd >= 0) { "Cannot open ${node.hidrawPath} for reading" }

            try {
                memScoped {
                    val pfd = alloc<pollfd>()
                    pfd.fd = fd
                    pfd.events = POLLIN.toShort()

                    val bufSize = 512
                    val buf = allocArray<UByteVar>(bufSize)

                    while (currentCoroutineContext().isActive) {
                        val pollRet = poll(pfd.ptr, 1u, DEFAULT_HID_REPORT_TIMEOUT_MS)
                        if (pollRet > 0) {
                            val bytesRead = read(fd, buf, bufSize.toULong())
                            if (bytesRead >= descriptor.statusReportLength.toLong() && buf[0].toInt() == HID_STATUS_REPORT_ID) {
                                val report = UByteArray(bytesRead.toInt()) { buf[it] }
                                val telemetry = StatusReportParser.parse(device, report)
                                emit(telemetry)
                            }
                        }
                    }
                }
            } finally {
                close(fd)
            }
        }

    override fun readConfig(device: DetectedHardwareDevice): DeviceConfig {
        val descriptor =
            DeviceRegistry.ALL.firstOrNull { it.type == device.type }
                ?: error("Unsupported device type: ${device.type}")

        val node = getScannedNode(device)
        val report = readFeatureReport(node.hidrawPath, 0x03u, descriptor.ctrlReportLength)

        return ConfigParser.parseConfig(device, descriptor, report)
    }

    override fun flashConfig(
        device: DetectedHardwareDevice,
        request: FlashConfigRequest,
    ): DeviceConfig {
        val descriptor =
            DeviceRegistry.ALL.firstOrNull { it.type == device.type }
                ?: error("Unsupported device type: ${device.type}")

        descriptor.validateRequestMatchesDevice(request)

        val node = getScannedNode(device)
        val report = readFeatureReport(node.hidrawPath, 0x03u, descriptor.ctrlReportLength)

        ConfigWriter.applyFlashConfig(report, descriptor, request)
        ConfigWriter.finalizeReport(report, descriptor.ctrlReportLength)

        writeFeatureReport(node.hidrawPath, report)
        writeFeatureReport(node.hidrawPath, ConfigWriter.SECONDARY_COMMIT_REPORT)
        usleep(250_000u)

        val readback = readFeatureReport(node.hidrawPath, 0x03u, descriptor.ctrlReportLength)
        val readbackReport = ConfigParser.parseConfig(device, descriptor, readback)

        ConfigWriter.verifyFlashConfig(expected = request, actual = readbackReport)

        return readbackReport
    }

    override fun feedVirtualSensors(
        device: DetectedHardwareDevice,
        sensors: Map<Int, Double>,
    ) {
        val descriptor =
            DeviceRegistry.ALL.firstOrNull { it.type == device.type }
                ?: error("Unsupported device type: ${device.type}")

        check(descriptor.virtualSensorsCount > 0) {
            "${device.name} does not support virtual sensors"
        }

        val report = VirtualSensorsWriter.buildReportFromChannels(descriptor, sensors)
        val node = getScannedNode(device)

        writeVirtualSensorsBulkReport(node.hidrawPath, report)
    }
}

private fun hidiocgfeature(len: Int): ULong = 0xC0000000UL or ((len.toULong() and 0x3FFFUL) shl 16) or 0x4807UL

private fun hidiocsfeature(len: Int): ULong = 0xC0000000UL or ((len.toULong() and 0x3FFFUL) shl 16) or 0x4806UL

private fun readFeatureReport(
    hidrawPath: String,
    reportId: UByte,
    reportPayloadLength: Int,
): UByteArray {
    val totalLen = reportPayloadLength + 1
    val fd = open(hidrawPath, O_RDWR)
    check(fd >= 0) { "Cannot open $hidrawPath for reading" }

    try {
        memScoped {
            val buf = allocArray<UByteVar>(totalLen)
            buf[0] = reportId

            val cmd = hidiocgfeature(totalLen)
            val ret = ioctl(fd, cmd, buf)
            check(ret >= 0) { "ioctl HIDIOCGFEATURE failed on $hidrawPath" }

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

private fun readFirmwareVersion(
    hidrawPath: String,
    firmwareOffset: Int,
    timeoutMs: Int = DEFAULT_HID_REPORT_TIMEOUT_MS,
): Int? {
    val fd = open(hidrawPath, O_RDWR or O_NONBLOCK)
    if (fd < 0) return null

    try {
        memScoped {
            val pfd = alloc<pollfd>()
            pfd.fd = fd
            pfd.events = POLLIN.toShort()

            val pollRet = poll(pfd.ptr, 1u, timeoutMs)
            if (pollRet <= 0) return null

            val bufSize = 512
            val buf = allocArray<UByteVar>(bufSize)
            val bytesRead = read(fd, buf, bufSize.toULong())
            if (bytesRead < (firmwareOffset + 2).toLong()) return null
            if (buf[0].toInt() != HID_STATUS_REPORT_ID) return null

            val fwHigh = buf[firmwareOffset].toInt() and 0xFF
            val fwLow = buf[firmwareOffset + 1].toInt() and 0xFF
            val version = (fwHigh shl 8) or fwLow

            return if (version in 1..65534) version else null
        }
    } catch (_: Throwable) {
        return null
    } finally {
        close(fd)
    }
}

private fun writeFeatureReport(
    hidrawPath: String,
    reportData: UByteArray,
) {
    val totalLen = reportData.size
    val fd = open(hidrawPath, O_RDWR)
    check(fd >= 0) { "Cannot open $hidrawPath for writing" }

    try {
        memScoped {
            val buf = allocArray<UByteVar>(totalLen)
            for (i in 0 until totalLen) {
                buf[i] = reportData[i]
            }

            usleep(200_000u)

            val cmd = hidiocsfeature(totalLen)
            val ret = ioctl(fd, cmd, buf)
            check(ret >= 0) { "ioctl HIDIOCSFEATURE failed on $hidrawPath" }
        }
    } finally {
        close(fd)
    }
}

private const val USBDEVFS_CLAIMINTERFACE: ULong = 0x8004550FUL
private const val USBDEVFS_RELEASEINTERFACE: ULong = 0x80045510UL
private const val USBDEVFS_BULK: ULong = 0xC0185502UL

private data class UsbBulkSession(
    val fd: Int,
    val ifaceNumber: UInt,
)

private val activeUsbBulkSessions = mutableMapOf<String, UsbBulkSession>()

@OptIn(ExperimentalForeignApi::class)
private fun closeUsbBulkSession(hidrawPath: String) {
    val session = activeUsbBulkSessions.remove(hidrawPath) ?: return
    memScoped {
        val iface = alloc<UIntVar>()
        iface.value = session.ifaceNumber
        ioctl(session.fd, USBDEVFS_RELEASEINTERFACE, iface.ptr)
    }
    close(session.fd)
}

@OptIn(ExperimentalForeignApi::class)
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

@OptIn(ExperimentalForeignApi::class)
private fun getOrCreateUsbBulkSession(hidrawPath: String): UsbBulkSession {
    val existing = activeUsbBulkSessions[hidrawPath]
    if (existing != null) return existing

    val usbPath =
        resolveUsbDevicePath(hidrawPath)
            ?: error("Cannot determine USB device path for $hidrawPath")

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

    memScoped {
        val iface = alloc<UIntVar>()
        iface.value = 0u // Interface 0 (Vendor Specific)
        val claimRet = ioctl(fd, USBDEVFS_CLAIMINTERFACE, iface.ptr)
        if (claimRet < 0) {
            val err = errno
            close(fd)
            error("ioctl USBDEVFS_CLAIMINTERFACE failed on $usbPath interface 0 (errno $err)")
        }
    }

    val session = UsbBulkSession(fd = fd, ifaceNumber = 0u)
    activeUsbBulkSessions[hidrawPath] = session
    return session
}

@OptIn(ExperimentalForeignApi::class)
private fun writeVirtualSensorsBulkReport(
    hidrawPath: String,
    reportData: UByteArray,
) {
    val session = getOrCreateUsbBulkSession(hidrawPath)
    try {
        memScoped {
            // struct usbdevfs_bulktransfer { ep: u32, len: u32, timeout: u32, pad: u32, data: void* }
            // 24 bytes on 64-bit Linux
            val transfer = allocArray<UByteVar>(24)
            val uintView = transfer.reinterpret<UIntVar>()
            uintView[0] = 0x02u // Endpoint 0x02 OUT
            uintView[1] = reportData.size.toUInt()
            uintView[2] = 2000u // 2000 ms timeout

            val buf = allocArray<UByteVar>(reportData.size)
            for (i in reportData.indices) {
                buf[i] = reportData[i]
            }

            val dataPtr = (transfer + 16)!!.reinterpret<COpaquePointerVar>().pointed
            dataPtr.value = buf

            val bulkRet = ioctl(session.fd, USBDEVFS_BULK, transfer)
            if (bulkRet < 0) {
                val err = errno
                closeUsbBulkSession(hidrawPath)
                error("ioctl USBDEVFS_BULK failed on $hidrawPath EP 0x02 OUT (errno $err)")
            }
        }
    } catch (t: Throwable) {
        closeUsbBulkSession(hidrawPath)
        throw t
    }
}
