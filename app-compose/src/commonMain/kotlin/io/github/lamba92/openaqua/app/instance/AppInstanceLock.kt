package io.github.lamba92.openaqua.app.instance

import kotlinx.io.files.Path

expect class AppInstanceLock {
    fun release()

    companion object {
        fun tryAcquire(path: Path): AppInstanceLock?
    }
}

expect fun platformSleep(millis: Long)
