package io.github.lamba92.openaqua.app.util

import co.touchlab.kermit.Logger
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeBytes

@JvmInline
value class DeviceEnginePath(
    val path: Path,
)

@JvmInline
value class HostEnginePath(
    val path: Path,
)

data class ExtractedEngines(
    val deviceEngine: DeviceEnginePath,
    val hostEngine: HostEnginePath,
)

object EngineExtractor {
    fun extract(logger: Logger): ExtractedEngines {
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

        val deviceBinary = extractBinary("open-aqua-device-engine", isWindows, targetSuffix)
        val hostBinary = extractBinary("open-aqua-host-engine", isWindows, targetSuffix)

        logger.i {
            "Extracted engine binaries: device='${deviceBinary.fileName}', host='${hostBinary.fileName}'"
        }

        return ExtractedEngines(
            deviceEngine = DeviceEnginePath(deviceBinary),
            hostEngine = HostEnginePath(hostBinary),
        )
    }

    private fun extractBinary(
        prefix: String,
        isWindows: Boolean,
        targetSuffix: String,
    ): Path {
        val resourceFileName = "$prefix-$targetSuffix"
        val tempDir = createTempDirectory(prefix)
        tempDir.toFile().deleteOnExit()

        val binaryName = if (isWindows) "$prefix.exe" else prefix
        val targetExecutable = tempDir.resolve(binaryName)
        targetExecutable.toFile().deleteOnExit()

        val binaryBytes =
            EngineExtractor::class.java.classLoader
                .getResourceAsStream("files/bin/$resourceFileName")
                ?.use { it.readAllBytes() }
                ?: EngineExtractor::class.java
                    .getResourceAsStream("/files/bin/$resourceFileName")
                    ?.use { it.readAllBytes() }
                ?: error("Could not find engine binary on classpath: files/bin/$resourceFileName")

        targetExecutable.writeBytes(binaryBytes)
        targetExecutable.toFile().setExecutable(true)

        return targetExecutable
    }
}
