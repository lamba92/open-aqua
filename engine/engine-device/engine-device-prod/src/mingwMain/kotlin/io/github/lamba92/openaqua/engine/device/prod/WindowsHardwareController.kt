package io.github.lamba92.openaqua.engine.device.prod

import io.github.lamba92.openaqua.engine.device.hardware.HardwareController
import io.github.lamba92.openaqua.engine.device.protocol.ConfigParser
import io.github.lamba92.openaqua.engine.device.protocol.ConfigWriter
import io.github.lamba92.openaqua.engine.device.protocol.StatusReportParser
import io.github.lamba92.openaqua.engine.device.protocol.VirtualSensorsWriter
import io.github.lamba92.openaqua.model.device.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.device.DeviceAlias
import io.github.lamba92.openaqua.model.device.DeviceConfig
import io.github.lamba92.openaqua.model.device.DeviceDescriptor
import io.github.lamba92.openaqua.model.device.DeviceId
import io.github.lamba92.openaqua.model.device.DeviceRegistry
import io.github.lamba92.openaqua.model.device.DeviceTelemetry
import io.github.lamba92.openaqua.model.device.FlashConfigRequest
import io.github.lamba92.openaqua.model.device.descriptor
import io.github.lamba92.openaqua.model.device.validateRequestMatchesDevice
import io.github.lamba92.openaqua.model.device.virtualSensorsCount
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.UShortVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.plus
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
import platform.posix.GUID
import platform.windows.CloseHandle
import platform.windows.CreateFileW
import platform.windows.FILE_FLAG_OVERLAPPED
import platform.windows.FILE_SHARE_READ
import platform.windows.FILE_SHARE_WRITE
import platform.windows.GENERIC_READ
import platform.windows.GENERIC_WRITE
import platform.windows.GetLastError
import platform.windows.HANDLE
import platform.windows.INVALID_HANDLE_VALUE
import platform.windows.OPEN_EXISTING
import platform.windows.ReadFile
import platform.windows.Sleep
import platform.windows.WriteFile
import winhid.DIGCF_DEVICEINTERFACE
import winhid.DIGCF_PRESENT
import winhid.HidD_GetAttributes
import winhid.HidD_GetFeature
import winhid.HidD_GetHidGuid
import winhid.HidD_GetSerialNumberString
import winhid.HidD_SetFeature
import winhid.HidD_SetOutputReport
import winhid.SP_DEVICE_INTERFACE_DATA
import winhid.SP_DEVICE_INTERFACE_DETAIL_DATA_W
import winhid.SetupDiDestroyDeviceInfoList
import winhid.SetupDiEnumDeviceInterfaces
import winhid.SetupDiGetClassDevsW
import winhid.SetupDiGetDeviceInterfaceDetailW
import winhid.WINUSB_INTERFACE_HANDLE
import winhid.WINUSB_INTERFACE_HANDLEVar
import winhid.WinUsb_Free
import winhid.WinUsb_GetDescriptor
import winhid.WinUsb_Initialize
import winhid.WinUsb_SetPipePolicy
import winhid.WinUsb_WritePipe
import kotlin.time.Duration.Companion.seconds

private const val HID_STATUS_REPORT_ID = 0x01

private data class ScannedWindowsNode(
    val id: DeviceId,
    val descriptor: DeviceDescriptor,
    val serial: String?,
    val devicePath: String,
    val fwVersion: Int?,
)

object WindowsHardwareController : HardwareController {
    private fun getScannedNode(device: DetectedHardwareDevice): ScannedWindowsNode =
        scanNodes().firstOrNull { it.id == device.id }
            ?: error("Hardware device '${device.id}' (${device.name}) not found")

    private fun scanNodes(): List<ScannedWindowsNode> {
        val scanned = mutableListOf<ScannedWindowsNode>()

        memScoped {
            val hidGuid = alloc<GUID>()
            HidD_GetHidGuid(hidGuid.ptr)

            forEachDeviceInterfacePath(hidGuid.ptr) { path ->
                val node = inspectDevicePath(path)
                if (node != null && scanned.none { it.id == node.id }) {
                    scanned.add(node)
                }
                false
            }
        }
        return scanned
    }

    override fun scanDevices(): List<DetectedHardwareDevice> =
        scanNodes()
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

    private fun inspectDevicePath(devicePath: String): ScannedWindowsNode? {
        val handle =
            CreateFileW(
                lpFileName = devicePath,
                dwDesiredAccess = GENERIC_READ,
                dwShareMode = (FILE_SHARE_READ or FILE_SHARE_WRITE).toUInt(),
                lpSecurityAttributes = null,
                dwCreationDisposition = OPEN_EXISTING.toUInt(),
                dwFlagsAndAttributes = 0u,
                hTemplateFile = null,
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

                val fw = readFirmwareVersion(devicePath, descriptor.protocol.firmwareVersionOffset)

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
            val descriptor = device.descriptor()

            val node = getScannedNode(device)
            val handle =
                CreateFileW(
                    lpFileName = node.devicePath,
                    dwDesiredAccess = GENERIC_READ,
                    dwShareMode = (FILE_SHARE_READ or FILE_SHARE_WRITE).toUInt(),
                    lpSecurityAttributes = null,
                    dwCreationDisposition = OPEN_EXISTING.toUInt(),
                    dwFlagsAndAttributes = 0u,
                    hTemplateFile = null,
                )
            check(handle != INVALID_HANDLE_VALUE) { "Cannot open device for streaming: ${node.devicePath}" }

            try {
                memScoped {
                    val reportLen = descriptor.protocol.statusReportLength
                    val buf = allocArray<UByteVar>(512)
                    val bytesRead = alloc<UIntVar>()

                    while (currentCoroutineContext().isActive) {
                        val ok = ReadFile(handle, buf, 512u, bytesRead.ptr, null)
                        if (ok != 0 && bytesRead.value >= reportLen.toUInt() && buf[0].toInt() == descriptor.protocol.statusReportId) {
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
        val report = readFeatureReport(node.devicePath, descriptor.protocol.ctrlReportId.toUByte(), descriptor.protocol.ctrlReportLength)

        return ConfigParser.parseConfig(descriptor, report)
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
        val report = readFeatureReport(node.devicePath, descriptor.protocol.ctrlReportId.toUByte(), descriptor.protocol.ctrlReportLength)

        val flashReports = ConfigWriter.prepareFlashReports(report, descriptor, request)
        flashReports.forEachIndexed { index, reportToWrite ->
            if (index == 0) {
                writeFeatureReport(node.devicePath, reportToWrite)
            } else {
                writeOutputReport(node.devicePath, reportToWrite)
            }
        }
        Sleep(250u)

        val readback = readFeatureReport(node.devicePath, descriptor.protocol.ctrlReportId.toUByte(), descriptor.protocol.ctrlReportLength)
        val readbackReport = ConfigParser.parseConfig(descriptor, readback)

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
        writeVirtualSensorsBulkReport(device.id, report)
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
            lpFileName = devicePath,
            dwDesiredAccess = (GENERIC_READ or GENERIC_WRITE.toUInt()),
            dwShareMode = (FILE_SHARE_READ or FILE_SHARE_WRITE).toUInt(),
            lpSecurityAttributes = null,
            dwCreationDisposition = OPEN_EXISTING.toUInt(),
            dwFlagsAndAttributes = 0u,
            hTemplateFile = null,
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
            lpFileName = devicePath,
            dwDesiredAccess = GENERIC_READ,
            dwShareMode = (FILE_SHARE_READ or FILE_SHARE_WRITE).toUInt(),
            lpSecurityAttributes = null,
            dwCreationDisposition = OPEN_EXISTING.toUInt(),
            dwFlagsAndAttributes = 0u,
            hTemplateFile = null,
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
            lpFileName = devicePath,
            dwDesiredAccess = (GENERIC_READ or GENERIC_WRITE.toUInt()),
            dwShareMode = (FILE_SHARE_READ or FILE_SHARE_WRITE).toUInt(),
            lpSecurityAttributes = null,
            dwCreationDisposition = OPEN_EXISTING.toUInt(),
            dwFlagsAndAttributes = 0u,
            hTemplateFile = null,
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
            lpFileName = devicePath,
            dwDesiredAccess = (GENERIC_READ or GENERIC_WRITE.toUInt()),
            dwShareMode = (FILE_SHARE_READ or FILE_SHARE_WRITE).toUInt(),
            lpSecurityAttributes = null,
            dwCreationDisposition = OPEN_EXISTING.toUInt(),
            dwFlagsAndAttributes = 0u,
            hTemplateFile = null,
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

private const val USB_DEVICE_DESCRIPTOR_TYPE: UByte = 0x01u
private const val USB_STRING_DESCRIPTOR_TYPE: UByte = 0x03u
private const val PIPE_TRANSFER_TIMEOUT: UInt = 0x03u
private const val VIRTUAL_SENSORS_BULK_OUT_PIPE: UByte = 0x02u

private data class WinUsbBulkSession(
    val fileHandle: HANDLE,
    val winUsbHandle: WINUSB_INTERFACE_HANDLE,
)

private val activeWinUsbBulkSessions = mutableMapOf<DeviceId, WinUsbBulkSession>()

private fun closeWinUsbBulkSession(deviceId: DeviceId) {
    val session = activeWinUsbBulkSessions.remove(deviceId) ?: return
    WinUsb_Free(session.winUsbHandle)
    CloseHandle(session.fileHandle)
}

private fun tryOpenMatchingWinUsbSession(
    devicePath: String,
    targetDeviceId: DeviceId,
): WinUsbBulkSession? {
    val fileHandle =
        CreateFileW(
            lpFileName = devicePath,
            dwDesiredAccess = (GENERIC_READ or GENERIC_WRITE.toUInt()),
            dwShareMode = (FILE_SHARE_READ or FILE_SHARE_WRITE).toUInt(),
            lpSecurityAttributes = null,
            dwCreationDisposition = OPEN_EXISTING.toUInt(),
            dwFlagsAndAttributes = FILE_FLAG_OVERLAPPED.toUInt(),
            hTemplateFile = null,
        )
    if (fileHandle == INVALID_HANDLE_VALUE || fileHandle == null) return null

    memScoped {
        val winUsbHandleVar = alloc<WINUSB_INTERFACE_HANDLEVar>()
        if (WinUsb_Initialize(fileHandle, winUsbHandleVar.ptr) == 0) {
            CloseHandle(fileHandle)
            return null
        }
        val winUsbHandle =
            winUsbHandleVar.value ?: run {
                CloseHandle(fileHandle)
                return null
            }

        val devDesc = allocArray<UByteVar>(18)
        val transferred = alloc<UIntVar>()
        val descOk =
            WinUsb_GetDescriptor(
                InterfaceHandle = winUsbHandle,
                DescriptorType = USB_DEVICE_DESCRIPTOR_TYPE,
                Index = 0u,
                LanguageID = 0u,
                Buffer = devDesc,
                BufferLength = 18u,
                LengthTransferred = transferred.ptr,
            )
        if (descOk == 0 || transferred.value < 18u) {
            WinUsb_Free(winUsbHandle)
            CloseHandle(fileHandle)
            return null
        }

        val vid = (devDesc[9].toInt() shl 8) or devDesc[8].toInt()
        val pid = (devDesc[11].toInt() shl 8) or devDesc[10].toInt()
        if (vid != DeviceRegistry.AQUACOMPUTER_VENDOR_ID) {
            WinUsb_Free(winUsbHandle)
            CloseHandle(fileHandle)
            return null
        }

        val iSerial = devDesc[16]
        var serial: String? = null
        if (iSerial > 0u) {
            val strBuf = allocArray<UByteVar>(256)
            val strOk =
                WinUsb_GetDescriptor(
                    InterfaceHandle = winUsbHandle,
                    DescriptorType = USB_STRING_DESCRIPTOR_TYPE,
                    Index = iSerial,
                    LanguageID = 0x0409u,
                    Buffer = strBuf,
                    BufferLength = 256u,
                    LengthTransferred = transferred.ptr,
                )
            val byteLen = transferred.value.toInt()
            if (strOk != 0 && byteLen > 2) {
                strBuf[byteLen.coerceAtMost(254)] = 0u
                strBuf[(byteLen + 1).coerceAtMost(255)] = 0u
                serial = (strBuf + 2)!!.reinterpret<UShortVar>().toKStringFromUtf16()
            }
        }

        val candidateId = DeviceId.from("$vid-$pid" + if (!serial.isNullOrEmpty()) "-$serial" else "")
        if (candidateId != targetDeviceId) {
            WinUsb_Free(winUsbHandle)
            CloseHandle(fileHandle)
            return null
        }

        val timeoutMs = alloc<UIntVar>()
        timeoutMs.value = 2000u
        WinUsb_SetPipePolicy(
            InterfaceHandle = winUsbHandle,
            PipeID = VIRTUAL_SENSORS_BULK_OUT_PIPE,
            PolicyType = PIPE_TRANSFER_TIMEOUT,
            ValueLength = 4u,
            Value = timeoutMs.ptr,
        )

        return WinUsbBulkSession(fileHandle = fileHandle, winUsbHandle = winUsbHandle)
    }
}

private fun getOrCreateWinUsbBulkSession(deviceId: DeviceId): WinUsbBulkSession {
    activeWinUsbBulkSessions[deviceId]?.let { return it }

    memScoped {
        val winusbGuid = allocWinUsbGuid()
        var matched: WinUsbBulkSession? = null
        forEachDeviceInterfacePath(winusbGuid.ptr) { path ->
            val session = tryOpenMatchingWinUsbSession(path, deviceId)
            if (session != null) {
                matched = session
                true
            } else {
                false
            }
        }
        if (matched != null) {
            activeWinUsbBulkSessions[deviceId] = matched
            return matched
        }
    }

    error("Cannot find WinUSB interface 0 for device '$deviceId'")
}

private fun MemScope.allocWinUsbGuid(): GUID =
    alloc<GUID>().apply {
        Data1 = 0xdee824efu
        Data2 = 0x729bu
        Data3 = 0x4a0eu
        Data4[0] = 0x9cu
        Data4[1] = 0x14u
        Data4[2] = 0xb7u
        Data4[3] = 0x11u
        Data4[4] = 0x7du
        Data4[5] = 0x33u
        Data4[6] = 0xa8u
        Data4[7] = 0x17u
    }

private inline fun MemScope.forEachDeviceInterfacePath(
    guid: CPointer<GUID>,
    onPath: (String) -> Boolean,
) {
    val devInfo =
        SetupDiGetClassDevsW(
            ClassGuid = guid,
            Enumerator = null,
            hwndParent = null,
            Flags = (DIGCF_PRESENT or DIGCF_DEVICEINTERFACE).toUInt(),
        )
    check(devInfo != INVALID_HANDLE_VALUE) { "SetupDiGetClassDevsW failed" }

    try {
        val ifData = alloc<SP_DEVICE_INTERFACE_DATA>()
        ifData.cbSize = sizeOf<SP_DEVICE_INTERFACE_DATA>().toUInt()

        var index = 0u
        while (SetupDiEnumDeviceInterfaces(devInfo, null, guid, index++, ifData.ptr) != 0) {
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
                if (onPath(path)) return
            }
        }
    } finally {
        SetupDiDestroyDeviceInfoList(devInfo)
    }
}

private fun writeVirtualSensorsBulkReport(
    deviceId: DeviceId,
    reportData: UByteArray,
) {
    val session = getOrCreateWinUsbBulkSession(deviceId)
    try {
        memScoped {
            val buf = allocArray<UByteVar>(reportData.size)
            for (i in reportData.indices) {
                buf[i] = reportData[i]
            }

            val bytesWritten = alloc<UIntVar>()
            val writeOk =
                WinUsb_WritePipe(
                    InterfaceHandle = session.winUsbHandle,
                    PipeID = VIRTUAL_SENSORS_BULK_OUT_PIPE,
                    Buffer = buf,
                    BufferLength = reportData.size.toUInt(),
                    LengthTransferred = bytesWritten.ptr,
                    Overlapped = null,
                )
            if (writeOk == 0) {
                val err = GetLastError()
                closeWinUsbBulkSession(deviceId)
                error("WinUsb_WritePipe failed on $deviceId EP 0x02 OUT (err=$err)")
            }
        }
    } catch (t: Throwable) {
        closeWinUsbBulkSession(deviceId)
        throw t
    }
}
