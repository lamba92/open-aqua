package io.github.lamba92.openaqua.engine.util

import kotlinx.io.buffered
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString

fun Path.readText(fileSystem: FileSystem = SystemFileSystem): String = fileSystem.source(this).buffered().use { it.readString() }

fun Path.readTextOrNull(fileSystem: FileSystem = SystemFileSystem): String? =
    try {
        if (!fileSystem.exists(this)) {
            null
        } else {
            fileSystem.source(this).buffered().use { it.readString() }
        }
    } catch (_: Exception) {
        null
    }

fun Path.writeText(
    text: String,
    fileSystem: FileSystem = SystemFileSystem,
) {
    fileSystem.sink(this).buffered().use { it.write(text.encodeToByteArray()) }
}

fun Path.atomicWriteText(
    text: String,
    fileSystem: FileSystem = SystemFileSystem,
) {
    val tempPath = Path("$this.tmp")
    tempPath.writeText(text, fileSystem)
    fileSystem.atomicMove(tempPath, this)
}

fun Path.listDirectory(fileSystem: FileSystem = SystemFileSystem): List<Path> =
    if (!fileSystem.exists(this)) {
        emptyList()
    } else {
        try {
            fileSystem.list(this).toList()
        } catch (_: Exception) {
            emptyList()
        }
    }

fun Path.exists(fileSystem: FileSystem = SystemFileSystem): Boolean = fileSystem.exists(this)

fun Path.delete(fileSystem: FileSystem = SystemFileSystem) {
    fileSystem.delete(this)
}

fun Path.deleteIfExists(fileSystem: FileSystem = SystemFileSystem): Boolean =
    try {
        if (fileSystem.exists(this)) {
            fileSystem.delete(this)
            true
        } else {
            false
        }
    } catch (_: Throwable) {
        false
    }

fun Path.createDirectories(
    mustCreate: Boolean = false,
    fileSystem: FileSystem = SystemFileSystem,
) {
    fileSystem.createDirectories(this, mustCreate)
}

fun Path.resolve(child: String): Path = Path(this, child)

val Path.nameWithoutExtension: String
    get() = name.substringBeforeLast('.', missingDelimiterValue = name)
