package io.github.lamba92.openaqua.engine.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.lamba92.openaqua.model.DescriptorsResponse
import io.github.lamba92.openaqua.model.DeviceRegistry

class DescriptorsCommand :
    CliktCommand(
        name = "descriptors",
    ) {
    override fun help(context: Context): String = "Print supported Aquacomputer device descriptors and hardware specs"

    private val pretty by option("--pretty", help = "Pretty-print JSON output").flag(default = false)

    override fun run() = printSuccessResponse(DescriptorsResponse(DeviceRegistry.ALL), pretty)
}
