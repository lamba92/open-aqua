@file:OptIn(ExperimentalSerializationApi::class)

package io.github.lamba92.openaqua.app.repository

import io.github.lamba92.openaqua.model.DescriptorsResponse
import io.github.lamba92.openaqua.model.DeviceConfig
import io.github.lamba92.openaqua.model.DeviceTelemetry
import io.github.lamba92.openaqua.model.DevicesResponse
import io.github.lamba92.openaqua.model.EngineResponse
import io.github.lamba92.openaqua.model.FeedVirtualSensorsRequest
import io.github.lamba92.openaqua.model.FlashConfigRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class OpenAquaEngineApi(
    private val enginePath: Path,
) {
    private val json: Json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = false
        }

    suspend fun listDevices(): DevicesResponse = executeAndDecodeWithTimeout("devices")

    suspend fun listDescriptors(): DescriptorsResponse = executeAndDecodeWithTimeout("descriptors")

    fun getTelemetryStream(deviceId: String): Flow<DeviceTelemetry> =
        channelFlow {
            withContext(Dispatchers.IO) {
                val process =
                    ProcessBuilder(enginePath.absolutePathString(), "stream-telemetry", "--device=$deviceId").start()
                try {
                    val reader = process.inputStream.bufferedReader()
                    while (isActive) {
                        val line = reader.readLine() ?: break
                        when (val telemetry = json.decodeFromString<EngineResponse<DeviceTelemetry>>(line)) {
                            is EngineResponse.Error -> error(telemetry.message)
                            is EngineResponse.Success<DeviceTelemetry> -> send(telemetry.value)
                        }
                    }
                } finally {
                    process.destroy()
                }
            }
        }

    suspend fun getDeviceConfig(deviceId: String): DeviceConfig = executeAndDecodeWithTimeout("config", "read", "--device=$deviceId")

    suspend fun flashConfig(
        deviceId: String,
        config: FlashConfigRequest,
    ): DeviceConfig =
        executeAndDecodeWithTimeout(
            "config",
            "flash",
            "--device=$deviceId",
            "--request=${json.encodeToString(FlashConfigRequest.serializer(), config)}",
        )

    suspend fun feedVirtualSensors(
        deviceId: String,
        source: Flow<FeedVirtualSensorsRequest>,
    ) = withContext(Dispatchers.IO) {
        val process =
            ProcessBuilder(
                enginePath.absolutePathString(),
                "feed-virtual-sensors",
                "--device=$deviceId",
            ).start()
        try {
            val writer = process.outputStream.bufferedWriter()
            source
                .flowOn(Dispatchers.Default)
                .collect { request ->
                    writer.write(json.encodeToString(FeedVirtualSensorsRequest.serializer(), request) + "\n")
                    writer.flush()
                }
        } finally {
            process.destroy()
        }
    }

    private suspend inline fun <reified T> executeAndDecodeWithTimeout(
        vararg command: String,
        timeout: Duration = 2.seconds,
    ): T =
        withContext(Dispatchers.IO) {
            val process =
                ProcessBuilder(
                    enginePath.absolutePathString(),
                    *command,
                ).start()
            val result =
                withTimeout(timeout) {
                    val input = process.inputStream.
                    when (val response = json.decodeFromStream<EngineResponse<T>>()) {
                        is EngineResponse.Error -> error(response.message)
                        is EngineResponse.Success<T> -> response.value
                    }
                }
            process.waitFor()
            result
        }
}
