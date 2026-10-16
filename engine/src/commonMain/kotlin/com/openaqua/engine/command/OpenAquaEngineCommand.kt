package com.openaqua.engine.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

class OpenAquaEngineCommand :
    CliktCommand(
        name = "open-aqua-engine",
    ) {
    override fun help(context: Context): String = "OpenAqua native hardware engine CLI"

    private val pretty by option("--pretty", help = "Pretty-print JSON output").flag(default = false)

    override fun run() {
        if (pretty) {
            currentContext.findOrSetObject { true }
        }
    }
}
