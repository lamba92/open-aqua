package io.github.lamba92.openaqua.app.permission

import co.touchlab.kermit.Logger
import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.util.exists
import io.github.lamba92.openaqua.app.util.ioDispatcher
import io.github.lamba92.openaqua.app.util.listDirectory
import io.github.lamba92.openaqua.app.util.readText
import io.github.lamba92.openaqua.app.util.resolve
import io.github.lamba92.openaqua.app.util.useLines
import io.github.lamba92.openaqua.model.device.DeviceRegistry
import io.github.lamba92.openaqua.model.every
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import kotlin.time.Duration.Companion.seconds

internal expect fun checkDeviceNodesAccessible(nodes: List<Path>): Boolean

internal expect fun runElevatedUdevScript(
    script: String,
    logger: Logger,
): Boolean

class LinuxDevicePermissionService(
    scopeProvider: CoroutineScopeProvider,
    private val logger: Logger,
) : DevicePermissionService {
    private val scope = scopeProvider.getScope(ioDispatcher)

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

    private fun checkDeviceNodesPermission(nodes: List<Path>): DevicePermissionState =
        if (checkDeviceNodesAccessible(nodes)) {
            DevicePermissionState.Granted
        } else {
            DevicePermissionState.Denied.LinuxUdevRulesMissing
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
        logger.i { "Attempting $actionName" }
        val succeeded = runElevatedUdevScript(script, logger)
        if (!succeeded) {
            logger.w { "All $actionName attempts failed" }
        }
        checkPermissionsInternal()
    }
}
