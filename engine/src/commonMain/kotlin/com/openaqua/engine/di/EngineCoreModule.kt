package com.openaqua.engine.di

import com.github.ajalt.clikt.core.CliktCommand
import com.openaqua.engine.command.ConfigCommand
import com.openaqua.engine.command.DevicesCommand
import com.openaqua.engine.command.FeedVirtualSensorsCommand
import com.openaqua.engine.command.FlashConfigCommand
import com.openaqua.engine.command.OpenAquaEngineCommand
import com.openaqua.engine.command.ReadConfigCommand
import com.openaqua.engine.command.StreamTelemetryCommand
import com.openaqua.engine.command.TelemetryCommand
import org.kodein.di.DI
import org.kodein.di.bindSet
import org.kodein.di.bindSingleton
import org.kodein.di.instance

val engineCoreModule =
    DI.Module("engineCore") {
        bindSingleton { OpenAquaEngineCommand() }
        bindSingleton { DevicesCommand(instance()) }
        bindSingleton { TelemetryCommand(instance()) }
        bindSingleton { StreamTelemetryCommand(instance()) }
        bindSingleton { ReadConfigCommand(instance()) }
        bindSingleton { FlashConfigCommand(instance()) }
        bindSingleton { ConfigCommand(instance(), instance()) }
        bindSingleton { FeedVirtualSensorsCommand(instance()) }
        bindSet<CliktCommand> {
            addSingleton { instance<DevicesCommand>() }
            addSingleton { instance<TelemetryCommand>() }
            addSingleton { instance<StreamTelemetryCommand>() }
            addSingleton { instance<ConfigCommand>() }
            addSingleton { instance<FeedVirtualSensorsCommand>() }
        }
    }
