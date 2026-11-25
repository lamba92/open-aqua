package io.github.lamba92.openaqua.app.instance

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.files.Path
import platform.windows.CloseHandle
import platform.windows.CreateFileW
import platform.windows.FILE_ATTRIBUTE_NORMAL
import platform.windows.FILE_SHARE_READ
import platform.windows.GENERIC_READ
import platform.windows.GENERIC_WRITE
import platform.windows.HANDLE
import platform.windows.INVALID_HANDLE_VALUE
import platform.windows.OPEN_ALWAYS
import platform.windows.Sleep

@OptIn(ExperimentalForeignApi::class)
actual class AppInstanceLock private constructor(
    private val handle: HANDLE,
) {
    actual fun release() {
        CloseHandle(handle)
    }

    actual companion object {
        actual fun tryAcquire(path: Path): AppInstanceLock? {
            val handle =
                CreateFileW(
                    lpFileName = path.toString(),
                    dwDesiredAccess = GENERIC_READ or GENERIC_WRITE.toUInt(),
                    dwShareMode = FILE_SHARE_READ.toUInt(),
                    lpSecurityAttributes = null,
                    dwCreationDisposition = OPEN_ALWAYS.toUInt(),
                    dwFlagsAndAttributes = FILE_ATTRIBUTE_NORMAL.toUInt(),
                    hTemplateFile = null,
                )
            if (handle == null || handle == INVALID_HANDLE_VALUE) return null
            return AppInstanceLock(handle)
        }
    }
}

actual fun platformSleep(millis: Long) {
    Sleep(millis.toUInt())
}
