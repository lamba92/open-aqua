package com.openaqua.engine.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.int
import com.openaqua.engine.hardware.HardwareController
import com.openaqua.engine.hardware.findTarget

class FlashOffsetsCommand(
    private val controller: HardwareController,
) : CliktCommand(
        name = "flash-offsets",
    ) {
    override fun help(context: Context): String = "Set temperature sensor calibration offset"

    private val device by option("--device", help = "Device ID or alias (e.g. octo-1)").required()
    private val channel by option("--channel", "--sensor", help = "Sensor channel index (0-indexed)").int().required()
    private val offset by option("--offset", help = "Calibration offset in degrees Celsius (e.g. 1.5, -0.5)").double().required()
    private val pretty by option("--pretty", help = "Pretty-print JSON output").flag(default = false)

    override fun run() {
        val devices = controller.scanDevices()
        val target =
            devices.findTarget(device)
                ?: error("Device '$device' not found")

        val response = controller.setSensorOffset(target, channel, offset)
        printResponse(response, pretty)
    }
}
