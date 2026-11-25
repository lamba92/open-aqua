package io.github.lamba92.openaqua.app.model

import io.github.lamba92.openaqua.kotlinx.io.createDirectories
import io.github.lamba92.openaqua.kotlinx.io.resolve
import kotlinx.io.files.Path
import kotlin.jvm.JvmInline

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
