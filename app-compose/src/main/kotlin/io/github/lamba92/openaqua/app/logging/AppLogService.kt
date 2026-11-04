package io.github.lamba92.openaqua.app.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.getScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.time.Duration.Companion.milliseconds

class AppLogService(
    val logsDirectory: Path,
    scopeProvider: CoroutineScopeProvider,
    private val maxLinesPerFile: Int = 5000,
    private val maxHistoryFiles: Int = 3,
    private val maxInMemoryLines: Int = 2000,
) {
    private val scope = scopeProvider.getScope(Dispatchers.IO)
    private val writeChannel = Channel<List<String>>(capacity = Channel.UNLIMITED)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private val currentLogFile = logsDirectory.resolve("open-aqua.log")

    val logLines: StateFlow<List<String>>
        field = MutableStateFlow<List<String>>(emptyList())

    private var currentFileLineCount = 0

    val logWriter: LogWriter =
        object : LogWriter() {
            override fun log(
                severity: Severity,
                message: String,
                tag: String,
                throwable: Throwable?,
            ) {
                val timestamp = LocalDateTime.now().format(dateFormatter)
                val header = "$timestamp [${severity.name.padEnd(5)}] [$tag] $message"
                val lines = mutableListOf(header)
                if (throwable != null) {
                    lines.addAll(throwable.stackTraceToString().lines().filter { it.isNotBlank() })
                }
                writeChannel.trySend(lines)
            }
        }

    init {
        scope.launch {
            runCatching {
                logsDirectory.createDirectories()
                if (currentLogFile.exists()) {
                    currentFileLineCount = currentLogFile.readLines().size
                }
            }

            for (lines in writeChannel) {
                runCatching {
                    logsDirectory.createDirectories()
                    if (currentFileLineCount + lines.size > maxLinesPerFile) {
                        rotateLogFiles()
                    }
                    Files.write(
                        currentLogFile,
                        lines,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.APPEND,
                    )
                    currentFileLineCount += lines.size
                }
            }
        }

        scope.launch {
            while (true) {
                runCatching {
                    if (currentLogFile.exists()) {
                        val lines = currentLogFile.readLines()
                        val window =
                            if (lines.size > maxInMemoryLines) {
                                lines.takeLast(maxInMemoryLines)
                            } else {
                                lines
                            }
                        if (logLines.value != window) {
                            logLines.value = window
                        }
                    } else if (logLines.value.isNotEmpty()) {
                        logLines.value = emptyList()
                    }
                }
                delay(300.milliseconds)
            }
        }
    }

    private fun rotateLogFiles() {
        for (i in (maxHistoryFiles - 1) downTo 1) {
            val source = logsDirectory.resolve("open-aqua.$i.log")
            if (source.exists()) {
                if (i == maxHistoryFiles - 1) {
                    source.deleteIfExists()
                } else {
                    val dest = logsDirectory.resolve("open-aqua.${i + 1}.log")
                    Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
        if (currentLogFile.exists()) {
            val firstHistory = logsDirectory.resolve("open-aqua.1.log")
            Files.move(currentLogFile, firstHistory, StandardCopyOption.REPLACE_EXISTING)
        }
        currentFileLineCount = 0
    }

    fun clearLogs() {
        scope.launch {
            runCatching {
                currentLogFile.deleteIfExists()
                for (i in 1 until maxHistoryFiles) {
                    logsDirectory.resolve("open-aqua.$i.log").deleteIfExists()
                }
                currentFileLineCount = 0
            }
            logLines.value = emptyList()
        }
    }

    fun openLogsFolder() {
        scope.launch {
            runCatching {
                logsDirectory.createDirectories()
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(logsDirectory.toFile())
                } else {
                    val os = System.getProperty("os.name").lowercase()
                    val cmd =
                        when {
                            os.contains("mac") -> listOf("open", logsDirectory.absolutePathString())
                            os.contains("win") -> listOf("explorer", logsDirectory.absolutePathString())
                            else -> listOf("xdg-open", logsDirectory.absolutePathString())
                        }
                    ProcessBuilder(cmd).start()
                }
            }
        }
    }
}
