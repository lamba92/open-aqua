@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)

package io.github.lamba92.openaqua.app.permission

import co.touchlab.kermit.Logger
import kotlinx.io.files.Path
import platform.posix.access
import platform.posix.system

internal actual fun checkDeviceNodesAccessible(nodes: List<Path>): Boolean =
    Platform.osFamily != OsFamily.LINUX ||
        nodes.all { node ->
            // R_OK (4) | W_OK (2) = 6
            access(node.toString(), 6) == 0
        } // R_OK (4) | W_OK (2) = 6

internal actual fun runElevatedUdevScript(
    script: String,
    logger: Logger,
): Boolean {
    if (Platform.osFamily != OsFamily.LINUX) return true
    val escapedScript = script.replace("'", "'\\''")
    val commandsToTry =
        listOf(
            "pkexec sh -c '$escapedScript'",
            "flatpak-spawn --host pkexec sh -c '$escapedScript'",
            "kdesu -- sh -c '$escapedScript'",
        )

    for (cmd in commandsToTry) {
        logger.i { "Attempting elevated command with: $cmd" }
        if (system(cmd) == 0) {
            logger.i { "Successfully executed elevated command" }
            return true
        }
    }
    return false
}
