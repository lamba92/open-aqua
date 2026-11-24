package io.github.lamba92.openaqua.app.di

import co.touchlab.kermit.Logger
import io.github.lamba92.openaqua.app.permission.DevicePermissionService
import io.github.lamba92.openaqua.app.permission.LinuxDevicePermissionService
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

val linuxAppModule =
    DI.Module("linuxAppModule") {
        bindSingleton<DevicePermissionService> {
            LinuxDevicePermissionService(
                scopeProvider = instance(),
                logger = instance<Logger>().withTag("LinuxDevicePermissionService"),
            )
        }
    }

actual val platformAppModule: DI.Module = linuxAppModule
