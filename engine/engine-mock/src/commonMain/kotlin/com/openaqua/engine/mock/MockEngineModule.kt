package com.openaqua.engine.mock

import com.openaqua.engine.hardware.HardwareController
import org.kodein.di.DI
import org.kodein.di.bindSingleton

val mockEngineModule =
    DI.Module("mockEngine") {
        bindSingleton<HardwareController> { MockHardwareController() }
    }
