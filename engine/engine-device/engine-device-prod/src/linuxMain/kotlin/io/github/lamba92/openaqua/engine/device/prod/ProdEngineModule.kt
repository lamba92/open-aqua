package io.github.lamba92.openaqua.engine.device.prod

import io.github.lamba92.openaqua.engine.device.hardware.HardwareController
import org.kodein.di.DI
import org.kodein.di.bindSingleton

actual val prodEngineModule: DI.Module =
    DI.Module("prodEngine") {
        bindSingleton<HardwareController> { LinuxHardwareController }
    }
