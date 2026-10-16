package com.openaqua.engine.di

import com.github.ajalt.clikt.core.CliktCommand
import com.openaqua.engine.OpenAquaEngine
import com.openaqua.engine.command.CurvesCommand
import com.openaqua.engine.command.DevicesCommand
import com.openaqua.engine.command.FlashCommand
import com.openaqua.engine.command.OpenAquaEngineCommand
import com.openaqua.engine.command.TelemetryCommand
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

val engineCoreModule =
    DI.Module("engineCore") {
        bindSingleton { OpenAquaEngineCommand() }
        bindSingleton { DevicesCommand(instance()) }
        bindSingleton { TelemetryCommand(instance()) }
        bindSingleton { CurvesCommand(instance()) }
        bindSingleton { FlashCommand(instance()) }
        bindSingleton<List<CliktCommand>>(tag = "coreSubcommands") {
            listOf(
                instance<DevicesCommand>(),
                instance<TelemetryCommand>(),
                instance<CurvesCommand>(),
                instance<FlashCommand>(),
            )
        }
        bindSingleton {
            OpenAquaEngine(
                rootCommand = instance(),
                subcommands = instance(),
            )
        }
    }
