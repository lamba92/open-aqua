package io.github.lamba92.openaqua.app.repository

import io.github.lamba92.openaqua.model.device.DeviceConfig
import io.github.lamba92.openaqua.model.device.DeviceId
import io.github.lamba92.openaqua.model.device.FlashConfigRequest

interface OpenAquaDeviceFlasher {
    suspend fun flashConfig(
        deviceId: DeviceId,
        config: FlashConfigRequest,
    ): DeviceConfig
}
