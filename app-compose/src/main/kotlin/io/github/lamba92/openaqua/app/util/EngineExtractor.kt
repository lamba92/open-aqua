package io.github.lamba92.openaqua.app.util

import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeBytes

object EngineExtractor {
    suspend fun extract(): Path {
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()

        val isLinux = os.contains("linux")
        val isWindows = os.contains("win")
        val isMac = os.contains("mac")
        val isX64 = arch == "x86_64" || arch == "amd64" || arch == "x64"
        val isArm64 = arch == "aarch64" || arch == "arm64"

        val resourceFileName =
            when {
                isLinux && isX64 -> "open-aqua-engine-linux-x64"
                isLinux && isArm64 -> "open-aqua-engine-linux-arm64"
                isWindows && isX64 -> "open-aqua-engine-windows-x64.exe"
                isMac && isArm64 -> "open-aqua-engine-macos-arm64"
                isMac && isX64 -> "open-aqua-engine-macos-x64"
                else -> error("Unsupported operating system or architecture: os=$os, arch=$arch")
            }

        val tempDir = createTempDirectory("open-aqua-engine")
        tempDir.toFile().deleteOnExit()

        val binaryName = if (isWindows) "open-aqua-engine.exe" else "open-aqua-engine"
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
