package io.github.lamba92.openaqua.engine.host.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context

class OpenAquaHostEngineCommand :
    CliktCommand(
        name = "open-aqua-host-engine",
    ) {
    override fun help(context: Context): String =
        "OpenAqua host machine hardware engine CLI. Provides system hardware monitoring for CPU, GPU, motherboard, and storage sensors."

    override fun run() = Unit
}
