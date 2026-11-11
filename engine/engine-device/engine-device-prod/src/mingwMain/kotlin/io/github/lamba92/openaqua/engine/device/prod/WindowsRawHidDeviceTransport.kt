@file:OptIn(ExperimentalForeignApi::class)

package io.github.lamba92.openaqua.engine.device.prod

import io.github.lamba92.openaqua.engine.device.hardware.RawHidDevice
import io.github.lamba92.openaqua.engine.device.hardware.RawHidDeviceTransport
import io.github.lamba92.openaqua.model.device.DeviceRegistry
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
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

private const val USB_DEVICE_DESCRIPTOR_TYPE: UByte = 0x01u
private const val USB_STRING_DESCRIPTOR_TYPE: UByte = 0x03u
private const val PIPE_TRANSFER_TIMEOUT: UInt = 0x03u
private const val VIRTUAL_SENSORS_BULK_OUT_PIPE: UByte = 0x02u

private data class WinUsbBulkSession(
    val fileHandle: HANDLE,
    val winUsbHandle: WINUSB_INTERFACE_HANDLE,
)

object WindowsRawHidDeviceTransport : RawHidDeviceTransport {
    override fun scanDevices(): List<RawHidDevice> {
        val scanned = mutableListOf<RawHidDevice>()

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

    private fun inspectDevicePath(devicePath: String): RawHidDevice? {
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
                if (DeviceRegistry.findByProductId(pid) == null) return null

                val serialBuf = allocArray<UByteVar>(256)
                val serialOk = HidD_GetSerialNumberString(handle, serialBuf, 256u)
                val serial =
                    if (serialOk != 0.toUByte()) {
                        serialBuf.reinterpret<UShortVar>().toKStringFromUtf16()
                    } else {
                        null
                    }

                return RawHidDevice(
                    id = devicePath,
                    vendorId = vid,
                    productId = pid,
                    serial = serial,
                )
            }
        } finally {
            CloseHandle(handle)
        }
    }

    override fun readFeatureReport(
        device: RawHidDevice,
        reportId: Int,
        reportLength: Int,
    ): UByteArray {
        val totalLen = reportLength
        val handle =
            CreateFileW(
                lpFileName = device.id,
                dwDesiredAccess = (GENERIC_READ or GENERIC_WRITE.toUInt()),
                dwShareMode = (FILE_SHARE_READ or FILE_SHARE_WRITE).toUInt(),
                lpSecurityAttributes = null,
                dwCreationDisposition = OPEN_EXISTING.toUInt(),
                dwFlagsAndAttributes = 0u,
                hTemplateFile = null,
            )
        check(handle != INVALID_HANDLE_VALUE) {
            val err = GetLastError()
            "Cannot open ${device.id} for reading feature report: GetLastError=$err"
        }

        try {
            memScoped {
                val buf = allocArray<UByteVar>(totalLen)
                buf[0] = reportId.toUByte()

                val ret = HidD_GetFeature(handle, buf, totalLen.toUInt())
                check(ret != 0.toUByte()) {
                    val err = GetLastError()
                    "HidD_GetFeature failed on ${device.id} (err=$err, reportId=0x${reportId.toString(16)}, totalLen=$totalLen)"
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

    override fun writeFeatureReport(
        device: RawHidDevice,
        report: UByteArray,
    ) {
        val totalLen = report.size
        val handle =
            CreateFileW(
                lpFileName = device.id,
                dwDesiredAccess = (GENERIC_READ or GENERIC_WRITE.toUInt()),
                dwShareMode = (FILE_SHARE_READ or FILE_SHARE_WRITE).toUInt(),
                lpSecurityAttributes = null,
                dwCreationDisposition = OPEN_EXISTING.toUInt(),
                dwFlagsAndAttributes = 0u,
                hTemplateFile = null,
            )
        check(handle != INVALID_HANDLE_VALUE) {
            val err = GetLastError()
            "Cannot open ${device.id} for writing feature report: GetLastError=$err"
        }

        try {
            memScoped {
                val buf = allocArray<UByteVar>(totalLen)
                for (i in 0 until totalLen) {
                    buf[i] = report[i]
                }

                Sleep(200u)

                val ret = HidD_SetFeature(handle, buf, totalLen.toUInt())
                check(ret != 0.toUByte()) {
                    val err = GetLastError()
                    "HidD_SetFeature failed on ${device.id} (err=$err, reportId=0x${report[0].toString(16)}, totalLen=$totalLen)"
                }
            }
        } finally {
            CloseHandle(handle)
        }
    }

    override fun writeOutputReport(
        device: RawHidDevice,
        report: UByteArray,
    ) {
        val totalLen = report.size
        val handle =
            CreateFileW(
                lpFileName = device.id,
                dwDesiredAccess = (GENERIC_READ or GENERIC_WRITE.toUInt()),
                dwShareMode = (FILE_SHARE_READ or FILE_SHARE_WRITE).toUInt(),
                lpSecurityAttributes = null,
                dwCreationDisposition = OPEN_EXISTING.toUInt(),
                dwFlagsAndAttributes = 0u,
                hTemplateFile = null,
            )
        check(handle != INVALID_HANDLE_VALUE) {
            val err = GetLastError()
            "Cannot open ${device.id} for writing output report: GetLastError=$err"
        }

        try {
            memScoped {
                val buf = allocArray<UByteVar>(totalLen)
                for (i in 0 until totalLen) {
                    buf[i] = report[i]
                }

                Sleep(200u)

                val ok = HidD_SetOutputReport(handle, buf, totalLen.toUInt())
                if (ok == 0.toUByte()) {
                    val bytesWritten = alloc<UIntVar>()
                    val writeOk = WriteFile(handle, buf, totalLen.toUInt(), bytesWritten.ptr, null)
                    check(writeOk != 0) {
                        val err = GetLastError()
                        "Failed to write output report to ${device.id} (SetOutputReport and WriteFile failed, err=$err)"
                    }
                }
            }
        } finally {
            CloseHandle(handle)
        }
    }

    override fun streamInputReports(
        device: RawHidDevice,
        reportId: Int,
        reportLength: Int,
    ): Flow<UByteArray> =
        flow {
            val handle =
                CreateFileW(
                    lpFileName = device.id,
                    dwDesiredAccess = GENERIC_READ,
                    dwShareMode = (FILE_SHARE_READ or FILE_SHARE_WRITE).toUInt(),
                    lpSecurityAttributes = null,
                    dwCreationDisposition = OPEN_EXISTING.toUInt(),
                    dwFlagsAndAttributes = 0u,
                    hTemplateFile = null,
                )
            check(handle != INVALID_HANDLE_VALUE) { "Cannot open device for streaming: ${device.id}" }

            try {
                memScoped {
                    val buf = allocArray<UByteVar>(512)
                    val bytesRead = alloc<UIntVar>()

                    while (currentCoroutineContext().isActive) {
                        val ok = ReadFile(handle, buf, 512u, bytesRead.ptr, null)
                        if (ok != 0 && bytesRead.value >= reportLength.toUInt() && buf[0].toInt() == reportId) {
                            val raw = UByteArray(bytesRead.value.toInt()) { buf[it] }
                            emit(raw)
                        }
                        delay(1.seconds)
                    }
                }
            } finally {
                CloseHandle(handle)
            }
        }

    override suspend fun feedBulkReports(
        device: RawHidDevice,
        reports: Flow<UByteArray>,
    ) {
        val session = openWinUsbBulkSession(device)
        try {
            reports.collect { reportData ->
                memScoped {
                    val buf = allocArray<UByteVar>(reportData.size)
                    for (i in reportData.indices) {
                        buf[i] = reportData[i]
                    }
                    val bytesWritten = alloc<UIntVar>()
                    val ok =
                        WinUsb_WritePipe(
                            InterfaceHandle = session.winUsbHandle,
                            PipeID = VIRTUAL_SENSORS_BULK_OUT_PIPE,
                            Buffer = buf,
                            BufferLength = reportData.size.toUInt(),
                            LengthTransferred = bytesWritten.ptr,
                            Overlapped = null,
                        )
                    if (ok == 0) {
                        val err = GetLastError()
                        error("WinUsb_WritePipe failed on ${device.id} pipe 0x02 OUT (GetLastError=$err)")
                    }
                }
            }
        } finally {
            WinUsb_Free(session.winUsbHandle)
            CloseHandle(session.fileHandle)
        }
    }

    private fun openWinUsbBulkSession(device: RawHidDevice): WinUsbBulkSession {
        memScoped {
            val winusbGuid = allocWinUsbGuid()
            var matched: WinUsbBulkSession? = null
            forEachDeviceInterfacePath(winusbGuid.ptr) { path ->
                val session = tryOpenMatchingWinUsbSession(path, device.vendorId, device.productId, device.serial)
                if (session != null) {
                    matched = session
                    true
                } else {
                    false
                }
            }
            if (matched != null) {
                return matched
            }
        }
        error("Cannot find WinUSB interface 0 for device '${device.id}'")
    }

    private fun tryOpenMatchingWinUsbSession(
        devicePath: String,
        targetVendorId: Int,
        targetProductId: Int,
        targetSerial: String?,
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
        if (fileHandle == null || fileHandle == INVALID_HANDLE_VALUE) return null

        memScoped {
            val winUsbHandleVar = alloc<WINUSB_INTERFACE_HANDLEVar>()
            val initOk = WinUsb_Initialize(fileHandle, winUsbHandleVar.ptr)
            if (initOk == 0) {
                CloseHandle(fileHandle)
                return null
            }
            val winUsbHandle = winUsbHandleVar.value
            if (winUsbHandle == null) {
                CloseHandle(fileHandle)
                return null
            }

            // Read USB device descriptor (18 bytes)
            val devDescBuf = allocArray<UByteVar>(18)
            val transferred = alloc<UIntVar>()
            val descOk =
                WinUsb_GetDescriptor(
                    InterfaceHandle = winUsbHandle,
                    DescriptorType = USB_DEVICE_DESCRIPTOR_TYPE,
                    Index = 0u,
                    LanguageID = 0u,
                    Buffer = devDescBuf,
                    BufferLength = 18u,
                    LengthTransferred = transferred.ptr,
                )
            if (descOk == 0) {
                WinUsb_Free(winUsbHandle)
                CloseHandle(fileHandle)
                return null
            }

            val vid = (devDescBuf[9].toInt() shl 8) or devDescBuf[8].toInt()
            val pid = (devDescBuf[11].toInt() shl 8) or devDescBuf[10].toInt()
            val iSerial = devDescBuf[16].toInt()

            if (vid != targetVendorId || pid != targetProductId) {
                WinUsb_Free(winUsbHandle)
                CloseHandle(fileHandle)
                return null
            }

            var serial: String? = null
            if (iSerial > 0) {
                val strDescBuf = allocArray<UByteVar>(256)
                val strTransferred = alloc<UIntVar>()
                val strOk =
                    WinUsb_GetDescriptor(
                        InterfaceHandle = winUsbHandle,
                        DescriptorType = USB_STRING_DESCRIPTOR_TYPE,
                        Index = iSerial.toUByte(),
                        LanguageID = 0x0409u,
                        Buffer = strDescBuf,
                        BufferLength = 256u,
                        LengthTransferred = strTransferred.ptr,
                    )
                if (strOk != 0 && strTransferred.value > 2u) {
                    val strLenBytes = (strTransferred.value.toInt() - 2)
                    val wcharCount = strLenBytes / 2
                    val wcharPtr = (strDescBuf + 2)!!.reinterpret<UShortVar>()
                    val chars = CharArray(wcharCount) { wcharPtr[it].toInt().toChar() }
                    serial = chars.concatToString().trim().ifEmpty { null }
                }
            }

            if (targetSerial != null && serial != targetSerial) {
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
            val cbSize = if (sizeOf<CPointerVar<*>>() == 8L) 8u else 6u
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
