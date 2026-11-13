package io.github.lamba92.openaqua.engine.host.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import io.github.lamba92.openaqua.engine.host.hardware.HostHardwareController

class RestoreFanDefaultsCommand(
    private val controller: HostHardwareController,
) : CliktCommand(
        name = "restore-fan-defaults",
    ) {
    override fun help(context: Context): String =
        "One-shot recovery command to restore all host fan speed controls to BIOS/hardware defaults"

    override fun run() {
        controller.restoreFanDefaults()
    }
}
