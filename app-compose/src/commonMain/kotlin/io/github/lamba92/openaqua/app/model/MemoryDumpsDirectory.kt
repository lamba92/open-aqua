package io.github.lamba92.openaqua.app.model

import io.github.lamba92.openaqua.kotlinx.io.createDirectories
import kotlinx.io.files.Path
import kotlin.jvm.JvmInline

@JvmInline
value class MemoryDumpsDirectory(
    val value: Path,
) {
    init {
        value.createDirectories()
    }
}
