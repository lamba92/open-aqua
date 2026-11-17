package io.github.lamba92.openaqua.app.di

import io.github.lamba92.openaqua.app.permission.AlwaysGrantedDevicePermissionService
import io.github.lamba92.openaqua.app.permission.DevicePermissionService
import io.github.lamba92.openaqua.app.repository.ExecutableOpenAquaHostEngineApi
import io.github.lamba92.openaqua.app.repository.NativeOpenAquaDeviceEngineApi
import io.github.lamba92.openaqua.app.repository.OpenAquaDeviceEngineApi
import io.github.lamba92.openaqua.app.repository.OpenAquaHostEngineApi
import io.github.lamba92.openaqua.engine.device.prod.prodEngineModule
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

val mingwEngineModule =
    DI.Module("mingwEngineModule") {
        import(prodEngineModule)
        bindSingleton<OpenAquaDeviceEngineApi> { NativeOpenAquaDeviceEngineApi(hardwareController = instance()) }
        bindSingleton<OpenAquaHostEngineApi> { ExecutableOpenAquaHostEngineApi(logger = instance()) }
        bindSingleton<DevicePermissionService> { AlwaysGrantedDevicePermissionService }
    }

actual val platformEngineModule: DI.Module = mingwEngineModule
