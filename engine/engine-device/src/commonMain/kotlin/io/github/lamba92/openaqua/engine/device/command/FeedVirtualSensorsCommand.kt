package io.github.lamba92.openaqua.engine.device.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import io.github.lamba92.openaqua.engine.device.hardware.HardwareController
import io.github.lamba92.openaqua.engine.device.hardware.requireTarget
import io.github.lamba92.openaqua.model.device.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.device.DeviceCapability
import io.github.lamba92.openaqua.model.device.DeviceType
import io.github.lamba92.openaqua.model.device.FeedVirtualSensorsRequest
import io.github.lamba92.openaqua.model.device.descriptor
import io.github.lamba92.openaqua.model.device.findCapability
import io.github.lamba92.openaqua.model.device.virtualSensorsChannelRange
import io.github.lamba92.openaqua.model.device.virtualSensorsStartChannel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
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

        val virtCap = descriptor.findCapability<DeviceCapability.VirtualSensors>()
        check(virtCap != null && virtCap.count > 0) {
            "${target.name} does not support virtual sensors"
        }

        val startChannel = descriptor.virtualSensorsStartChannel
        val maxChannel = descriptor.virtualSensorsChannelRange.last

        val requestsFlow =
            flow {
                while (true) {
                    val line = readlnOrNull() ?: break
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) continue
                    val request = Json.decodeFromString<FeedVirtualSensorsRequest>(trimmed)
                    validateRequest(request, target, startChannel, maxChannel)
                    emit(request.sensors)
                }
            }

        runBlocking {
            controller.feedVirtualSensors(target, requestsFlow)
        }
    }

    private fun validateRequest(
        request: FeedVirtualSensorsRequest,
        target: DetectedHardwareDevice,
        startChannel: Int,
        maxChannel: Int,
    ) {
        request.sensors.keys.forEach { channel ->
            when {
                target.type == DeviceType.D5_NEXT && channel == 0 ->
                    error(
                        "Channel 0 is the coolant temperature sensor on ${target.name}. " +
                            "Virtual sensors are channels 1..8.",
                    )
                channel in 0 until startChannel ->
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
    }
}
