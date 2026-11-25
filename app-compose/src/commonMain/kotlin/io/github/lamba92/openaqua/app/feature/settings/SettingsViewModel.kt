package io.github.lamba92.openaqua.app.feature.settings

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.LoggingScope
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.model.BaseAppDataDir
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.permission.DevicePermissionService
import io.github.lamba92.openaqua.app.permission.DevicePermissionState
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import io.github.lamba92.openaqua.app.util.openUrlInBrowser
import io.github.lamba92.openaqua.kotlinx.io.exists
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import kotlin.time.Duration.Companion.seconds

class SettingsViewModel(
    private val appDataRepository: AppDataRepository,
    private val permissionService: DevicePermissionService,
    val baseAppDataDir: BaseAppDataDir,
    scopeProvider: CoroutineScopeProvider,
) : MVIViewModel<SettingsState, SettingsEvent, SettingsEffect> {
    override val viewModelScope: LoggingScope = scopeProvider.getScope()

    override val state: StateFlow<SettingsState>
        field = MutableStateFlow<SettingsState>(SettingsState.Loading)

    override val effects: SharedFlow<SettingsEffect>
        field = MutableSharedFlow<SettingsEffect>()

    init {
        val readyFlow =
            appDataRepository.state
                .filterIsInstance<AppDataRepository.State.Ready>()

        combine(
            readyFlow,
            permissionService.state,
        ) { ready, permissions ->
            state.update { current ->
                when (current) {
                    SettingsState.Loading -> {
                        SettingsState.Ready(
                            osSettings = permissions.toCurrentOSSettings(),
                            storedData = ready.data,
                            dataFolderPath = baseAppDataDir.path.toString(),
                        )
                    }

                    is SettingsState.Ready -> {
                        val isRemoving = (current.osSettings as? CurrentOSSettings.Linux)?.isRemovingPermissions ?: false
                        current.copy(
                            osSettings = permissions.toCurrentOSSettings(isRemoving),
                            storedData = ready.data,
                        )
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    override fun onEvent(event: SettingsEvent): Job =
        viewModelScope.launch {
            when (event) {
                SettingsEvent.OpenDataFolder -> {
                    openUrlInBrowser(baseAppDataDir.path.toString())
                }

                SettingsEvent.RequestRestoreDefaults -> {
                    state.update { current ->
                        if (current is SettingsState.Ready) current.copy(showResetConfirmationDialog = true) else current
                    }
                }

                SettingsEvent.DismissRestoreDefaults -> {
                    state.update { current ->
                        if (current is SettingsState.Ready) current.copy(showResetConfirmationDialog = false) else current
                    }
                }

                SettingsEvent.ConfirmRestoreDefaults -> {
                    state.update { current ->
                        if (current is SettingsState.Ready) current.copy(showResetConfirmationDialog = false) else current
                    }
                    appDataRepository.resetToDefaults()
                }

                SettingsEvent.RequestRemoveUdevRule -> {
                    state.update { current ->
                        if (current is SettingsState.Ready) current.copy(showRemoveUdevRuleConfirmationDialog = true) else current
                    }
                }

                SettingsEvent.DismissRemoveUdevRule -> {
                    state.update { current ->
                        if (current is SettingsState.Ready) current.copy(showRemoveUdevRuleConfirmationDialog = false) else current
                    }
                }

                SettingsEvent.ConfirmRemoveUdevRule -> {
                    state.update { current ->
                        if (current is SettingsState.Ready) {
                            val linuxSettings = current.osSettings as? CurrentOSSettings.Linux
                            current.copy(
                                showRemoveUdevRuleConfirmationDialog = false,
                                osSettings = linuxSettings?.copy(isRemovingPermissions = true) ?: current.osSettings,
                            )
                        } else {
                            current
                        }
                    }
                    permissionService.removePermissions()
                    delay(3.seconds)
                    state.update { current ->
                        if (current is SettingsState.Ready) {
                            val linuxSettings = current.osSettings as? CurrentOSSettings.Linux
                            current.copy(
                                osSettings = linuxSettings?.copy(isRemovingPermissions = false) ?: current.osSettings,
                            )
                        } else {
                            current
                        }
                    }
                }

                is SettingsEvent.SelectColorScheme -> {
                    appDataRepository.update { it.copy(colorScheme = event.scheme) }
                }

                is SettingsEvent.SelectMetricsScale -> {
                    appDataRepository.update { it.copy(metricsScale = event.scale) }
                }

                is SettingsEvent.SetFontSp -> {
                    appDataRepository.update { it.copy(fontSp = event.sp) }
                }

                is SettingsEvent.SelectFontFamily -> {
                    appDataRepository.update { it.copy(fontFamily = event.family) }
                }

                is SettingsEvent.SelectLanguage -> {
                    appDataRepository.update { it.copy(language = event.language) }
                }

                is SettingsEvent.ToggleHideDisconnectedFans -> {
                    appDataRepository.update { it.copy(hideDisconnectedFans = event.hide) }
                }

                is SettingsEvent.ToggleHideDisconnectedSensorsInFans -> {
                    appDataRepository.update { it.copy(hideDisconnectedSensorsInFans = event.hide) }
                }

                is SettingsEvent.ToggleHideDisconnectedSensors -> {
                    appDataRepository.update { it.copy(hideDisconnectedSensors = event.hide) }
                }

                is SettingsEvent.ToggleHideUnconfiguredVirtualSensors -> {
                    appDataRepository.update { it.copy(hideUnconfiguredVirtualSensors = event.hide) }
                }

                is SettingsEvent.ToggleMinimizeToTrayOnClose -> {
                    appDataRepository.update { it.copy(minimizeToTrayOnClose = event.minimize) }
                }

                is SettingsEvent.ToggleStartMinimized -> {
                    appDataRepository.update { it.copy(startMinimized = event.startMinimized) }
                }
            }
        }
}

private fun DevicePermissionState.toCurrentOSSettings(isRemovingPermissions: Boolean = false): CurrentOSSettings =
    when {
        io.github.lamba92.openaqua.app.platform.PlatformInfo.osName
            .contains("linux", ignoreCase = true) -> {
            CurrentOSSettings.Linux(
                hasUdevRule = Path("/etc/udev/rules.d/60-open-aqua.rules").exists(),
                isRemovingPermissions = isRemovingPermissions,
            )
        }

        else -> {
            CurrentOSSettings.None
        }
    }
