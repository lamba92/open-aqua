package io.github.lamba92.openaqua.engine.device.di

import com.github.ajalt.clikt.core.CliktCommand
import io.github.lamba92.openaqua.engine.device.command.ConfigCommand
import io.github.lamba92.openaqua.engine.device.command.DescriptorsCommand
import io.github.lamba92.openaqua.engine.device.command.DevicesCommand
import io.github.lamba92.openaqua.engine.device.command.FeedVirtualSensorsCommand
import io.github.lamba92.openaqua.engine.device.command.FlashConfigCommand
import io.github.lamba92.openaqua.engine.device.command.OpenAquaEngineCommand
import io.github.lamba92.openaqua.engine.device.command.ReadConfigCommand
import io.github.lamba92.openaqua.engine.device.command.StreamTelemetryCommand
import io.github.lamba92.openaqua.engine.device.command.TelemetryCommand
import org.kodein.di.DI
import org.kodein.di.bindSet
import org.kodein.di.bindSingleton
import org.kodein.di.instance

val engineCoreModule =
    DI.Module("engineCore") {
        bindSingleton { OpenAquaEngineCommand() }
        bindSingleton { DevicesCommand(instance()) }
        bindSingleton { DescriptorsCommand() }
        bindSingleton { TelemetryCommand(instance()) }
        bindSingleton { StreamTelemetryCommand(instance()) }
        bindSingleton { ReadConfigCommand(instance()) }
        bindSingleton { FlashConfigCommand(instance()) }
        bindSingleton { ConfigCommand(instance(), instance()) }
        bindSingleton { FeedVirtualSensorsCommand(instance()) }
        bindSet<CliktCommand> {
            addSingleton { instance<DevicesCommand>() }
            addSingleton { instance<DescriptorsCommand>() }
            addSingleton { instance<TelemetryCommand>() }
            addSingleton { instance<StreamTelemetryCommand>() }
            addSingleton { instance<ConfigCommand>() }
            addSingleton { instance<FeedVirtualSensorsCommand>() }
        }
    }
