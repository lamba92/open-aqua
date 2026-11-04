package io.github.lamba92.openaqua.app.logging

import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import io.github.lamba92.openaqua.app.model.LogsDirectory
import io.github.lamba92.openaqua.app.util.createDirectories
import io.github.lamba92.openaqua.app.util.emitLines
import io.github.lamba92.openaqua.app.util.openInDesktopEnvironment
import io.github.lamba92.openaqua.app.util.windowed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppLogService(
    val logsDirectory: LogsDirectory,
    scopeProvider: CoroutineScopeProvider,
    logSource: SharedLogSource,
    maxInMemoryLines: Int = 2000,
) {
    private val scope = scopeProvider.getScope(Dispatchers.IO)

    val logLines: StateFlow<List<String>> =
        logFlow(logSource)
            .windowed(min = 0, max = maxInMemoryLines)
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList(),
            )

    private fun logFlow(logSource: SharedLogSource): Flow<String> =
        flow {
            logsDirectory.logFile.emitLines()
            logSource.logFlow.collect { entry ->
                entry.format().forEach { line -> emit(line) }
            }
        }

    fun openLogsFolder() {
        scope.launch {
            runCatching {
                logsDirectory.value.createDirectories()
                logsDirectory.value.openInDesktopEnvironment()
            }
        }
    }
}
