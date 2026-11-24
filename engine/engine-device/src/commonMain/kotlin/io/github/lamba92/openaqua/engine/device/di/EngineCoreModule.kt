package io.github.lamba92.openaqua.engine.device.di

import io.github.lamba92.openaqua.engine.device.hardware.HardwareController
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

expect val platformDeviceEngineModule: DI.Module

val deviceEngineCoreModule =
    DI.Module("deviceEngineCore") {
        import(platformDeviceEngineModule)
        bindSingleton<HardwareController> { HardwareController(transport = instance()) }
    }
