package io.github.lamba92.openaqua.app.mock

import io.github.lamba92.openaqua.engine.device.hardware.RawHidDeviceTransport
import io.github.lamba92.openaqua.engine.host.hardware.HostHardwareController
import org.kodein.di.DI
import org.kodein.di.bindSingleton

val mockEngineModule =
    DI.Module("mockEngineModule") {
        bindSingleton<RawHidDeviceTransport>(overrides = true) { MockRawHidDeviceTransport }
        bindSingleton<HostHardwareController>(overrides = true) { MockHostHardwareController }
    }
