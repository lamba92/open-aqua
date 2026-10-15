package com.openaqua.engine.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context

class OpenAquaEngineCommand :
    CliktCommand(
        name = "open-aqua-engine",
    ) {
    override fun help(context: Context): String = "OpenAqua native hardware engine CLI"

    override fun run() = Unit
}
