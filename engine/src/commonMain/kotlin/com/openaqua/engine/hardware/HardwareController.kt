package com.openaqua.engine.hardware

import com.openaqua.model.DetectedHardwareDevice
import com.openaqua.model.DeviceConfig
import com.openaqua.model.DeviceTelemetry
import com.openaqua.model.FlashConfigRequest
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
    firstOrNull { it.id.equals(trimmed, ignoreCase = true) || it.alias.equals(trimmed, ignoreCase = true) }?.let { return it }

    val noHyphenQuery = trimmed.replace("-", "").replace("_", "")
    firstOrNull {
        it.alias
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
