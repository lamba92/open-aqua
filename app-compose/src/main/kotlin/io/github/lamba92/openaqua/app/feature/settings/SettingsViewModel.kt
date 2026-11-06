package io.github.lamba92.openaqua.app.feature.settings

import co.touchlab.kermit.Severity
import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.LoggingScope
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.logging.AppLogService
import io.github.lamba92.openaqua.app.model.BaseAppDataDir
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import io.github.lamba92.openaqua.app.mvi.viewModelLogger
import io.github.lamba92.openaqua.app.permission.DevicePermissionService
import io.github.lamba92.openaqua.app.permission.DevicePermissionState
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import io.github.lamba92.openaqua.app.util.exists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import kotlin.time.Duration.Companion.seconds

class SettingsViewModel(
    private val appDataRepository: AppDataRepository,
    private val appLogService: AppLogService,
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

        val logsFlow =
            appLogService.logLines
                .map { it.toLogLines() }
                .flowOn(Dispatchers.Default)

        combine(
            readyFlow,
            logsFlow,
            permissionService.state,
        ) { ready, logs, permissions ->
            state.update { current ->
                when (current) {
                    SettingsState.Loading -> {
                        SettingsState.Ready(
                            osSettings = permissions.toCurrentOSSettings(),
                            storedData = ready.data,
                            logs = logs,
                            dataFolderPath = baseAppDataDir.path.toString(),
                        )
                    }

                    is SettingsState.Ready -> {
                        val isRemoving = (current.osSettings as? CurrentOSSettings.Linux)?.isRemovingPermissions ?: false
                        current.copy(
                            osSettings = permissions.toCurrentOSSettings(isRemoving),
                            storedData = ready.data,
                            logs = logs,
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
                    val current = state.value as? SettingsState.Ready ?: return@launch
                    viewModelLogger.i { "Opening app data folder: ${current.dataFolderPath}" }
                    appDataRepository.openDataFolder()
                }

                SettingsEvent.OpenLogsFolder -> {
                    viewModelLogger.i { "Opening logs folder: ${appLogService.logsDirectory.value}" }
                    appLogService.openLogsFolder()
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
                    viewModelLogger.i { "Restored settings to defaults" }
                    appDataRepository.resetToDefaults()
                    state.update { current ->
                        if (current is SettingsState.Ready) current.copy(showResetConfirmationDialog = false) else current
                    }
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
                    viewModelLogger.i { "Removing Linux udev rule" }
                    state.update { current ->
                        when (current) {
                            is SettingsState.Ready -> {
                                current.copy(
                                    showRemoveUdevRuleConfirmationDialog = false,
                                    osSettings =
                                        when (val currentOs = current.osSettings) {
                                            is CurrentOSSettings.Linux -> currentOs.copy(isRemovingPermissions = true)
                                            else -> currentOs
                                        },
                                )
                            }

                            else -> {
                                current
                            }
                        }
                    }
                    permissionService.removePermissions()
                    viewModelScope.launch {
                        delay(2.seconds)
                        state.update { current ->
                            when (current) {
                                is SettingsState.Ready if current.osSettings is CurrentOSSettings.Linux -> {
                                    current.copy(osSettings = current.osSettings.copy(isRemovingPermissions = false))
                                }

                                else -> {
                                    current
                                }
                            }
                        }
                    }
                }

                is SettingsEvent.SelectColorScheme -> {
                    viewModelLogger.i { "Selected color scheme: ${event.scheme}" }
                    appDataRepository.update { it.copy(colorScheme = event.scheme) }
                }

                is SettingsEvent.SelectMetricsScale -> {
                    viewModelLogger.i { "Selected metrics scale: ${event.scale}" }
                    appDataRepository.update { it.copy(metricsScale = event.scale) }
                }

                is SettingsEvent.SetFontSp -> {
                    viewModelLogger.i { "Updated font size: ${event.sp} sp" }
                    appDataRepository.update { it.copy(fontSp = event.sp) }
                }

                is SettingsEvent.SelectFontFamily -> {
                    viewModelLogger.i { "Selected font family: ${event.family}" }
                    appDataRepository.update { it.copy(fontFamily = event.family) }
                }

                is SettingsEvent.SelectLanguage -> {
                    viewModelLogger.i { "Selected language: ${event.language}" }
                    appDataRepository.update { it.copy(language = event.language) }
                }

                is SettingsEvent.ToggleHideDisconnectedFans -> {
                    viewModelLogger.i { "Set hide disconnected fans: ${event.hide}" }
                    appDataRepository.setHideDisconnectedFans(event.hide)
                }

                is SettingsEvent.ToggleHideDisconnectedSensorsInFans -> {
                    viewModelLogger.i { "Set hide disconnected sensors in fans: ${event.hide}" }
                    appDataRepository.setHideDisconnectedSensorsInFans(event.hide)
                }

                is SettingsEvent.ToggleHideDisconnectedSensors -> {
                    viewModelLogger.i { "Set hide disconnected sensors: ${event.hide}" }
                    appDataRepository.setHideDisconnectedSensors(event.hide)
                }

                is SettingsEvent.ToggleHideUnconfiguredVirtualSensors -> {
                    viewModelLogger.i { "Set hide unconfigured virtual sensors: ${event.hide}" }
                    appDataRepository.setHideUnconfiguredVirtualSensors(event.hide)
                }
            }
        }
}

private fun DevicePermissionState.toCurrentOSSettings(isRemovingPermissions: Boolean = false): CurrentOSSettings =
    when {
        System.getProperty("os.name").contains("linux", ignoreCase = true) -> {
            CurrentOSSettings.Linux(
                hasUdevRule = Path("/etc/udev/rules.d/60-open-aqua.rules").exists(),
                isRemovingPermissions = isRemovingPermissions,
            )
        }

        else -> {
            CurrentOSSettings.None
        }
    }

private val TIMESTAMP_PREFIX_REGEX = Regex("""^\d{4}-\d{2}-\d{2}[T\s]""")

private fun detectHeaderSeverity(trimmed: String): Severity? =
    when {
        trimmed.contains("[ERROR", ignoreCase = true) ||
            trimmed.contains("Error:", ignoreCase = true) -> Severity.Error

        trimmed.contains("[ASSERT", ignoreCase = true) ||
            trimmed.contains("Assert:", ignoreCase = true) -> Severity.Assert

        trimmed.contains("[WARN", ignoreCase = true) ||
            trimmed.contains("Warn:", ignoreCase = true) -> Severity.Warn

        trimmed.contains("[DEBUG", ignoreCase = true) ||
            trimmed.contains("Debug:", ignoreCase = true) -> Severity.Debug

        trimmed.contains("[INFO", ignoreCase = true) ||
            trimmed.contains("Info:", ignoreCase = true) -> Severity.Info

        trimmed.contains("[VERBO", ignoreCase = true) ||
            trimmed.contains("Verbose:", ignoreCase = true) -> Severity.Verbose

        else -> null
    }

private fun isStackTraceOrErrorLine(trimmed: String): Boolean =
    trimmed.startsWith("at ") ||
        trimmed.startsWith("\tat ") ||
        trimmed.startsWith("Caused by:") ||
        trimmed.startsWith("Suppressed:") ||
        trimmed.startsWith("...") ||
        trimmed.endsWith("Exception") ||
        trimmed.contains("Exception:") ||
        trimmed.endsWith("Error") ||
        trimmed.contains("Error:")

private fun List<String>.toLogLines(): List<LogLine> {
    var currentSeverity: Severity? = null
    return map { line ->
        val trimmed = line.trimStart()
        val headerSeverity = detectHeaderSeverity(trimmed)
        val isTimestamped = TIMESTAMP_PREFIX_REGEX.containsMatchIn(trimmed)

        val severity =
            when {
                headerSeverity != null -> {
                    currentSeverity = headerSeverity
                    headerSeverity
                }

                isTimestamped -> {
                    currentSeverity = null
                    null
                }

                currentSeverity != null -> {
                    currentSeverity
                }

                isStackTraceOrErrorLine(trimmed) -> {
                    currentSeverity = Severity.Error
                    Severity.Error
                }

                else -> {
                    null
                }
            }

        LogLine(text = line, severity = severity)
    }
}
