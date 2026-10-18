package com.openaqua.engine.di

import com.github.ajalt.clikt.core.CliktCommand
import com.openaqua.engine.command.DevicesCommand
import com.openaqua.engine.command.FlashCurvesCommand
import com.openaqua.engine.command.FlashFanCommand
import com.openaqua.engine.command.FlashOffsetsCommand
import com.openaqua.engine.command.OpenAquaEngineCommand
import com.openaqua.engine.command.ReadCurvesCommand
import com.openaqua.engine.command.ReadOffsetsCommand
import com.openaqua.engine.command.StreamTelemetryCommand
import com.openaqua.engine.command.TelemetryCommand
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

val engineCoreModule =
    DI.Module("engineCore") {
        bindSingleton { OpenAquaEngineCommand() }
        bindSingleton { DevicesCommand(instance()) }
        bindSingleton { TelemetryCommand(instance()) }
        bindSingleton { StreamTelemetryCommand(instance()) }
        bindSingleton { ReadCurvesCommand(instance()) }
        bindSingleton { FlashCurvesCommand(instance()) }
        bindSingleton { FlashFanCommand(instance()) }
        bindSingleton { ReadOffsetsCommand(instance()) }
        bindSingleton { FlashOffsetsCommand(instance()) }
        bindSingleton<List<CliktCommand>>(tag = "coreSubcommands") {
            listOf(
                instance<DevicesCommand>(),
                instance<TelemetryCommand>(),
                instance<StreamTelemetryCommand>(),
                instance<ReadCurvesCommand>(),
                instance<FlashCurvesCommand>(),
                instance<FlashFanCommand>(),
                instance<ReadOffsetsCommand>(),
                instance<FlashOffsetsCommand>(),
            )
        }
    }
