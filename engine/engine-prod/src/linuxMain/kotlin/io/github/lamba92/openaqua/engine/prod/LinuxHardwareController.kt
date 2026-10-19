package io.github.lamba92.openaqua.engine.prod

import io.github.lamba92.openaqua.engine.hardware.HardwareController
import io.github.lamba92.openaqua.engine.protocol.ConfigParser
import io.github.lamba92.openaqua.engine.protocol.ConfigWriter
import io.github.lamba92.openaqua.engine.protocol.StatusReportParser
import io.github.lamba92.openaqua.engine.protocol.VirtualSensorsWriter
import io.github.lamba92.openaqua.engine.util.listDirectory
import io.github.lamba92.openaqua.engine.util.readTextOrNull
import io.github.lamba92.openaqua.model.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.DeviceConfig
import io.github.lamba92.openaqua.model.DeviceDescriptor
import io.github.lamba92.openaqua.model.DeviceRegistry
import io.github.lamba92.openaqua.model.DeviceTelemetry
import io.github.lamba92.openaqua.model.DeviceType
import io.github.lamba92.openaqua.model.FlashConfigRequest
import io.github.lamba92.openaqua.model.FlashD5NextConfig
import io.github.lamba92.openaqua.model.FlashOctoConfig
import io.github.lamba92.openaqua.model.FlashQuadroConfig
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.io.files.Path
import platform.posix.O_NONBLOCK
import platform.posix.O_RDWR
import platform.posix.POLLIN
import platform.posix.close
import platform.posix.ioctl
import platform.posix.open
import platform.posix.poll
import platform.posix.pollfd
import platform.posix.read
import platform.posix.usleep

private data class ScannedHardwareNode(
    val id: String,
    val descriptor: DeviceDescriptor,
    val serial: String?,
    val hidrawPath: String,
)

class LinuxHardwareController : HardwareController {
    private val scannedNodes = mutableMapOf<String, ScannedHardwareNode>()

    private fun getScannedNode(device: DetectedHardwareDevice): ScannedHardwareNode =
        scannedNodes[device.id] ?: run {
            scanDevices()
            scannedNodes[device.id]
                ?: error("Hardware device '${device.id}' (${device.name}) not found")
        }

    override fun scanDevices(): List<DetectedHardwareDevice> {
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
            val id = hidUniq ?: entryName

            scanned.add(
                ScannedHardwareNode(
                    id = id,
                    descriptor = descriptor,
                    serial = hidUniq,
                    hidrawPath = hidrawDevPath(entryName),
                ),
            )
        }

        scannedNodes.clear()
        for (node in scanned) {
            scannedNodes[node.id] = node
        }

        return scanned
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
                        alias = "$typePrefix-${index + 1}",
                        type = node.descriptor.type,
                        name = node.descriptor.name,
                        serial = node.serial,
                        firmwareVersion = fwVersion,
                    )
                }
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

        validateRequestMatchesDevice(device.type, request)

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

        val startChannel = if (device.type == DeviceType.D5_NEXT) 1 else 4
        val slotMap = mutableMapOf<Int, Double>()
        sensors.forEach { (channel, temp) ->
            val slot = channel - startChannel
            require(slot in 0 until descriptor.virtualSensorsCount) {
                "Channel $channel is not a valid virtual sensor for ${device.name} (valid channels: $startChannel..${startChannel + descriptor.virtualSensorsCount - 1})"
            }
            slotMap[slot] = temp
        }

        val report = VirtualSensorsWriter.buildReport(descriptor, slotMap)
        val node = getScannedNode(device)

        writeOutputOrFeatureReport(node.hidrawPath, report)
    }
}

private fun validateRequestMatchesDevice(
    type: DeviceType,
    request: FlashConfigRequest,
) {
    when (type) {
        DeviceType.OCTO ->
            check(request is FlashOctoConfig) {
                "Expected FlashOctoConfig for device type OCTO, got ${request::class.simpleName}"
            }
        DeviceType.QUADRO ->
            check(request is FlashQuadroConfig) {
                "Expected FlashQuadroConfig for device type QUADRO, got ${request::class.simpleName}"
            }
        DeviceType.D5_NEXT ->
            check(request is FlashD5NextConfig) {
                "Expected FlashD5NextConfig for device type D5_NEXT, got ${request::class.simpleName}"
            }
        else -> error("Unsupported device type: $type")
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

private fun writeOutputOrFeatureReport(
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

            val written = platform.posix.write(fd, buf, totalLen.toULong())
            if (written < 0) {
                val cmd = hidiocsfeature(totalLen)
                val ret = ioctl(fd, cmd, buf)
                check(ret >= 0) { "Failed to write report to $hidrawPath" }
            }
        }
    } finally {
        close(fd)
    }
}
