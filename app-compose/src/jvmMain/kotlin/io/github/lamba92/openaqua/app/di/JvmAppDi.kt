package io.github.lamba92.openaqua.app.di

import co.touchlab.kermit.Logger
import io.github.lamba92.openaqua.app.instance.ForegroundRequestService
import io.github.lamba92.openaqua.app.model.BaseAppDataDir
import io.github.lamba92.openaqua.app.permission.AlwaysGrantedDevicePermissionService
import io.github.lamba92.openaqua.app.permission.DevicePermissionService
import io.github.lamba92.openaqua.app.permission.LinuxDevicePermissionService
import io.github.lamba92.openaqua.app.permission.WindowsDevicePermissionService
import io.github.lamba92.openaqua.app.repository.ExecutableOpenAquaDeviceEngineApi
import io.github.lamba92.openaqua.app.repository.ExecutableOpenAquaHostEngineApi
import io.github.lamba92.openaqua.app.repository.OpenAquaDeviceEngineApi
import io.github.lamba92.openaqua.app.repository.OpenAquaHostEngineApi
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

val jvmEngineModule =
    DI.Module("jvmEngineModule") {
        bindSingleton<OpenAquaDeviceEngineApi> { ExecutableOpenAquaDeviceEngineApi(logger = instance()) }
        bindSingleton<OpenAquaHostEngineApi> { ExecutableOpenAquaHostEngineApi(logger = instance()) }
    }

val jvmAppModule =
    DI.Module("jvmAppModule") {
        import(jvmEngineModule)
        bindSingleton {
            ForegroundRequestService(
                portFile = instance<BaseAppDataDir>().portFile,
                scopeProvider = instance(),
            )
        }
        bindSingleton<DevicePermissionService> {
            val os = System.getProperty("os.name").lowercase()
            when {
                "linux" in os -> {
                    LinuxDevicePermissionService(
                        scopeProvider = instance(),
                        logger = instance<Logger>().withTag("LinuxDevicePermissionService"),
                    )
                }

                "win" in os -> {
                    WindowsDevicePermissionService(
                        scopeProvider = instance(),
                    )
                }

                else -> {
                    AlwaysGrantedDevicePermissionService
                }
            }
        }
    }

fun createOpenAquaDI(vararg overrideModules: DI.Module): DI =
    DI {
        import(commonAppModule)
        import(jvmAppModule)
        overrideModules.forEach { import(it, allowOverride = true) }
    }
