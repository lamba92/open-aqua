@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)

package io.github.lamba92.openaqua.app.util

import co.touchlab.kermit.Logger
import kotlinx.cinterop.toKString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import platform.posix.getenv
import kotlin.random.Random

actual fun startProcess(command: List<String>): PlatformProcess = startNativeProcess(command)

internal expect fun startNativeProcess(command: List<String>): PlatformProcess

internal expect fun getExecutableDirectory(): Path?

actual fun extractEngineBinary(
    prefix: String,
    logger: Logger?,
): Path {
    val isWindows = Platform.osFamily == OsFamily.WINDOWS
    val binaryName = if (isWindows) "$prefix.exe" else prefix

    val candidates = mutableListOf<Path>()

    getExecutableDirectory()?.let { appDir ->
        candidates += Path(appDir, binaryName)
        candidates += Path(appDir, "bin", binaryName)
        candidates += Path(appDir, "files", "bin", binaryName)
    }

    candidates += Path(binaryName)
    candidates += Path("bin", binaryName)
    candidates += Path("files", "bin", binaryName)

    if (isWindows) {
        candidates += Path("engine/engine-host/engine-host-windows/build/publish/win-x64", binaryName)
    }

    getSystemPathDirectories().forEach { pathDir ->
        candidates += Path(pathDir, binaryName)
    }

    for (candidate in candidates) {
        if (candidate.exists()) {
            logger?.i { "Found engine binary for $prefix at '$candidate'" }
            return candidate
        }
    }

    error("Could not find engine executable '$binaryName'. Checked: ${candidates.joinToString()}")
}

actual fun createTempDirectoryPath(prefix: String): Path {
    val baseTemp = getSystemTempDirectory()
    val randomPart = Random.nextLong().toString(16)
    val dirPath = Path(baseTemp, "$prefix$randomPart")
    SystemFileSystem.createDirectories(dirPath)
    return dirPath
}

actual fun createTempFilePath(
    prefix: String,
    suffix: String,
): Path {
    val baseTemp = getSystemTempDirectory()
    val randomPart = Random.nextLong().toString(16)
    return Path(baseTemp, "$prefix$randomPart$suffix")
}

private fun getSystemTempDirectory(): Path {
    val env =
        getenv("TEMP")?.let { it.toKString() }
            ?: getenv("TMP")?.let { it.toKString() }
            ?: getenv("TMPDIR")?.let { it.toKString() }
            ?: "/tmp"
    return Path(env)
}

private fun getSystemPathDirectories(): List<Path> {
    val rawPath = getenv("PATH")?.let { it.toKString() } ?: return emptyList()
    val separator = if (Platform.osFamily == OsFamily.WINDOWS) ';' else ':'
    return rawPath.split(separator).filter { it.isNotBlank() }.map { Path(it) }
}
