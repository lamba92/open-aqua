package io.github.lamba92.openaqua.app.model

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.io.files.Path
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
internal actual fun defaultBaseAppDataDir(): BaseAppDataDir {
    val appData = getenv("APPDATA")?.toKString()?.takeIf { it.isNotBlank() }
    val baseDir =
        if (appData != null) {
            Path(appData)
        } else {
            val userProfile = getenv("USERPROFILE")?.toKString() ?: "."
            Path(userProfile, "AppData", "Roaming")
        }
    return BaseAppDataDir(Path(baseDir, "open-aqua"))
}
