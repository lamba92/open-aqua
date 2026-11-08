package io.github.lamba92.openaqua.model.device

/**
 * Validates that all fields in a [FlashConfigRequest] correspond to capabilities, channels,
 * and ports physically present and configurable on this [DeviceDescriptor].
 *
 * Checks:
 * - Configured fan channels exist and support hardware limits.
 * - Source channels for copy/follow mode exist on the device and do not reference the same channel.
 * - Sensor offset channels exist and support calibration.
 * - RGBpx ports exist within the device's port count.
 * - Flow sensor calibration is only requested on devices with a calibrated flow sensor.
 * - Display configuration is only requested on devices equipped with a display.
 *
 * @param request The flash configuration payload to validate against this device.
 * @throws IllegalArgumentException If any configuration target is unsupported or invalid for this device.
 */
fun DeviceDescriptor.validateRequestMatchesDevice(request: FlashConfigRequest) {
    val fansCap = findCapability<DeviceCapability.Fans>()
    request.fans?.forEach { (ch, config) ->
        val fan = fansCap?.channels?.firstOrNull { it.channel == ch }
        requireNotNull(fan) {
            "Invalid fan channel $ch for $name"
        }
        require(fan is FanChannelDescriptor.WithLimits) {
            "Fan channel $ch does not support fan limits on $name"
        }
        when (config) {
            is CopyFanConfig -> {
                val sourceFan = fansCap.channels.firstOrNull { it.channel == config.sourceChannel }
                requireNotNull(sourceFan) {
                    "Invalid source fan channel ${config.sourceChannel} to copy for $name"
                }
                require(config.sourceChannel != ch) {
                    "Fan channel $ch cannot copy from itself"
                }
            }
            else -> Unit
        }
    }

    val tempSensorsCap = findCapability<DeviceCapability.TemperatureSensors>()
    request.sensorOffsets?.keys?.forEach { ch ->
        require(tempSensorsCap is DeviceCapability.TemperatureSensors.Calibrated && tempSensorsCap.sensors.any { it.channel == ch }) {
            "Invalid sensor channel $ch for $name"
        }
    }

    val rgbpxCap = findCapability<DeviceCapability.Lighting.Rgbpx>()
    request.rgbpx?.keys?.forEach { port ->
        require(rgbpxCap != null && port in 0 until rgbpxCap.portCount) {
            "Invalid RGBpx port $port for $name"
        }
    }

    val flowCap = findCapability<DeviceCapability.FlowSensor>()
    if (request.flowPulsesPerLiter != null) {
        require(flowCap is DeviceCapability.FlowSensor.Calibrated) {
            "Device $name does not support flow sensor calibration"
        }
    }

    val displayCap = findCapability<DeviceCapability.Display>()
    if (request.display != null) {
        require(displayCap != null) {
            "Device $name does not support display configuration"
        }
    }
}
