package io.github.lamba92.openaqua.engine.host.di

import com.github.ajalt.clikt.core.CliktCommand
import io.github.lamba92.openaqua.engine.host.command.InfoCommand
import io.github.lamba92.openaqua.engine.host.command.OpenAquaHostEngineCommand
import io.github.lamba92.openaqua.engine.host.command.RestoreFanDefaultsCommand
import io.github.lamba92.openaqua.engine.host.command.StreamFansCommand
import io.github.lamba92.openaqua.engine.host.command.StreamTelemetryCommand
import org.kodein.di.DI
import org.kodein.di.bindSet
import org.kodein.di.bindSingleton
import org.kodein.di.instance

val hostEngineCoreModule =
    DI.Module("hostEngineCore") {
        bindSingleton { OpenAquaHostEngineCommand() }
        bindSingleton { InfoCommand(instance()) }
        bindSingleton { StreamTelemetryCommand(instance()) }
        bindSingleton { StreamFansCommand(instance()) }
        bindSingleton { RestoreFanDefaultsCommand(instance()) }
        bindSet<CliktCommand> {
            addSingleton { instance<InfoCommand>() }
            addSingleton { instance<StreamTelemetryCommand>() }
            addSingleton { instance<StreamFansCommand>() }
            addSingleton { instance<RestoreFanDefaultsCommand>() }
        }
    }
