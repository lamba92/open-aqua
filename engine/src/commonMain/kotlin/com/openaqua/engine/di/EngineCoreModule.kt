package com.openaqua.engine.di

import com.github.ajalt.clikt.core.CliktCommand
import com.openaqua.engine.OpenAquaEngine
import com.openaqua.engine.command.CurvesCommand
import com.openaqua.engine.command.DevicesCommand
import com.openaqua.engine.command.FlashCommand
import com.openaqua.engine.command.OpenAquaEngineCommand
import com.openaqua.engine.command.TelemetryCommand
import kotlinx.serialization.json.Json
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

val engineCoreModule =
    DI.Module("engineCore") {
        bindSingleton<Json> {
            Json {
                prettyPrint = false
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        }
        bindSingleton { OpenAquaEngineCommand() }
        bindSingleton { DevicesCommand(instance(), instance()) }
        bindSingleton { TelemetryCommand(instance(), instance()) }
        bindSingleton { CurvesCommand(instance(), instance()) }
        bindSingleton { FlashCommand(instance(), instance()) }
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
                json = instance(),
            )
        }
    }
