package io.github.lamba92.openaqua.app.repository

import io.github.lamba92.openaqua.model.EngineResponse
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
    private val enginePath: Path,
) {
    private val json: Json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = false
        }

    suspend fun getHostInfo(): HostInfo = executeAndDecodeWithTimeout("info")

    fun getTelemetryStream(): Flow<HostTelemetry> =
        channelFlow {
            withContext(Dispatchers.IO) {
                val process =
                    ProcessBuilder(enginePath.absolutePathString(), "stream-telemetry").start()
                try {
                    val reader = process.inputStream.asSource().buffered()
                    while (true) {
                        val line = reader.readLine() ?: error("Host telemetry stream process closed unexpectedly")
                        when (val telemetry = json.decodeFromString<EngineResponse<HostTelemetry>>(line)) {
                            is EngineResponse.Error -> error(telemetry.message)
                            is EngineResponse.Success<HostTelemetry> -> send(telemetry.value)
                        }
                    }
                } finally {
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
