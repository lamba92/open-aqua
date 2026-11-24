package io.github.lamba92.openaqua.app.model

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.io.files.Path
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
internal actual fun defaultBaseAppDataDir(): BaseAppDataDir {
    val home = getenv("HOME")?.toKString() ?: "."
    val xdgConfig = getenv("XDG_CONFIG_HOME")?.toKString()?.takeIf { it.isNotBlank() }
    val baseDir = if (xdgConfig != null) Path(xdgConfig) else Path(home, ".config")
    return BaseAppDataDir(Path(baseDir, "open-aqua"))
}
