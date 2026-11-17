package io.github.lamba92.openaqua.app.feature.settings.logs

import co.touchlab.kermit.Severity
import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.LoggingScope
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.logging.AppLogService
import io.github.lamba92.openaqua.app.mvi.MVIViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LogsViewModel(
    private val appLogService: AppLogService,
    scopeProvider: CoroutineScopeProvider,
) : MVIViewModel<LogsState, LogsEvent, LogsEffect> {
    override val viewModelScope: LoggingScope = scopeProvider.getScope()

    override val state: StateFlow<LogsState>
        field = MutableStateFlow(LogsState())

    override val effects: SharedFlow<LogsEffect>
        field = MutableSharedFlow()

    init {
        appLogService.logLines
            .map { it.toLogLines() }
            .flowOn(Dispatchers.Default)
            .onEach { lines ->
                state.update { LogsState(logs = lines) }
            }.launchIn(viewModelScope)
    }

    override fun onEvent(event: LogsEvent): Job =
        viewModelScope.launch {
            when (event) {
                LogsEvent.OpenLogsFolder -> appLogService.openLogsFolder()
            }
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
