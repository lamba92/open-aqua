package io.github.lamba92.openaqua.app.repository

import co.touchlab.kermit.Logger
import io.github.lamba92.openaqua.app.coroutine.getLogger
import io.github.lamba92.openaqua.app.util.createTempFilePath
import io.github.lamba92.openaqua.app.util.deleteIfExists
import io.github.lamba92.openaqua.app.util.executeAndDecodeWithTimeout
import io.github.lamba92.openaqua.app.util.extractEngineBinary
import io.github.lamba92.openaqua.app.util.streamNdjsonToProcess
import io.github.lamba92.openaqua.app.util.streamProcessLines
import io.github.lamba92.openaqua.app.util.writeText
import io.github.lamba92.openaqua.model.EngineResponse
import io.github.lamba92.openaqua.model.device.DescriptorsResponse
import io.github.lamba92.openaqua.model.device.DeviceConfig
import io.github.lamba92.openaqua.model.device.DeviceId
import io.github.lamba92.openaqua.model.device.DeviceTelemetry
import io.github.lamba92.openaqua.model.device.DevicesResponse
import io.github.lamba92.openaqua.model.device.FeedVirtualSensorsRequest
import io.github.lamba92.openaqua.model.device.FlashConfigRequest
import io.github.lamba92.openaqua.model.formatWithStackTrace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.io.files.Path
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ExecutableOpenAquaDeviceEngineApi(
    private val logger: Logger? = null,
) : OpenAquaDeviceEngineApi {
    private val enginePath: Path by lazy {
        extractEngineBinary("open-aqua-device-engine", logger)
    }

    private val json: Json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = false
        }

    override suspend fun listDevices(): DevicesResponse = executeAndDecodeWithTimeout("devices")

    override suspend fun listDescriptors(): DescriptorsResponse = executeAndDecodeWithTimeout("descriptors")

    override fun getTelemetryStream(deviceId: DeviceId): Flow<DeviceTelemetry> =
        channelFlow {
            val log = getLogger()
            val lines =
                streamProcessLines(
                    command = listOf(enginePath.toString(), "stream-telemetry", "--device=${deviceId.value}"),
                    onStarted = { process ->
                        log.i { "Started device telemetry stream process for '${deviceId.value}' (PID ${process.pid})" }
                    },
                    onTerminated = {
                        log.i { "Terminating device telemetry stream process for '${deviceId.value}'" }
                    },
                )
            lines.collect { line ->
                when (val telemetry = json.decodeFromString<EngineResponse<DeviceTelemetry>>(line)) {
                    is EngineResponse.Error -> error(telemetry.formatWithStackTrace())
                    is EngineResponse.Success<DeviceTelemetry> -> send(telemetry.value)
                }
            }
        }

    override suspend fun getDeviceConfig(deviceId: DeviceId): DeviceConfig = executeAndDecodeWithTimeout("config", "read", "--device=${deviceId.value}")

    override suspend fun flashConfig(
        deviceId: DeviceId,
        config: FlashConfigRequest,
    ): DeviceConfig {
        val log = getLogger()
        log.i { "Flashing configuration to device '${deviceId.value}'..." }
        val tempFile = createTempFilePath("open-aqua-flash-", ".json")
        try {
            tempFile.writeText(json.encodeToString(FlashConfigRequest.serializer(), config))
            val result: DeviceConfig =
                executeAndDecodeWithTimeout(
                    "config",
                    "flash",
                    "--device=${deviceId.value}",
                    "--file=$tempFile",
                    timeout = 10.seconds,
                )
            log.i { "Configuration flashed successfully to device '${deviceId.value}'" }
            return result
        } finally {
            tempFile.deleteIfExists()
        }
    }

    override suspend fun feedVirtualSensors(
        deviceId: DeviceId,
        source: Flow<FeedVirtualSensorsRequest>,
    ) {
        val log = getLogger()
        streamNdjsonToProcess(
            command = listOf(enginePath.toString(), "feed-virtual-sensors", "--device=${deviceId.value}"),
            source = source,
            json = json,
            onProcessStarted = { process ->
                log.i { "Started virtual sensor feeder process for device '${deviceId.value}' (PID ${process.pid})" }
            },
            onProcessTerminated = {
                log.i { "Terminating virtual sensor feeder process for device '${deviceId.value}'" }
            },
        )
    }

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
