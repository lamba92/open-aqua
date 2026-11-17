package io.github.lamba92.openaqua.app.permission

import co.touchlab.kermit.Logger
import io.github.lamba92.openaqua.app.util.exists
import io.github.lamba92.openaqua.app.util.toJavaPath
import io.github.lamba92.openaqua.app.util.useLines
import kotlinx.io.files.Path
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

internal actual fun checkDeviceNodesAccessible(nodes: List<Path>): Boolean =
    nodes.all { node ->
        val javaPath = node.toJavaPath()
        Files.isReadable(javaPath) && Files.isWritable(javaPath)
    }

internal actual fun runElevatedUdevScript(
    script: String,
    logger: Logger,
): Boolean {
    val noNewPrivs = isNoNewPrivs()
    val hasFlatpakSpawn = isCommandAvailable("flatpak-spawn")
    val hasPkexec = isCommandAvailable("pkexec")
    val hasKdesu = isCommandAvailable("kdesu")

    val commandsToTry =
        buildList {
            if (noNewPrivs && hasFlatpakSpawn) {
                add(listOf("flatpak-spawn", "--host", "pkexec", "sh", "-c", script))
                if (hasKdesu) {
                    add(listOf("flatpak-spawn", "--host", "kdesu", "--", "sh", "-c", script))
                }
            }
            if (hasPkexec) {
                add(listOf("pkexec", "sh", "-c", script))
            }
            if (hasKdesu) {
                add(listOf("kdesu", "--", "sh", "-c", script))
            }
            if (hasFlatpakSpawn && !noNewPrivs) {
                add(listOf("flatpak-spawn", "--host", "pkexec", "sh", "-c", script))
                if (hasKdesu) {
                    add(listOf("flatpak-spawn", "--host", "kdesu", "--", "sh", "-c", script))
                }
            }
        }

    for (cmd in commandsToTry) {
        logger.i { "Attempting elevated command with: ${cmd.joinToString(" ")}" }
        val exitCode = runElevatedCommand(cmd, logger)
        if (exitCode == 0) return true
    }
    return false
}

private fun runElevatedCommand(
    command: List<String>,
    logger: Logger,
): Int =
    runCatching {
        val process = ProcessBuilder(command).start()
        val stdoutFuture =
            CompletableFuture.supplyAsync {
                process.inputStream.bufferedReader().readText()
            }
        val stderrFuture =
            CompletableFuture.supplyAsync {
                process.errorStream.bufferedReader().readText()
            }
        val finished = process.waitFor(60, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            logger.w { "${command.joinToString(" ")} timed out after 60 seconds" }
            return@runCatching -1
        }
        val exitCode = process.exitValue()
        val stdout = stdoutFuture.get(2, TimeUnit.SECONDS)
        val stderr = stderrFuture.get(2, TimeUnit.SECONDS)
        if (exitCode != 0) {
            logger.w {
                "${command.joinToString(" ")} failed with exit code $exitCode.\nStdout: $stdout\nStderr: $stderr"
            }
        }
        exitCode
    }.getOrElse { e ->
        logger.w(e) { "Failed to execute ${command.joinToString(" ")}" }
        -1
    }

private fun isNoNewPrivs(): Boolean =
    runCatching {
        Path("/proc/self/status").useLines { lines ->
            lines.any { it.startsWith("NoNewPrivs:") && it.contains("1") }
        }
    }.getOrDefault(false)

private fun isCommandAvailable(command: String): Boolean {
    val pathDirs = System.getenv("PATH")?.split(":") ?: listOf("/usr/bin", "/bin", "/usr/local/bin")
    return pathDirs.any { dir ->
        Path(dir, command).exists()
    }
}
