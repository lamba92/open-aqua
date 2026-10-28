package io.github.lamba92.openaqua.engine.host.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.lamba92.openaqua.engine.host.hardware.HostHardwareController

class InfoCommand(
    private val controller: HostHardwareController,
) : CliktCommand(
        name = "info",
    ) {
    override fun help(context: Context): String = "Get connected host devices and available sensors"

    private val pretty by option("--pretty", help = "Format JSON output with indentation").flag(default = false)

    override fun run() {
        val info = controller.getHostInfo()
        printSuccessResponse(info, pretty)
    }
}
