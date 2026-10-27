package io.github.lamba92.openaqua.app.repository

import io.github.lamba92.openaqua.model.DeviceConfig
import io.github.lamba92.openaqua.model.DeviceId
import io.github.lamba92.openaqua.model.FlashConfigRequest

interface OpenAquaDeviceFlasher {
    suspend fun flashConfig(
        deviceId: DeviceId,
        config: FlashConfigRequest,
    ): DeviceConfig
}
