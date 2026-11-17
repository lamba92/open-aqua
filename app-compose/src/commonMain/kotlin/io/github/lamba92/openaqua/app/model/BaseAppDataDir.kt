package io.github.lamba92.openaqua.app.model

import io.github.lamba92.openaqua.app.util.createDirectories
import io.github.lamba92.openaqua.app.util.resolve
import kotlinx.io.files.Path
import kotlin.jvm.JvmInline

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
        fun default(): BaseAppDataDir = defaultBaseAppDataDir()
    }
}

internal expect fun defaultBaseAppDataDir(): BaseAppDataDir
