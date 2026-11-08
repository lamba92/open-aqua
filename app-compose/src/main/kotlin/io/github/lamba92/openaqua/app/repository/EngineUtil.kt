package io.github.lamba92.openaqua.app.repository

import io.github.lamba92.openaqua.model.EngineResponse
import io.github.lamba92.openaqua.model.formatWithStackTrace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.io.readLine
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
        val result =
            try {
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
            } catch (e: Throwable) {
                if (process.isAlive) {
                    process.destroyForcibly()
                }
                throw e
            }
        process.waitFor()
        result
    }
