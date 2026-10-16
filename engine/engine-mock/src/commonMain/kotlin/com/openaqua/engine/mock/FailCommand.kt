package com.openaqua.engine.mock

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option

class FailCommand :
    CliktCommand(
        name = "fail",
    ) {
    override fun help(context: Context): String = "Simulate a hardware engine failure"

    private val message by option("--message", help = "Failure error message")
        .default("Simulated hardware failure")

    override fun run() {
        error(message)
    }
}
