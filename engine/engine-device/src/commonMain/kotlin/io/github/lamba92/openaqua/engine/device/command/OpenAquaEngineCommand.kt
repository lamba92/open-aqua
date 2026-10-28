package io.github.lamba92.openaqua.engine.device.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context

class OpenAquaEngineCommand :
    CliktCommand(
        name = "open-aqua-device-engine",
    ) {
    override fun help(context: Context): String =
        "OpenAqua device hardware engine CLI. Provides low-level USB HID hardware control for supported devices. Supported devices: OCTO (physically verified), QUADRO (experimental), D5 Next (experimental)."

    override fun run() = Unit
}
