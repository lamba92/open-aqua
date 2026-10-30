package io.github.lamba92.openaqua.app.repository

import io.github.lamba92.openaqua.model.EngineResponse
import io.github.lamba92.openaqua.model.device.DescriptorsResponse
import io.github.lamba92.openaqua.model.device.DeviceConfig
import io.github.lamba92.openaqua.model.device.DeviceId
import io.github.lamba92.openaqua.model.device.DeviceTelemetry
import io.github.lamba92.openaqua.model.device.DevicesResponse
import io.github.lamba92.openaqua.model.device.FeedVirtualSensorsRequest
import io.github.lamba92.openaqua.model.device.FlashConfigRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class OpenAquaDeviceEngineApi(
    private val enginePath: Path,
) : OpenAquaDeviceFlasher {
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
            withContext(Dispatchers.IO) {
                val process =
                    ProcessBuilder(enginePath.absolutePathString(), "stream-telemetry", "--device=${deviceId.value}").start()
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

    suspend fun getDeviceConfig(deviceId: DeviceId): DeviceConfig = executeAndDecodeWithTimeout("config", "read", "--device=${deviceId.value}")

    override suspend fun flashConfig(
        deviceId: DeviceId,
        config: FlashConfigRequest,
    ): DeviceConfig =
        executeAndDecodeWithTimeout(
            "config",
            "flash",
            "--device=${deviceId.value}",
            "--request=${json.encodeToString(FlashConfigRequest.serializer(), config)}",
        )

    suspend fun feedVirtualSensors(
        deviceId: DeviceId,
        source: Flow<FeedVirtualSensorsRequest>,
    ) = withContext(Dispatchers.IO) {
        val process =
            ProcessBuilder(
                enginePath.absolutePathString(),
                "feed-virtual-sensors",
                "--device=${deviceId.value}",
            ).start()
        try {
            val writer = process.outputStream.bufferedWriter()
            source
                .flowOn(Dispatchers.Default)
                .onEach { request ->
                    writer.write(json.encodeToString(FeedVirtualSensorsRequest.serializer(), request) + "\n")
                    writer.flush()
                }.flowOn(Dispatchers.IO)
                .collect()
        } finally {
            process.destroy()
        }
    }

    private suspend inline fun <reified T> executeAndDecodeWithTimeout(
        vararg command: String,
        timeout: Duration = 2.seconds,
    ): T =
        executeAndDecodeWithTimeout(
            command = arrayOf(enginePath.absolutePathString(), *command),
            timeout = timeout,
            json = json,
        )
}
