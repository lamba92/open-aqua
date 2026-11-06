package io.github.lamba92.openaqua.app.permission

import co.touchlab.kermit.Logger
import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.model.every
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class WindowsDevicePermissionService(
    scopeProvider: CoroutineScopeProvider,
) : DevicePermissionService {
    private val scope = scopeProvider.getScope(Dispatchers.IO)

    override val state: StateFlow<DevicePermissionState>
        field = MutableStateFlow<DevicePermissionState>(DevicePermissionState.Initializing)

    private val logger
        get() = scope.logger

    init {
        scope.every(2.seconds) {
            checkPermissionsInternal()
        }
    }

    private fun checkPermissionsInternal() {
        val isElevated =
            runCatching {
                val process =
                    ProcessBuilder("net", "session")
                        .redirectErrorStream(true)
                        .start()
                process.waitFor() == 0
            }.getOrDefault(false)

        val newState =
            when {
                isElevated -> DevicePermissionState.Granted
                else -> DevicePermissionState.Denied.WindowsElevationRequired
            }

        state.update { current ->
            if (current != newState) {
                logger.i { "Windows admin permission state changed from $current to $newState" }
            }
            newState
        }
    }

    override fun requestPermissions() {
        scope.launch {
            val currentExecutable =
                ProcessHandle
                    .current()
                    .info()
                    .command()
                    .orElse(null)

            when {
                currentExecutable != null -> {
                    logger.i { "Requesting elevation for executable: $currentExecutable" }
                    val process =
                        ProcessBuilder(
                            "powershell",
                            "-NoProfile",
                            "-Command",
                            "Start-Process -FilePath '$currentExecutable' -Verb RunAs",
                        ).start()
                    process.waitFor()
                }

                else -> {
                    error("Unable to determine current executable path for elevation")
                }
            }

            checkPermissionsInternal()
        }
    }
}
