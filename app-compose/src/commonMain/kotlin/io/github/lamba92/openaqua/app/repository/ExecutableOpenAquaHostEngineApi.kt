package io.github.lamba92.openaqua.app.repository

import co.touchlab.kermit.Logger
import io.github.lamba92.openaqua.app.coroutine.getLogger
import io.github.lamba92.openaqua.app.util.executeAndDecodeWithTimeout
import io.github.lamba92.openaqua.app.util.extractEngineBinary
import io.github.lamba92.openaqua.app.util.streamNdjsonToProcess
import io.github.lamba92.openaqua.app.util.streamProcessLines
import io.github.lamba92.openaqua.model.EngineResponse
import io.github.lamba92.openaqua.model.formatWithStackTrace
import io.github.lamba92.openaqua.model.host.HostFanControlRequest
import io.github.lamba92.openaqua.model.host.HostInfo
import io.github.lamba92.openaqua.model.host.HostTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.io.files.Path
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ExecutableOpenAquaHostEngineApi(
    private val logger: Logger? = null,
) : OpenAquaHostEngineApi {
    private val enginePath: Path by lazy {
        extractEngineBinary("open-aqua-host-engine", logger)
    }

    private val json: Json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = false
        }

    override suspend fun getHostInfo(): HostInfo = executeAndDecodeWithTimeout("info")

    override fun getTelemetryStream(): Flow<HostTelemetry> =
        channelFlow {
            val log = getLogger()
            val lines =
                streamProcessLines(
                    command = listOf(enginePath.toString(), "stream-telemetry"),
                    onStarted = { process ->
                        log.i { "Started host telemetry stream process (PID ${process.pid})" }
                    },
                    onTerminated = {
                        log.i { "Terminating host telemetry stream process" }
                    },
                )
            lines.collect { line ->
                when (val telemetry = json.decodeFromString<EngineResponse<HostTelemetry>>(line)) {
                    is EngineResponse.Error -> error(telemetry.formatWithStackTrace())
                    is EngineResponse.Success<HostTelemetry> -> send(telemetry.value)
                }
            }
        }

    override suspend fun streamFanControl(requests: Flow<HostFanControlRequest>) {
        val log = getLogger()
        streamNdjsonToProcess(
            command = listOf(enginePath.toString(), "stream-fans"),
            source = requests,
            json = json,
            onProcessStarted = { process ->
                log.i { "Started host fan control stream process (PID ${process.pid})" }
            },
            onProcessTerminated = {
                log.i { "Terminating host fan control stream process" }
            },
        )
    }

    override suspend fun restoreFanDefaults(): Unit = executeAndDecodeWithTimeout("restore-fan-defaults", timeout = 5.seconds)

    private suspend inline fun <reified T> executeAndDecodeWithTimeout(
        vararg command: String,
        timeout: Duration = 5.seconds,
    ): T =
        executeAndDecodeWithTimeout(
            command = listOf(enginePath.toString(), *command),
            timeout = timeout,
            json = json,
        )
}
