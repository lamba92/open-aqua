package io.github.lamba92.openaqua.app.util

import co.touchlab.kermit.Logger
import io.github.lamba92.openaqua.model.EngineResponse
import io.github.lamba92.openaqua.model.formatWithStackTrace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.io.files.Path
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface PlatformProcess {
    val pid: Long
    val isAlive: Boolean
    val exitCode: Int?

    suspend fun readLine(): String?

    suspend fun readStderr(): String

    suspend fun writeLine(line: String)

    suspend fun flushInput()

    suspend fun closeInput()

    suspend fun waitFor(): Int

    fun destroy()
}

expect fun startProcess(command: List<String>): PlatformProcess

expect fun extractEngineBinary(
    prefix: String,
    logger: Logger? = null,
): Path

expect fun createTempDirectoryPath(prefix: String): Path

expect fun createTempFilePath(
    prefix: String,
    suffix: String,
): Path

suspend inline fun <reified T> executeAndDecodeWithTimeout(
    command: List<String>,
    timeout: Duration = 5.seconds,
    json: Json,
): T =
    withContext(ioDispatcher) {
        val process = startProcess(command)
        try {
            val result =
                withTimeout(timeout) {
                    val input = process.readLine()
                    if (input == null) {
                        val exitCode = if (!process.isAlive) process.exitCode else null
                        val stderr = process.readStderr().trim()
                        val commandName = command.firstOrNull()?.substringAfterLast('/')?.substringAfterLast('\\') ?: "engine"
                        error(
                            "Engine process '$commandName' terminated without output" +
                                (if (exitCode != null) " (exit code $exitCode)" else "") +
                                (if (stderr.isNotBlank()) ": $stderr" else ""),
                        )
                    }
                    when (val response = json.decodeFromString<EngineResponse<T>>(input)) {
                        is EngineResponse.Error -> error(response.formatWithStackTrace())
                        is EngineResponse.Success<T> -> response.value
                    }
                }
            process.waitFor()
            result
        } finally {
            if (process.isAlive) {
                process.destroy()
            }
        }
    }

fun streamProcessLines(
    command: List<String>,
    onStarted: (PlatformProcess) -> Unit = {},
    onTerminated: () -> Unit = {},
): Flow<String> =
    channelFlow {
        withContext(ioDispatcher) {
            val process = startProcess(command)
            onStarted(process)
            try {
                while (true) {
                    val line =
                        process.readLine() ?: run {
                            val exitCode = if (!process.isAlive) process.exitCode else null
                            val stderr = process.readStderr().trim()
                            val commandName = command.firstOrNull()?.substringAfterLast('/')?.substringAfterLast('\\') ?: "engine"
                            error(
                                "Process '$commandName' closed unexpectedly" +
                                    (if (exitCode != null) " (exit code $exitCode)" else "") +
                                    (if (stderr.isNotBlank()) ": $stderr" else ""),
                            )
                        }
                    send(line)
                }
            } finally {
                onTerminated()
                process.destroy()
            }
        }
    }

suspend inline fun <reified T> streamNdjsonToProcess(
    command: List<String>,
    source: Flow<T>,
    json: Json,
    crossinline onProcessStarted: (PlatformProcess) -> Unit = {},
    crossinline onProcessTerminated: () -> Unit = {},
): Unit =
    withContext(ioDispatcher) {
        val process = startProcess(command)
        onProcessStarted(process)
        try {
            source
                .flowOn(Dispatchers.Default)
                .collect { item ->
                    process.writeLine(json.encodeToString(item))
                    process.flushInput()
                }
        } finally {
            onProcessTerminated()
            runCatching { process.closeInput() }
            process.destroy()
        }
    }
