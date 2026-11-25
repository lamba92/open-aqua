package io.github.lamba92.openaqua.app.instance

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.files.Path
import platform.posix.F_TLOCK
import platform.posix.F_ULOCK
import platform.posix.O_CREAT
import platform.posix.O_RDWR
import platform.posix.close
import platform.posix.lockf
import platform.posix.open
import platform.posix.usleep

@OptIn(ExperimentalForeignApi::class)
actual class AppInstanceLock private constructor(
    private val fd: Int,
) {
    actual fun release() {
        lockf(fd, F_ULOCK, 0)
        close(fd)
    }

    actual companion object {
        actual fun tryAcquire(path: Path): AppInstanceLock? {
            val fd = open(path.toString(), O_RDWR or O_CREAT, "0644".toInt(8))
            if (fd < 0) return null
            if (lockf(fd, F_TLOCK, 0) != 0) {
                close(fd)
                return null
            }
            return AppInstanceLock(fd)
        }
    }
}

actual fun platformSleep(millis: Long) {
    usleep((millis * 1000).toUInt())
}
