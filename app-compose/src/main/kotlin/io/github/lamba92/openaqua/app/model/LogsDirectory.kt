package io.github.lamba92.openaqua.app.model

import io.github.lamba92.openaqua.app.util.createDirectories
import io.github.lamba92.openaqua.app.util.resolve
import kotlinx.io.files.Path

@JvmInline
value class LogsDirectory(
    val value: Path,
) {
    val logFile: Path
        get() = value.resolve("open-aqua.log")

    init {
        value.createDirectories()
    }
}
