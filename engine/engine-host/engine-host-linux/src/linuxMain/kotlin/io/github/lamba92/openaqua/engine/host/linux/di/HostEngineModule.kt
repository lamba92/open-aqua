package io.github.lamba92.openaqua.engine.host.linux.di

import com.github.ajalt.clikt.core.CliktCommand
import io.github.lamba92.openaqua.engine.host.linux.command.InfoCommand
import io.github.lamba92.openaqua.engine.host.linux.command.OpenAquaHostEngineCommand
import io.github.lamba92.openaqua.engine.host.linux.command.StreamTelemetryCommand
import io.github.lamba92.openaqua.engine.host.linux.hardware.HostHardwareController
import io.github.lamba92.openaqua.engine.host.linux.hardware.LinuxHostHardwareController
import org.kodein.di.DI
import org.kodein.di.bindSet
import org.kodein.di.bindSingleton
import org.kodein.di.instance

val hostEngineModule =
    DI.Module("hostEngine") {
        bindSingleton<HostHardwareController> { LinuxHostHardwareController() }
        bindSingleton { OpenAquaHostEngineCommand() }
        bindSingleton { InfoCommand(instance()) }
        bindSingleton { StreamTelemetryCommand(instance()) }
        bindSet<CliktCommand> {
            addSingleton { instance<InfoCommand>() }
            addSingleton { instance<StreamTelemetryCommand>() }
        }
    }
