package com.openaqua.engine.mock

import com.github.ajalt.clikt.core.CliktCommand
import com.openaqua.engine.command.OpenAquaEngineCommand
import com.openaqua.engine.di.engineCoreModule
import com.openaqua.engine.openAquaEngine
import org.kodein.di.DI
import org.kodein.di.instance

fun main(args: Array<String>) {
    val di =
        DI {
            import(engineCoreModule)
            import(mockEngineModule)
        }
    val rootCommand by di.instance<OpenAquaEngineCommand>()
    val subcommands by di.instance<List<CliktCommand>>()
    openAquaEngine(rootCommand, subcommands, args)
}
