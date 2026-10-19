package io.github.lamba92.openaqua.engine.prod

import io.github.lamba92.openaqua.engine.hardware.HardwareController
import org.kodein.di.DI
import org.kodein.di.bindSingleton

actual val prodEngineModule: DI.Module =
    DI.Module("prodEngine") {
        bindSingleton<HardwareController> { WindowsHardwareController() }
    }
