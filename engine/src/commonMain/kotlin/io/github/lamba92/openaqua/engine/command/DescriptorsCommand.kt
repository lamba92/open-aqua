package io.github.lamba92.openaqua.engine.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.lamba92.openaqua.model.DescriptorsResponse
import io.github.lamba92.openaqua.model.DeviceRegistry
import io.github.lamba92.openaqua.model.DeviceType

class DescriptorsCommand :
    CliktCommand(
        name = "descriptors",
    ) {
    override fun help(context: Context): String = "Print supported Aquacomputer device descriptors and hardware specs"

    private val type by option("-t", "--type", help = "Filter by device type (e.g. OCTO, QUADRO, D5_NEXT)")
    private val pretty by option("--pretty", help = "Pretty-print JSON output").flag(default = false)

    override fun run() {
        val descriptors =
            when {
                type != null -> {
                    val matchingType =
                        DeviceType.entries.firstOrNull { it.name.equals(type, ignoreCase = true) }
                            ?: error("Unknown device type '$type'. Supported types: ${DeviceType.entries.joinToString()}")
                    listOf(DeviceRegistry.findByType(matchingType))
                }
                else -> DeviceRegistry.ALL
            }
        printResponse(DescriptorsResponse(descriptors), pretty)
    }
}
