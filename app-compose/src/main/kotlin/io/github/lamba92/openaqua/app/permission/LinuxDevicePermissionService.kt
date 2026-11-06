package io.github.lamba92.openaqua.app.permission

import co.touchlab.kermit.Logger
import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.util.exists
import io.github.lamba92.openaqua.app.util.listDirectory
import io.github.lamba92.openaqua.app.util.readText
import io.github.lamba92.openaqua.app.util.resolve
import io.github.lamba92.openaqua.app.util.toJavaPath
import io.github.lamba92.openaqua.app.util.useLines
import io.github.lamba92.openaqua.model.device.DeviceRegistry
import io.github.lamba92.openaqua.model.every
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

val LINUX_UDEV_RULE_INSTALL_SCRIPT =
    $$"""mkdir -p /etc/udev/rules.d && cat > /etc/udev/rules.d/60-open-aqua.rules << 'EOF'
KERNEL=="hidraw*", ATTRS{idVendor}=="0c70", MODE="0666", TAG+="uaccess"
SUBSYSTEM=="usb", ATTRS{idVendor}=="0c70", MODE="0666", TAG+="uaccess"
EOF
rm -f /etc/udev/rules.d/70-open-aqua.rules
udevadm control --reload-rules
udevadm trigger --action=change --subsystem-match=hidraw || true
udevadm trigger --action=add --subsystem-match=hidraw || true
for dev in /sys/class/hidraw/hidraw*; do
    if [ -f "$dev/device/uevent" ] && grep -qi "0c70" "$dev/device/uevent"; then
        chmod 666 "/dev/$(basename "$dev")" 2>/dev/null || true
    fi
done"""

val LINUX_UDEV_RULE_UNINSTALL_SCRIPT =
    "rm -f /etc/udev/rules.d/60-open-aqua.rules /etc/udev/rules.d/70-open-aqua.rules && udevadm control --reload-rules && udevadm trigger --action=change --subsystem-match=hidraw || true"

class LinuxDevicePermissionService(
    scopeProvider: CoroutineScopeProvider,
    private val logger: Logger,
) : DevicePermissionService {
    private val scope = scopeProvider.getScope(Dispatchers.IO)

    override val state: StateFlow<DevicePermissionState>
        field = MutableStateFlow<DevicePermissionState>(DevicePermissionState.Initializing)

    init {
        scope.every(2.seconds) {
            checkPermissionsInternal()
        }
    }

    private fun checkPermissionsInternal() {
        val ruleFile = Path("/etc/udev/rules.d/60-open-aqua.rules")
        val hasRule =
            ruleFile.exists() &&
                runCatching { ruleFile.readText() }.getOrNull()?.contains("0c70", ignoreCase = true) == true

        val detectedAquaDevNodes = findAquaHidrawDeviceNodes()
        val newState =
            when {
                !hasRule -> DevicePermissionState.Denied.LinuxUdevRulesMissing
                detectedAquaDevNodes.isNotEmpty() -> checkDeviceNodesPermission(detectedAquaDevNodes)
                else -> DevicePermissionState.Granted
            }

        state.update { current ->
            if (current != newState) {
                logger.i { "Linux device permission state changed from $current to $newState" }
            }
            newState
        }
    }

    private fun checkDeviceNodesPermission(nodes: List<Path>): DevicePermissionState {
        val allAccessible =
            nodes.all { node ->
                val javaPath = node.toJavaPath()
                Files.isReadable(javaPath) && Files.isWritable(javaPath)
            }
        return if (allAccessible) {
            DevicePermissionState.Granted
        } else {
            DevicePermissionState.Denied.LinuxUdevRulesMissing
        }
    }

    private fun findAquaHidrawDeviceNodes(): List<Path> {
        val hidrawDir = Path("/sys/class/hidraw")
        if (!hidrawDir.exists()) return emptyList()

        return buildList {
            for (devDir in hidrawDir.listDirectory()) {
                if (isAquaHidrawDirectory(devDir)) {
                    add(Path("/dev", devDir.name))
                }
            }
        }
    }

    private fun isAquaHidrawDirectory(devDir: Path): Boolean {
        val uevent = devDir.resolve("device/uevent")
        return uevent.exists() &&
            runCatching {
                uevent.useLines { lines -> lines.any(::isAquaHidIdLine) }
            }.getOrDefault(false)
    }

    private fun isAquaHidIdLine(line: String): Boolean {
        val trimmed = line.trim()
        if (!trimmed.startsWith("HID_ID=")) return false

        val parts = trimmed.substringAfter("HID_ID=").split(":")
        if (parts.size < 3) return false

        val vendorId = parts[1].toIntOrNull(16)
        val productId = parts[2].toIntOrNull(16) ?: return false

        return vendorId == DeviceRegistry.AQUACOMPUTER_VENDOR_ID &&
            DeviceRegistry.findByProductId(productId) != null
    }

    override fun requestPermissions() {
        executeElevatedScript(LINUX_UDEV_RULE_INSTALL_SCRIPT, "installation")
    }

    override fun removePermissions() {
        executeElevatedScript(LINUX_UDEV_RULE_UNINSTALL_SCRIPT, "removal")
    }

    private fun executeElevatedScript(
        script: String,
        actionName: String,
    ) = scope.launch {
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

        if (commandsToTry.isEmpty()) {
            error("No elevation tool (pkexec, flatpak-spawn, kdesu) found")
        }

        var succeeded = false
        for (cmd in commandsToTry) {
            logger.i { "Attempting $actionName with: ${cmd.joinToString(" ")}" }
            val exitCode = runElevatedCommand(cmd)
            if (exitCode == 0) {
                logger.i { "Successfully executed $actionName via ${cmd.first()}" }
                succeeded = true
                break
            }
        }

        if (!succeeded) {
            logger.w { "All $actionName attempts failed" }
        }
        checkPermissionsInternal()
    }

    private fun runElevatedCommand(command: List<String>): Int =
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
}
