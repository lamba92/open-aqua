package com.openaqua.engine.prod

import com.openaqua.engine.hardware.HardwareController
import org.kodein.di.DI
import org.kodein.di.bindSingleton

actual val prodEngineModule: DI.Module =
    DI.Module("prodEngine") {
        bindSingleton<HardwareController> { LinuxHardwareController() }
    }
