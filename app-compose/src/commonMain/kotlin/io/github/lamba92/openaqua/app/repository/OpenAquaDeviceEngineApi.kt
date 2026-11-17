package io.github.lamba92.openaqua.app.repository

import io.github.lamba92.openaqua.model.device.DescriptorsResponse
import io.github.lamba92.openaqua.model.device.DeviceConfig
import io.github.lamba92.openaqua.model.device.DeviceId
import io.github.lamba92.openaqua.model.device.DeviceTelemetry
import io.github.lamba92.openaqua.model.device.DevicesResponse
import io.github.lamba92.openaqua.model.device.FeedVirtualSensorsRequest
import kotlinx.coroutines.flow.Flow

interface OpenAquaDeviceEngineApi : OpenAquaDeviceFlasher {
    suspend fun listDevices(): DevicesResponse

    suspend fun listDescriptors(): DescriptorsResponse

    fun getTelemetryStream(deviceId: DeviceId): Flow<DeviceTelemetry>

    suspend fun getDeviceConfig(deviceId: DeviceId): DeviceConfig

    suspend fun feedVirtualSensors(
        deviceId: DeviceId,
        source: Flow<FeedVirtualSensorsRequest>,
    )
}
