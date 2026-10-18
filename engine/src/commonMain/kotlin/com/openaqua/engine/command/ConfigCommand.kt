package com.openaqua.engine.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands

class ConfigCommand(
    readCommand: ReadConfigCommand,
    flashCommand: FlashConfigCommand,
) : CliktCommand(
        name = "config",
    ) {
    init {
        subcommands(readCommand, flashCommand)
    }

    override fun help(context: Context): String = "Read or flash hardware configuration"

    override fun run() = Unit
}
