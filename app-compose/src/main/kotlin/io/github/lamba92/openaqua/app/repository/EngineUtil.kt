package io.github.lamba92.openaqua.app.repository

import io.github.lamba92.openaqua.model.EngineResponse
import io.github.lamba92.openaqua.model.formatWithStackTrace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.io.readLine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

suspend inline fun <reified T> executeAndDecodeWithTimeout(
    vararg command: String,
    timeout: Duration = 5.seconds,
    json: Json,
): T =
    withContext(Dispatchers.IO) {
        val process = ProcessBuilder(*command).start()
        try {
            val result =
                withTimeout(timeout) {
                    val input =
                        process.inputStream
                            .asSource()
                            .buffered()
                            .readLine()
                    if (input == null) {
                        val exitCode = if (!process.isAlive) process.exitValue() else null
                        val stderr =
                            process.errorStream
                                .bufferedReader()
                                .readText()
                                .trim()
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
                process.destroyForcibly()
            }
        }
    }

suspend inline fun <reified T> streamNdjsonToProcess(
    vararg command: String,
    source: Flow<T>,
    json: Json,
    crossinline onProcessStarted: (Process) -> Unit = {},
    crossinline onProcessTerminated: () -> Unit = {},
): Unit =
    withContext(Dispatchers.IO) {
        val process = ProcessBuilder(*command).start()
        onProcessStarted(process)
        try {
            val writer = process.outputStream.bufferedWriter()
            source
                .flowOn(Dispatchers.Default)
                .collect { item ->
                    writer.write(json.encodeToString(item))
                    writer.newLine()
                    writer.flush()
                }
        } finally {
            onProcessTerminated()
            runCatching { process.outputStream.close() }
            process.destroy()
        }
    }
