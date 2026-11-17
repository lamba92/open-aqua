package io.github.lamba92.openaqua.app.model

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.io.files.Path
import platform.posix.getenv
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
internal actual fun defaultBaseAppDataDir(): BaseAppDataDir {
    val home = getenv("HOME")?.toKString() ?: "."
    val baseDir =
        when (Platform.osFamily) {
            OsFamily.WINDOWS -> {
                val appData = getenv("APPDATA")?.toKString()?.takeIf { it.isNotBlank() }
                if (appData != null) {
                    Path(appData)
                } else {
                    val userProfile = getenv("USERPROFILE")?.toKString() ?: "."
                    Path(userProfile, "AppData", "Roaming")
                }
            }

            OsFamily.MACOSX -> {
                Path(home, "Library", "Application Support")
            }

            else -> {
                val xdgConfig = getenv("XDG_CONFIG_HOME")?.toKString()?.takeIf { it.isNotBlank() }
                if (xdgConfig != null) Path(xdgConfig) else Path(home, ".config")
            }
        }
    return BaseAppDataDir(Path(baseDir, "open-aqua"))
}
