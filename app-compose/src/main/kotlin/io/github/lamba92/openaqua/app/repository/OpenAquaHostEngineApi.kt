package io.github.lamba92.openaqua.app.repository

import io.github.lamba92.openaqua.app.coroutine.getLogger
import io.github.lamba92.openaqua.app.util.HostEnginePath
import io.github.lamba92.openaqua.model.EngineResponse
import io.github.lamba92.openaqua.model.formatWithStackTrace
import io.github.lamba92.openaqua.model.host.HostInfo
import io.github.lamba92.openaqua.model.host.HostTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.io.readLine
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class OpenAquaHostEngineApi(
    private val hostEnginePath: HostEnginePath,
) {
    private val enginePath: Path
        get() = hostEnginePath.path

    private val json: Json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = false
        }

    suspend fun getHostInfo(): HostInfo = executeAndDecodeWithTimeout("info")

    fun getTelemetryStream(): Flow<HostTelemetry> =
        channelFlow {
            val logger = getLogger()
            withContext(Dispatchers.IO) {
                val process =
                    ProcessBuilder(enginePath.absolutePathString(), "stream-telemetry").start()
                logger.i { "Started host telemetry stream process (PID ${process.pid()})" }
                try {
                    val reader = process.inputStream.asSource().buffered()
                    while (true) {
                        val line =
                            reader.readLine() ?: run {
                                val exitCode = if (!process.isAlive) process.exitValue() else null
                                val stderr =
                                    process.errorStream
                                        .bufferedReader()
                                        .readText()
                                        .trim()
                                error(
                                    "Host telemetry stream process closed unexpectedly" +
                                        (if (exitCode != null) " (exit code $exitCode)" else "") +
                                        (if (stderr.isNotBlank()) ": $stderr" else ""),
                                )
                            }
                        when (val telemetry = json.decodeFromString<EngineResponse<HostTelemetry>>(line)) {
                            is EngineResponse.Error -> error(telemetry.formatWithStackTrace())
                            is EngineResponse.Success<HostTelemetry> -> send(telemetry.value)
                        }
                    }
                } finally {
                    logger.i { "Terminating host telemetry stream process" }
                    process.destroy()
                }
            }
        }

    private suspend inline fun <reified T> executeAndDecodeWithTimeout(
        vararg command: String,
        timeout: Duration = 5.seconds,
    ): T =
        executeAndDecodeWithTimeout(
            command = arrayOf(enginePath.absolutePathString(), *command),
            timeout = timeout,
            json = json,
        )
}
