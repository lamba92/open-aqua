package io.github.lamba92.openaqua.engine.device.di

import io.github.lamba92.openaqua.engine.device.hardware.RawHidDeviceTransport
import io.github.lamba92.openaqua.engine.device.transport.WindowsRawHidDeviceTransport
import org.kodein.di.DI
import org.kodein.di.bindSingleton

actual val platformDeviceEngineModule: DI.Module =
    DI.Module("mingwDeviceEngine") {
        bindSingleton<RawHidDeviceTransport> { WindowsRawHidDeviceTransport }
    }
