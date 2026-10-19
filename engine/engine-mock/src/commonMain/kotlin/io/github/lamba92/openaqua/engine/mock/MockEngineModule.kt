package io.github.lamba92.openaqua.engine.mock

import com.github.ajalt.clikt.core.CliktCommand
import io.github.lamba92.openaqua.engine.hardware.HardwareController
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.inBindSet
import org.kodein.di.instance

val mockEngineModule =
    DI.Module("mockEngine") {
        bindSingleton<HardwareController> { MockHardwareController() }
        bindSingleton { FailCommand() }
        inBindSet<CliktCommand> {
            addSingleton { instance<FailCommand>() }
        }
    }
