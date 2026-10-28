package io.github.lamba92.openaqua.engine.host.linux.di

import io.github.lamba92.openaqua.engine.host.hardware.HostHardwareController
import io.github.lamba92.openaqua.engine.host.linux.hardware.LinuxHostHardwareController
import io.github.lamba92.openaqua.engine.host.linux.hardware.NvidiaCapability
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

val linuxHostEngineModule =
    DI.Module("linuxHostEngine") {
        bindSingleton<HostHardwareController> { LinuxHostHardwareController(instance()) }
        bindSingleton { NvidiaCapability.get() }
    }
