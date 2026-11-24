package io.github.lamba92.openaqua.app.di

import co.touchlab.kermit.Logger
import io.github.lamba92.openaqua.app.permission.DevicePermissionService
import io.github.lamba92.openaqua.app.permission.LinuxDevicePermissionService
import io.github.lamba92.openaqua.app.repository.NativeOpenAquaDeviceEngineApi
import io.github.lamba92.openaqua.app.repository.NativeOpenAquaHostEngineApi
import io.github.lamba92.openaqua.app.repository.OpenAquaDeviceEngineApi
import io.github.lamba92.openaqua.app.repository.OpenAquaHostEngineApi
import io.github.lamba92.openaqua.engine.device.di.deviceEngineCoreModule
import io.github.lamba92.openaqua.engine.host.di.hostEngineCoreModule
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

val linuxEngineModule =
    DI.Module("linuxEngineModule") {
        import(deviceEngineCoreModule)
        import(hostEngineCoreModule)
        bindSingleton<OpenAquaDeviceEngineApi> { NativeOpenAquaDeviceEngineApi(hardwareController = instance()) }
        bindSingleton<OpenAquaHostEngineApi> { NativeOpenAquaHostEngineApi(controller = instance()) }
        bindSingleton<DevicePermissionService> {
            LinuxDevicePermissionService(
                scopeProvider = instance(),
                logger = instance<Logger>().withTag("LinuxDevicePermissionService"),
            )
        }
    }

actual val platformEngineModule: DI.Module = linuxEngineModule
