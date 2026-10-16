package com.openaqua.engine.prod

import com.github.ajalt.clikt.core.CliktCommand
import com.openaqua.engine.hardware.HardwareController
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

val prodEngineModule =
    DI.Module("prodEngine") {
        bindSingleton<HardwareController> { LinuxHardwareController() }
        bindSingleton<List<CliktCommand>> { instance(tag = "coreSubcommands") }
    }
