package io.github.lamba92.openaqua.app.repository

import io.github.lamba92.openaqua.app.coroutine.getLogger
import io.github.lamba92.openaqua.app.util.DeviceEnginePath
import io.github.lamba92.openaqua.model.EngineResponse
import io.github.lamba92.openaqua.model.device.DescriptorsResponse
import io.github.lamba92.openaqua.model.device.DeviceConfig
import io.github.lamba92.openaqua.model.device.DeviceId
import io.github.lamba92.openaqua.model.device.DeviceTelemetry
import io.github.lamba92.openaqua.model.device.DevicesResponse
import io.github.lamba92.openaqua.model.device.FeedVirtualSensorsRequest
import io.github.lamba92.openaqua.model.device.FlashConfigRequest
import io.github.lamba92.openaqua.model.formatWithStackTrace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.io.readLine
import kotlinx.io.writeString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.writeText
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class OpenAquaDeviceEngineApi(
    private val deviceEnginePath: DeviceEnginePath,
) : OpenAquaDeviceFlasher {
    private val enginePath: Path
        get() = deviceEnginePath.path

    private val json: Json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = false
        }

    suspend fun listDevices(): DevicesResponse = executeAndDecodeWithTimeout("devices")

    suspend fun listDescriptors(): DescriptorsResponse = executeAndDecodeWithTimeout("descriptors")

    fun getTelemetryStream(deviceId: DeviceId): Flow<DeviceTelemetry> =
        channelFlow {
            val logger = getLogger()
            withContext(Dispatchers.IO) {
                val process =
                    ProcessBuilder(enginePath.absolutePathString(), "stream-telemetry", "--device=${deviceId.value}").start()
                logger.i { "Started device telemetry stream process for '${deviceId.value}' (PID ${process.pid()})" }
                try {
                    val reader = process.inputStream.asSource().buffered()
                    while (true) {
                        val line = reader.readLine() ?: error("Device telemetry stream process closed unexpectedly for device '${deviceId.value}'")
                        when (val telemetry = json.decodeFromString<EngineResponse<DeviceTelemetry>>(line)) {
                            is EngineResponse.Error -> error(telemetry.formatWithStackTrace())
                            is EngineResponse.Success<DeviceTelemetry> -> send(telemetry.value)
                        }
                    }
                } finally {
                    logger.i { "Terminating device telemetry stream process for '${deviceId.value}'" }
                    process.destroy()
                }
            }
        }

    suspend fun getDeviceConfig(deviceId: DeviceId): DeviceConfig = executeAndDecodeWithTimeout("config", "read", "--device=${deviceId.value}")

    override suspend fun flashConfig(
        deviceId: DeviceId,
        config: FlashConfigRequest,
    ): DeviceConfig {
        val logger = getLogger()
        logger.i { "Flashing configuration to device '${deviceId.value}'..." }
        val tempFile =
            withContext(Dispatchers.IO) {
                java.nio.file.Files.createTempFile("open-aqua-flash-", ".json").apply {
                    toFile().deleteOnExit()
                    writeText(json.encodeToString(FlashConfigRequest.serializer(), config))
                }
            }
        val result: DeviceConfig =
            try {
                executeAndDecodeWithTimeout(
                    "config",
                    "flash",
                    "--device=${deviceId.value}",
                    "--file=${tempFile.absolutePathString()}",
                    timeout = 10.seconds,
                )
            } finally {
                withContext(Dispatchers.IO) {
                    runCatching { java.nio.file.Files.deleteIfExists(tempFile) }
                }
            }
        logger.i { "Configuration flashed successfully to device '${deviceId.value}'" }
        return result
    }

    suspend fun feedVirtualSensors(
        deviceId: DeviceId,
        source: Flow<FeedVirtualSensorsRequest>,
    ) = withContext(Dispatchers.IO) {
        val logger = getLogger()
        val process =
            ProcessBuilder(
                enginePath.absolutePathString(),
                "feed-virtual-sensors",
                "--device=${deviceId.value}",
            ).start()
        logger.i { "Started virtual sensor feeder process for device '${deviceId.value}' (PID ${process.pid()})" }
        try {
            val writer = process.outputStream.asSink().buffered()
            source
                .flowOn(Dispatchers.Default)
                .onEach { request ->
                    writer.writeString(json.encodeToString(FeedVirtualSensorsRequest.serializer(), request) + "\n")
                    writer.flush()
                }.flowOn(Dispatchers.IO)
                .collect()
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            val errorOutput = runCatching { process.errorStream.bufferedReader().readText() }.getOrNull().orEmpty()
            if (e is IOException && e.message?.contains("Stream closed") == true) {
                logger.i { "Feeder stream closed for device '${deviceId.value}'" }
            } else {
                logger.e(e) {
                    "Error while feeding virtual sensors for device '${deviceId.value}'" +
                        if (errorOutput.isNotBlank()) ": $errorOutput" else ""
                }
            }
        } finally {
            logger.i { "Terminating virtual sensor feeder process for device '${deviceId.value}'" }
            runCatching { process.outputStream.close() }
            process.destroy()
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
