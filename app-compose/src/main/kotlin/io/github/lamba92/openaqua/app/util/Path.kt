package io.github.lamba92.openaqua.app.util

import kotlinx.io.buffered
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readLine
import kotlinx.io.readString
import kotlinx.io.writeString
import java.awt.Desktop
import java.io.File
import java.nio.file.Path as JavaPath

fun Path.resolve(child: String): Path = Path(this, child)

fun Path.resolveSibling(name: String): Path {
    val parent = parent ?: error("Path has no parent: $this")
    return Path(parent, name)
}

fun Path.exists(fileSystem: FileSystem = SystemFileSystem): Boolean = fileSystem.exists(this)

fun Path.createDirectories(
    mustCreate: Boolean = false,
    fileSystem: FileSystem = SystemFileSystem,
) {
    fileSystem.createDirectories(this, mustCreate)
}

inline fun <T> Path.useLines(
    fileSystem: FileSystem = SystemFileSystem,
    block: (Sequence<String>) -> T,
): T =
    fileSystem.source(this).buffered().use { source ->
        block(generateSequence { source.readLine() })
    }

fun Path.openInDesktopEnvironment() {
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
        Desktop.getDesktop().open(File(toString()))
    } else {
        val os = System.getProperty("os.name").lowercase()
        val cmd =
            when {
                os.contains("mac") -> listOf("open", toString())
                os.contains("win") -> listOf("explorer", toString())
                else -> listOf("xdg-open", toString())
            }
        ProcessBuilder(cmd).start()
    }
}

fun Path.toJavaPath(): JavaPath = JavaPath.of(toString())

fun JavaPath.toKotlinxPath(): Path = Path(toString())

fun Path.readText(fileSystem: FileSystem = SystemFileSystem): String = fileSystem.source(this).buffered().use { it.readString() }

fun Path.writeText(
    text: String,
    fileSystem: FileSystem = SystemFileSystem,
) {
    fileSystem.sink(this, append = false).buffered().use { it.writeString(text) }
}

fun Path.delete(fileSystem: FileSystem = SystemFileSystem) {
    fileSystem.delete(this)
}

fun Path.deleteIfExists(fileSystem: FileSystem = SystemFileSystem) {
    runCatching { fileSystem.delete(this) }
}

fun Path.atomicMove(
    destination: Path,
    fileSystem: FileSystem = SystemFileSystem,
) {
    fileSystem.atomicMove(this, destination)
}

val Path.nameWithoutExtension: String
    get() = name.substringBeforeLast('.', missingDelimiterValue = name)
