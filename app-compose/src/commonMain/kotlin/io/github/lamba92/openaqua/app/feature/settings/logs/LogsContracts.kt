package io.github.lamba92.openaqua.app.feature.settings.logs

import co.touchlab.kermit.Severity

data class LogLine(
    val text: String,
    val severity: Severity? = null,
)

data class LogsState(
    val logs: List<LogLine> = emptyList(),
)

sealed interface LogsEvent {
    data object OpenLogsFolder : LogsEvent
}

sealed interface LogsEffect
