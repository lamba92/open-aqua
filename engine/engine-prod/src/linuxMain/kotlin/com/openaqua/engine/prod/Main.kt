package com.openaqua.engine.prod

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
            import(prodEngineModule)
        }
    val rootCommand by di.instance<OpenAquaEngineCommand>()
    val subcommands by di.instance<List<CliktCommand>>()
    openAquaEngine(rootCommand, subcommands, args)
}
