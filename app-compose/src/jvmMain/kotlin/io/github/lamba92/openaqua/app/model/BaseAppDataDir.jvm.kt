package io.github.lamba92.openaqua.app.model

import kotlinx.io.files.Path

internal actual fun defaultBaseAppDataDir(): BaseAppDataDir {
    val os = System.getProperty("os.name").lowercase()
    val userHome = System.getProperty("user.home") ?: "."
    val baseDir =
        when {
            os.contains("win") -> {
                val appData = System.getenv("APPDATA")?.takeIf { it.isNotBlank() }
                if (appData != null) Path(appData) else Path(userHome, "AppData", "Roaming")
            }

            os.contains("mac") -> {
                Path(userHome, "Library", "Application Support")
            }

            else -> {
                val xdgConfig = System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }
                if (xdgConfig != null) Path(xdgConfig) else Path(userHome, ".config")
            }
        }
    return BaseAppDataDir(Path(baseDir, "open-aqua"))
}
