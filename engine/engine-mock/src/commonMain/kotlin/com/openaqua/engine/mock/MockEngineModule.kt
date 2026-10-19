package com.openaqua.engine.mock

import com.github.ajalt.clikt.core.CliktCommand
import com.openaqua.engine.hardware.HardwareController
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
