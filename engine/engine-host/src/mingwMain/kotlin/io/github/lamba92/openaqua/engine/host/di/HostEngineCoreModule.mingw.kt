package io.github.lamba92.openaqua.engine.host.di

import io.github.lamba92.libhwmon.HwmonClient
import io.github.lamba92.libhwmon.HwmonConfig
import io.github.lamba92.openaqua.engine.host.hardware.HostHardwareController
import io.github.lamba92.openaqua.engine.host.windows.hardware.WindowsHostHardwareController
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

actual val hostEngineCoreModule: DI.Module =
    DI.Module("mingwHostEngineCore") {
        bindSingleton {
            HwmonConfig(
                enableCpu = true,
                enableGpu = true,
                enableMotherboard = true,
                enableStorage = true,
                enableController = true,
                enableMemory = false,
                enableNetwork = false,
                enableBattery = false,
            )
        }
        bindSingleton { HwmonClient(instance()) }
        bindSingleton<HostHardwareController> { WindowsHostHardwareController(client = instance()) }
    }
