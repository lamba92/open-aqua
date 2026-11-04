package io.github.lamba92.openaqua.app.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class LogEntry(
    val timestamp: LocalDateTime,
    val severity: Severity,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null,
) {
    fun format(formatter: DateTimeFormat<LocalDateTime> = LocalDateTime.Formats.ISO): List<String> {
        val timestampStr = formatter.format(timestamp)
        val header = "$timestampStr [${severity.name.padEnd(5)}] [$tag] $message"
        val lines = mutableListOf(header)
        if (throwable != null) {
            lines.addAll(throwable.stackTraceToString().lines().filter { it.isNotBlank() })
        }
        return lines
    }
}

class SharedLogSource(
    bufferCapacity: Int = 1000,
) {
    val logFlow: SharedFlow<LogEntry>
        field =
        MutableSharedFlow<LogEntry>(
            extraBufferCapacity = bufferCapacity,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    val writer =
        object : LogWriter() {
            override fun log(
                severity: Severity,
                message: String,
                tag: String,
                throwable: Throwable?,
            ) {
                val entry =
                    LogEntry(
                        timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
                        severity = severity,
                        tag = tag,
                        message = message,
                        throwable = throwable,
                    )
                logFlow.tryEmit(entry)
            }
        }
}
