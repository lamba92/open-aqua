package io.github.lamba92.openaqua.engine.prod

import io.github.lamba92.openaqua.engine.hardware.HardwareController
import io.github.lamba92.openaqua.engine.protocol.ConfigParser
import io.github.lamba92.openaqua.engine.protocol.ConfigWriter
import io.github.lamba92.openaqua.engine.protocol.StatusReportParser
import io.github.lamba92.openaqua.engine.protocol.VirtualSensorsWriter
import io.github.lamba92.openaqua.model.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.DeviceAlias
import io.github.lamba92.openaqua.model.DeviceConfig
import io.github.lamba92.openaqua.model.DeviceDescriptor
import io.github.lamba92.openaqua.model.DeviceId
import io.github.lamba92.openaqua.model.DeviceRegistry
import io.github.lamba92.openaqua.model.DeviceTelemetry
import io.github.lamba92.openaqua.model.DeviceType
import io.github.lamba92.openaqua.model.FlashConfigRequest
import io.github.lamba92.openaqua.model.FlashD5NextConfig
import io.github.lamba92.openaqua.model.FlashOctoConfig
import io.github.lamba92.openaqua.model.FlashQuadroConfig
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.UShortVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKStringFromUtf16
import kotlinx.cinterop.value
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import platform.windows.CloseHandle
import platform.windows.CreateFileW
import platform.windows.FILE_SHARE_READ
import platform.windows.FILE_SHARE_WRITE
import platform.windows.GENERIC_READ
import platform.windows.GENERIC_WRITE
import platform.windows.GetLastError
import platform.windows.INVALID_HANDLE_VALUE
import platform.windows.OPEN_EXISTING
import platform.windows.ReadFile
import platform.windows.Sleep
import platform.windows.WriteFile
import winhid.HidD_GetAttributes
import winhid.HidD_GetFeature
import winhid.HidD_GetSerialNumberString
import winhid.HidD_SetFeature
import winhid.HidD_SetOutputReport
import winhid.SP_DEVICE_INTERFACE_DATA
import winhid.SP_DEVICE_INTERFACE_DETAIL_DATA_W
import winhid.SetupDiDestroyDeviceInfoList
import winhid.SetupDiGetDeviceInterfaceDetailW
import winhid.enum_device_interface
import winhid.get_hid_dev_info
import winhid.get_hid_guid
import kotlin.time.Duration.Companion.seconds

private const val HID_STATUS_REPORT_ID = 0x01

private data class ScannedWindowsNode(
    val id: DeviceId,
    val descriptor: DeviceDescriptor,
    val serial: String?,
    val devicePath: String,
    val fwVersion: Int?,
)

class WindowsHardwareController : HardwareController {
    private val scannedNodes = mutableMapOf<DeviceId, ScannedWindowsNode>()

    private fun getScannedNode(device: DetectedHardwareDevice): ScannedWindowsNode =
        scannedNodes[device.id] ?: run {
            scanDevices()
            scannedNodes[device.id]
                ?: error("Hardware device '${device.id}' (${device.name}) not found")
        }

    override fun scanDevices(): List<DetectedHardwareDevice> {
        val scanned = mutableListOf<ScannedWindowsNode>()

        memScoped {
            val hidGuid = allocArray<UByteVar>(16)
            get_hid_guid(hidGuid)

            val devInfo = get_hid_dev_info(hidGuid)
            check(devInfo != INVALID_HANDLE_VALUE) { "SetupDiGetClassDevs failed" }

            try {
                val ifData = alloc<SP_DEVICE_INTERFACE_DATA>()
                ifData.cbSize = sizeOf<SP_DEVICE_INTERFACE_DATA>().toUInt()

                var index = 0u
                while (enum_device_interface(devInfo, hidGuid, index++, ifData.ptr) != 0) {
                    val reqSize = alloc<UIntVar>()
                    SetupDiGetDeviceInterfaceDetailW(devInfo, ifData.ptr, null, 0u, reqSize.ptr, null)

                    val detailSize = reqSize.value.toInt()
                    if (detailSize <= 0) continue

                    val detailBuf = allocArray<UByteVar>(detailSize)
                    val detail = detailBuf.reinterpret<SP_DEVICE_INTERFACE_DETAIL_DATA_W>()
                    val cbSize = if (sizeOf<CPointerVar<*>>() == 8L) 8u else (4u + 2u)
                    detail.pointed.cbSize = cbSize

                    if (SetupDiGetDeviceInterfaceDetailW(devInfo, ifData.ptr, detail, detailSize.toUInt(), null, null) != 0) {
                        val path = detail.pointed.DevicePath.toKStringFromUtf16()
                        val node = inspectDevicePath(path)
                        if (node != null && scanned.none { it.id == node.id }) {
                            scanned.add(node)
                        }
                    }
                }
            } finally {
                SetupDiDestroyDeviceInfoList(devInfo)
            }
        }

        scannedNodes.clear()
        for (node in scanned) {
            scannedNodes[node.id] = node
        }

        return scanned
            .sortedWith(
                compareBy<ScannedWindowsNode> { it.descriptor.type.name }
                    .thenBy { it.serial ?: it.devicePath },
            ).groupBy { it.descriptor.type }
            .flatMap { (type, nodes) ->
                val typePrefix = type.name.lowercase().replace('_', '-')
                nodes.mapIndexed { index, node ->
                    DetectedHardwareDevice(
                        id = node.id,
                        alias = DeviceAlias.from("$typePrefix-${index + 1}"),
                        type = node.descriptor.type,
                        name = node.descriptor.name,
                        serial = node.serial,
                        firmwareVersion = node.fwVersion,
                    )
                }
            }
    }

    private fun inspectDevicePath(devicePath: String): ScannedWindowsNode? {
        val handle =
            CreateFileW(
                devicePath,
                GENERIC_READ,
                (FILE_SHARE_READ or FILE_SHARE_WRITE).toUInt(),
                null,
                OPEN_EXISTING.toUInt(),
                0u,
                null,
            )
        if (handle == INVALID_HANDLE_VALUE) return null

        try {
            memScoped {
                val attribSize = 10
                val attrib = allocArray<UByteVar>(attribSize)
                attrib[0] = attribSize.toUByte()

                val ok = HidD_GetAttributes(handle, attrib.reinterpret())
                if (ok == 0.toUByte()) return null

                val vid = (attrib[5].toInt() shl 8) or attrib[4].toInt()
                val pid = (attrib[7].toInt() shl 8) or attrib[6].toInt()

                if (vid != DeviceRegistry.AQUACOMPUTER_VENDOR_ID) return null
                val descriptor = DeviceRegistry.findByProductId(pid) ?: return null

                val serialBuf = allocArray<UByteVar>(256)
                val serialOk = HidD_GetSerialNumberString(handle, serialBuf, 256u)
                val serial =
                    if (serialOk != 0.toUByte()) {
                        serialBuf.reinterpret<UShortVar>().toKStringFromUtf16()
                    } else {
                        null
                    }

                val fw =
                    descriptor.firmwareVersionOffset?.let { offset ->
                        readFirmwareVersion(devicePath, offset)
                    }

                val id = DeviceId.from("$vid-$pid" + if (serial != null) "-$serial" else "")

                return ScannedWindowsNode(
                    id = id,
                    descriptor = descriptor,
                    serial = serial,
                    devicePath = devicePath,
                    fwVersion = fw,
                )
            }
        } finally {
            CloseHandle(handle)
        }
    }

    override fun streamTelemetry(device: DetectedHardwareDevice): Flow<DeviceTelemetry> =
        flow {
            val descriptor =
                DeviceRegistry.ALL.firstOrNull { it.type == device.type }
                    ?: error("Unsupported device type: ${device.type}")

            val node = getScannedNode(device)
            val handle =
                CreateFileW(
                    node.devicePath,
                    GENERIC_READ,
                    (FILE_SHARE_READ or FILE_SHARE_WRITE).toUInt(),
                    null,
                    OPEN_EXISTING.toUInt(),
                    0u,
                    null,
                )
            check(handle != INVALID_HANDLE_VALUE) { "Cannot open device for streaming: ${node.devicePath}" }

            try {
                memScoped {
                    val reportLen = descriptor.statusReportLength
                    val buf = allocArray<UByteVar>(512)
                    val bytesRead = alloc<UIntVar>()

                    while (currentCoroutineContext().isActive) {
                        val ok = ReadFile(handle, buf, 512u, bytesRead.ptr, null)
                        if (ok != 0 && bytesRead.value >= reportLen.toUInt() && buf[0].toInt() == HID_STATUS_REPORT_ID) {
                            val raw = UByteArray(bytesRead.value.toInt()) { buf[it] }
                            val telemetry = StatusReportParser.parse(device, raw)
                            emit(telemetry)
                        }
                        delay(1.seconds)
                    }
                }
            } finally {
                CloseHandle(handle)
            }
        }

    override fun readConfig(device: DetectedHardwareDevice): DeviceConfig {
        val descriptor =
            DeviceRegistry.ALL.firstOrNull { it.type == device.type }
                ?: error("Unsupported device type: ${device.type}")

        val node = getScannedNode(device)
        val report = readFeatureReport(node.devicePath, 0x03u, descriptor.ctrlReportLength)

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
        val report = readFeatureReport(node.devicePath, 0x03u, descriptor.ctrlReportLength)

        ConfigWriter.applyFlashConfig(report, descriptor, request)
        ConfigWriter.finalizeReport(report, descriptor.ctrlReportLength)

        writeFeatureReport(node.devicePath, report)
        writeOutputReport(node.devicePath, ConfigWriter.SECONDARY_COMMIT_REPORT)
        Sleep(250u)

        val readback = readFeatureReport(node.devicePath, 0x03u, descriptor.ctrlReportLength)
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

        writeOutputOrFeatureReport(node.devicePath, report)
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

private fun readFeatureReport(
    devicePath: String,
    reportId: UByte,
    reportPayloadLength: Int,
): UByteArray {
    val totalLen = reportPayloadLength + 1
    val handle =
        CreateFileW(
            devicePath,
            (GENERIC_READ or GENERIC_WRITE.toUInt()),
            (FILE_SHARE_READ or FILE_SHARE_WRITE).toUInt(),
            null,
            OPEN_EXISTING.toUInt(),
            0u,
            null,
        )
    check(handle != INVALID_HANDLE_VALUE) { "Cannot open $devicePath for reading feature report" }

    try {
        memScoped {
            val buf = allocArray<UByteVar>(totalLen)
            buf[0] = reportId

            val ret = HidD_GetFeature(handle, buf, totalLen.toUInt())
            check(ret != 0.toUByte()) {
                val err = GetLastError()
                "HidD_GetFeature failed on $devicePath: GetLastError=$err"
            }

            val result = UByteArray(totalLen)
            for (i in 0 until totalLen) {
                result[i] = buf[i]
            }
            return result
        }
    } finally {
        CloseHandle(handle)
    }
}

private fun readFirmwareVersion(
    devicePath: String,
    firmwareOffset: Int,
): Int? {
    val handle =
        CreateFileW(
            devicePath,
            GENERIC_READ,
            (FILE_SHARE_READ or FILE_SHARE_WRITE).toUInt(),
            null,
            OPEN_EXISTING.toUInt(),
            0u,
            null,
        )
    if (handle == INVALID_HANDLE_VALUE) return null

    try {
        memScoped {
            val bufSize = 512
            val buf = allocArray<UByteVar>(bufSize)
            val bytesRead = alloc<UIntVar>()

            val ok = ReadFile(handle, buf, bufSize.toUInt(), bytesRead.ptr, null)
            if (ok == 0) return null
            if (bytesRead.value < (firmwareOffset + 2).toUInt()) return null
            if (buf[0].toInt() != HID_STATUS_REPORT_ID) return null

            val fwHigh = buf[firmwareOffset].toInt() and 0xFF
            val fwLow = buf[firmwareOffset + 1].toInt() and 0xFF
            val version = (fwHigh shl 8) or fwLow

            return if (version in 1..65534) version else null
        }
    } catch (_: Throwable) {
        return null
    } finally {
        CloseHandle(handle)
    }
}

private fun writeFeatureReport(
    devicePath: String,
    reportData: UByteArray,
) {
    val totalLen = reportData.size
    val handle =
        CreateFileW(
            devicePath,
            (GENERIC_READ or GENERIC_WRITE.toUInt()),
            (FILE_SHARE_READ or FILE_SHARE_WRITE).toUInt(),
            null,
            OPEN_EXISTING.toUInt(),
            0u,
            null,
        )
    check(handle != INVALID_HANDLE_VALUE) {
        val err = GetLastError()
        "Cannot open $devicePath for writing feature report: GetLastError=$err"
    }

    try {
        memScoped {
            val buf = allocArray<UByteVar>(totalLen)
            for (i in 0 until totalLen) {
                buf[i] = reportData[i]
            }

            Sleep(200u)

            val ret = HidD_SetFeature(handle, buf, totalLen.toUInt())
            check(ret != 0.toUByte()) {
                val err = GetLastError()
                val reportId = reportData.getOrNull(0)?.toInt() ?: -1
                "HidD_SetFeature failed on $devicePath (err=$err, reportId=0x${reportId.toString(16)}, totalLen=$totalLen)"
            }
        }
    } finally {
        CloseHandle(handle)
    }
}

private fun writeOutputReport(
    devicePath: String,
    reportData: UByteArray,
) {
    val totalLen = reportData.size
    val handle =
        CreateFileW(
            devicePath,
            (GENERIC_READ or GENERIC_WRITE.toUInt()),
            (FILE_SHARE_READ or FILE_SHARE_WRITE).toUInt(),
            null,
            OPEN_EXISTING.toUInt(),
            0u,
            null,
        )
    check(handle != INVALID_HANDLE_VALUE) {
        val err = GetLastError()
        "Cannot open $devicePath for writing output report: GetLastError=$err"
    }

    try {
        memScoped {
            val buf = allocArray<UByteVar>(totalLen)
            for (i in 0 until totalLen) {
                buf[i] = reportData[i]
            }

            Sleep(100u)

            val ret = HidD_SetOutputReport(handle, buf, totalLen.toUInt())
            if (ret == 0.toUByte()) {
                val bytesWritten = alloc<UIntVar>()
                val writeOk = WriteFile(handle, buf, totalLen.toUInt(), bytesWritten.ptr, null)
                check(writeOk != 0) {
                    val err = GetLastError()
                    "Both HidD_SetOutputReport and WriteFile failed on $devicePath (err=$err)"
                }
            }
        }
    } finally {
        CloseHandle(handle)
    }
}

private fun writeOutputOrFeatureReport(
    devicePath: String,
    reportData: UByteArray,
) {
    val totalLen = reportData.size
    val handle =
        CreateFileW(
            devicePath,
            (GENERIC_READ or GENERIC_WRITE.toUInt()),
            (FILE_SHARE_READ or FILE_SHARE_WRITE).toUInt(),
            null,
            OPEN_EXISTING.toUInt(),
            0u,
            null,
        )
    check(handle != INVALID_HANDLE_VALUE) { "Cannot open $devicePath for writing report" }

    try {
        memScoped {
            val buf = allocArray<UByteVar>(totalLen)
            for (i in 0 until totalLen) {
                buf[i] = reportData[i]
            }

            val setOutputOk = HidD_SetOutputReport(handle, buf, totalLen.toUInt())
            if (setOutputOk == 0.toUByte()) {
                val bytesWritten = alloc<UIntVar>()
                val writeOk = WriteFile(handle, buf, totalLen.toUInt(), bytesWritten.ptr, null)
                if (writeOk == 0) {
                    val setFeatureOk = HidD_SetFeature(handle, buf, totalLen.toUInt())
                    check(setFeatureOk != 0.toUByte()) {
                        "HidD_SetOutputReport, WriteFile, and HidD_SetFeature all failed on $devicePath"
                    }
                }
            }
        }
    } finally {
        CloseHandle(handle)
    }
}
