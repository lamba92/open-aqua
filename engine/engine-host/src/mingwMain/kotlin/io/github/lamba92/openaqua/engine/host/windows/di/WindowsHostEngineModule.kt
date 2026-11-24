package io.github.lamba92.openaqua.engine.host.windows.di

import io.github.lamba92.openaqua.engine.host.hardware.HostHardwareController
import io.github.lamba92.openaqua.engine.host.windows.hardware.WindowsHostHardwareController
import org.kodein.di.DI
import org.kodein.di.bindSingleton

val windowsHostEngineModule =
    DI.Module("windowsHostEngine") {
        bindSingleton<HostHardwareController> { WindowsHostHardwareController() }
    }
