package io.github.lamba92.openaqua.engine.host.di

import io.github.lamba92.openaqua.engine.host.hardware.HostHardwareController
import io.github.lamba92.openaqua.engine.host.linux.hardware.LinuxHostHardwareController
import io.github.lamba92.openaqua.engine.host.linux.hardware.NvidiaCapability
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

actual val hostEngineCoreModule: DI.Module =
    DI.Module("linuxHostEngineCore") {
        bindSingleton<HostHardwareController> { LinuxHostHardwareController(instance()) }
        bindSingleton { NvidiaCapability.get() }
    }
