package com.openaqua.engine.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.double
import com.openaqua.engine.hardware.HardwareController
import com.openaqua.engine.hardware.findTarget

class SensorOffsetCommand(
    private val controller: HardwareController,
) : CliktCommand(
        name = "sensor-offset",
    ) {
    override fun help(context: Context): String = "Set temperature sensor calibration offset"

    private val device by option("--device", help = "Device ID or alias (e.g. octo-1)").required()
    private val sensor by option("--sensor", help = "Sensor ID (e.g. temp1)").required()
    private val offset by option("--offset", help = "Calibration offset in degrees Celsius (e.g. 1.5, -0.5)").double().required()
    private val pretty by option("--pretty", help = "Pretty-print JSON output").flag(default = false)

    override fun run() {
        val devices = controller.scanDevices()
        val target =
            devices.findTarget(device)
                ?: error("Device '$device' not found")

        val response = controller.setSensorOffset(target, sensor, offset)
        printResponse(response, isPretty(pretty))
    }
}
