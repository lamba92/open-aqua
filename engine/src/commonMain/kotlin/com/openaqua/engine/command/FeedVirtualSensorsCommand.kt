package com.openaqua.engine.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.openaqua.engine.hardware.HardwareController
import com.openaqua.engine.hardware.requireTarget
import com.openaqua.model.DetectedHardwareDevice
import com.openaqua.model.DeviceDescriptor
import com.openaqua.model.DeviceType
import com.openaqua.model.FeedVirtualSensorsRequest
import com.openaqua.model.descriptor
import kotlinx.serialization.json.Json

class FeedVirtualSensorsCommand(
    private val controller: HardwareController,
) : CliktCommand(
        name = "feed-virtual-sensors",
    ) {
    override fun help(context: Context): String = "Feed live virtual temperature sensors via NDJSON on stdin"

    private val device by option("--device", help = "Device ID or alias (e.g. octo-1)").required()

    override fun run() {
        val target = controller.requireTarget(device)
        val descriptor = target.descriptor()

        check(descriptor.virtualSensorsCount > 0) {
            "${target.name} does not support virtual sensors"
        }

        val startChannel = if (target.type == DeviceType.D5_NEXT) 1 else 4
        val maxChannel = startChannel + descriptor.virtualSensorsCount - 1

        while (true) {
            val line = readlnOrNull() ?: break
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val request = Json.decodeFromString<FeedVirtualSensorsRequest>(trimmed)
            validateAndFeed(request, target, descriptor, startChannel, maxChannel)
        }
    }

    private fun validateAndFeed(
        request: FeedVirtualSensorsRequest,
        target: DetectedHardwareDevice,
        descriptor: DeviceDescriptor,
        startChannel: Int,
        maxChannel: Int,
    ) {
        request.sensors.keys.forEach { channel ->
            if (target.type == DeviceType.D5_NEXT && channel == 0) {
                error(
                    "Channel 0 is the coolant temperature sensor on ${target.name}. " +
                        "Virtual sensors are channels 1..8.",
                )
            } else if (channel in 0..3) {
                error(
                    "Channel $channel is a physical sensor on ${target.name}. " +
                        "Virtual sensors are channels $startChannel..$maxChannel.",
                )
            }
            require(channel in startChannel..maxChannel) {
                "Channel $channel is invalid for ${target.name}. " +
                    "Valid virtual sensors are channels $startChannel..$maxChannel."
            }
        }
        controller.feedVirtualSensors(target, request.sensors)
    }
}
