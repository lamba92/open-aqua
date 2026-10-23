package io.github.lamba92.openaqua.engine.hardware

import io.github.lamba92.openaqua.model.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.DeviceConfig
import io.github.lamba92.openaqua.model.DeviceTelemetry
import io.github.lamba92.openaqua.model.FlashConfigRequest
import kotlinx.coroutines.flow.Flow

interface HardwareController {
    fun scanDevices(): List<DetectedHardwareDevice>

    fun streamTelemetry(device: DetectedHardwareDevice): Flow<DeviceTelemetry>

    fun readConfig(device: DetectedHardwareDevice): DeviceConfig

    fun flashConfig(
        device: DetectedHardwareDevice,
        request: FlashConfigRequest,
    ): DeviceConfig

    fun feedVirtualSensors(
        device: DetectedHardwareDevice,
        sensors: Map<Int, Double>,
    )
}

fun List<DetectedHardwareDevice>.findTarget(query: String): DetectedHardwareDevice? {
    val trimmed = query.trim()
    firstOrNull {
        it.id.value.equals(trimmed, ignoreCase = true) ||
            it.alias.value.equals(trimmed, ignoreCase = true)
    }?.let { return it }

    val noHyphenQuery = trimmed.replace("-", "").replace("_", "")
    firstOrNull {
        it.alias.value
            .replace("-", "")
            .replace("_", "")
            .equals(noHyphenQuery, ignoreCase = true)
    }?.let { return it }

    val matchesByType =
        filter {
            val typeSlug =
                it.type.name
                    .lowercase()
                    .replace('_', '-')
            typeSlug.equals(trimmed, ignoreCase = true) || it.type.name.equals(trimmed, ignoreCase = true)
        }
    if (matchesByType.size == 1) {
        return matchesByType.first()
    }

    return null
}

fun HardwareController.requireTarget(query: String): DetectedHardwareDevice =
    scanDevices().findTarget(query) ?: error("Device '$query' not found")
