package io.github.lamba92.openaqua.engine.host.mock

import io.github.lamba92.openaqua.engine.host.hardware.HostHardwareController
import org.kodein.di.DI
import org.kodein.di.bindSingleton

val mockHostEngineModule =
    DI.Module("mockHostEngine") {
        bindSingleton<HostHardwareController> { MockHostHardwareController() }
    }
