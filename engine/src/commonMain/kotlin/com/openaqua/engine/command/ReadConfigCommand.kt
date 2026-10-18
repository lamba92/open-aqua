package com.openaqua.engine.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.openaqua.engine.hardware.HardwareController
import com.openaqua.engine.hardware.findTarget

class ReadConfigCommand(
    private val controller: HardwareController,
) : CliktCommand(
        name = "read",
    ) {
    override fun help(context: Context): String = "Read hardware configuration"

    private val device by option("--device", help = "Device ID or alias (e.g. octo-1)").required()
    private val pretty by option("--pretty", help = "Pretty-print JSON output").flag(default = false)

    override fun run() {
        val devices = controller.scanDevices()
        val target =
            devices.findTarget(device)
                ?: error("Device '$device' not found")

        val config = controller.readConfig(target)
        printResponse(config, pretty)
    }
}
