package io.github.lamba92.openaqua.app.di

import io.github.lamba92.openaqua.app.permission.AlwaysGrantedDevicePermissionService
import io.github.lamba92.openaqua.app.permission.DevicePermissionService
import org.kodein.di.DI
import org.kodein.di.bindSingleton

val mingwAppModule =
    DI.Module("mingwAppModule") {
        bindSingleton<DevicePermissionService> { AlwaysGrantedDevicePermissionService }
    }

actual val platformAppModule: DI.Module = mingwAppModule
