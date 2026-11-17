package io.github.lamba92.openaqua.app.util

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.readLine
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.io.path.writeBytes

private class JvmPlatformProcess(
    private val process: Process,
) : PlatformProcess {
    private val stdoutReader = process.inputStream.asSource().buffered()
    private val stdinWriter: BufferedWriter by lazy {
        BufferedWriter(OutputStreamWriter(process.outputStream))
    }

    override val pid: Long get() = process.pid()
    override val isAlive: Boolean get() = process.isAlive
    override val exitCode: Int? get() = if (!process.isAlive) process.exitValue() else null

    override suspend fun readLine(): String? =
        withContext(Dispatchers.IO) {
            stdoutReader.readLine()
        }

    override suspend fun readStderr(): String =
        withContext(Dispatchers.IO) {
            process.errorStream.bufferedReader().readText()
        }

    override suspend fun writeLine(line: String) =
        withContext(Dispatchers.IO) {
            stdinWriter.write(line)
            stdinWriter.newLine()
        }

    override suspend fun flushInput() =
        withContext(Dispatchers.IO) {
            stdinWriter.flush()
        }

    override suspend fun closeInput() =
        withContext(Dispatchers.IO) {
            stdinWriter.close()
        }

    override suspend fun waitFor(): Int =
        withContext(Dispatchers.IO) {
            process.waitFor()
        }

    override fun destroy() {
        process.destroyForcibly()
    }
}

actual fun startProcess(command: List<String>): PlatformProcess = JvmPlatformProcess(ProcessBuilder(command).start())

actual fun extractEngineBinary(
    prefix: String,
    logger: Logger?,
): Path {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()

    val isLinux = os.contains("linux")
    val isWindows = os.contains("win")
    val isMac = os.contains("mac")
    val isX64 = arch == "x86_64" || arch == "amd64" || arch == "x64"
    val isArm64 = arch == "aarch64" || arch == "arm64"

    val targetSuffix =
        when {
            isLinux && isX64 -> "linux-x64"
            isLinux && isArm64 -> "linux-arm64"
            isWindows && isX64 -> "windows-x64.exe"
            isMac && isArm64 -> "macos-arm64"
            isMac && isX64 -> "macos-x64"
            else -> error("Unsupported operating system or architecture: os=$os, arch=$arch")
        }

    val resourceFileName = "$prefix-$targetSuffix"
    val tempDir = Files.createTempDirectory(prefix)
    tempDir.toFile().deleteOnExit()

    val binaryName = if (isWindows) "$prefix.exe" else prefix
    val targetExecutable = tempDir.resolve(binaryName)
    targetExecutable.toFile().deleteOnExit()

    val binaryBytes =
        Thread
            .currentThread()
            .contextClassLoader
            ?.getResourceAsStream("files/bin/$resourceFileName")
            ?.use { it.readAllBytes() }
            ?: object {}
                .javaClass
                .getResourceAsStream("/files/bin/$resourceFileName")
                ?.use { it.readAllBytes() }
            ?: error("Could not find engine binary on classpath: files/bin/$resourceFileName")

    targetExecutable.writeBytes(binaryBytes)
    targetExecutable.toFile().setExecutable(true)

    logger?.i { "Extracted $prefix binary: '${targetExecutable.fileName}'" }
    return Path(targetExecutable.absolutePathString())
}

actual fun createTempDirectoryPath(prefix: String): Path =
    Path(
        Files
            .createTempDirectory(prefix)
            .toFile()
            .apply { deleteOnExit() }
            .absolutePath,
    )

actual fun createTempFilePath(
    prefix: String,
    suffix: String,
): Path =
    Path(
        Files
            .createTempFile(prefix, suffix)
            .toFile()
            .apply { deleteOnExit() }
            .absolutePath,
    )
