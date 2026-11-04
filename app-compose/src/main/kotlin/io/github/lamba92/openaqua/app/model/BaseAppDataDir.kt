package io.github.lamba92.openaqua.app.model

import io.github.lamba92.openaqua.app.util.createDirectories
import io.github.lamba92.openaqua.app.util.resolve
import kotlinx.io.files.Path

@JvmInline
value class AppConfigFile(
    val path: Path,
)

@JvmInline
value class AppLockFile(
    val path: Path,
)

@JvmInline
value class AppPortFile(
    val path: Path,
)

@JvmInline
value class BaseAppDataDir(
    val path: Path,
) {
    val logsDirectory: LogsDirectory
        get() = LogsDirectory(path.resolve("logs"))

    val configFile: AppConfigFile
        get() = AppConfigFile(path.resolve("config.json"))

    val lockFile: AppLockFile
        get() = AppLockFile(path.resolve("open-aqua.lock"))

    val portFile: AppPortFile
        get() = AppPortFile(path.resolve("open-aqua.port"))

    init {
        path.createDirectories()
    }

    companion object {
        private const val APP_DIR_NAME = "open-aqua"

        fun default(): BaseAppDataDir {
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
            return BaseAppDataDir(Path(baseDir, APP_DIR_NAME))
        }
    }
}
