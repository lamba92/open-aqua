package io.github.lamba92.openaqua.app.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.DefaultCoroutineScopeProvider
import io.github.lamba92.openaqua.app.feature.device.DeviceViewModel
import io.github.lamba92.openaqua.app.feature.device.fans.FansViewModel
import io.github.lamba92.openaqua.app.feature.device.sensors.SensorsViewModel
import io.github.lamba92.openaqua.app.feature.host.HostViewModel
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavViewModel
import io.github.lamba92.openaqua.app.feature.settings.SettingsViewModel
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import io.github.lamba92.openaqua.app.repository.DeviceDataCacheService
import io.github.lamba92.openaqua.app.repository.OpenAquaDeviceEngineApi
import io.github.lamba92.openaqua.app.repository.OpenAquaHostEngineApi
import io.github.lamba92.openaqua.model.device.DeviceId
import org.kodein.di.DI
import org.kodein.di.bindMultiton
import org.kodein.di.bindSingleton
import org.kodein.di.direct
import org.kodein.di.instance
import java.nio.file.Path

fun createAppModule(
    deviceEnginePath: Path,
    hostEnginePath: Path,
    scopeProvider: CoroutineScopeProvider = DefaultCoroutineScopeProvider(),
) = DI.Module("appModule") {
    bindSingleton<CoroutineScopeProvider> { scopeProvider }
    bindSingleton<OpenAquaDeviceEngineApi> { OpenAquaDeviceEngineApi(enginePath = deviceEnginePath) }
    bindSingleton<OpenAquaHostEngineApi> { OpenAquaHostEngineApi(enginePath = hostEnginePath) }
    bindSingleton { AppDataRepository(coroutineScopeProvider = instance()) }
    bindSingleton {
        DeviceDataCacheService(
            openAquaDeviceEngineApi = instance(),
            openAquaHostEngineApi = instance(),
            coroutineScopeProvider = instance(),
        )
    }
    bindSingleton { DeviceNavViewModel(dataCacheService = instance(), scopeProvider = instance()) }
    bindSingleton { SettingsViewModel(appDataRepository = instance(), scopeProvider = instance()) }
    bindSingleton {
        HostViewModel(
            dataCacheService = instance(),
            appDataRepository = instance(),
            scopeProvider = instance(),
        )
    }
    bindMultiton { deviceId: DeviceId ->
        DeviceViewModel(
            cacheService = instance(),
            scopeProvider = instance(),
            deviceId = deviceId,
        )
    }
    bindMultiton { deviceId: DeviceId ->
        FansViewModel(
            cacheService = instance(),
            appDataRepository = instance(),
            scopeProvider = instance(),
            deviceId = deviceId,
        )
    }
    bindMultiton { deviceId: DeviceId ->
        SensorsViewModel(
            cacheService = instance(),
            appDataRepository = instance(),
            scopeProvider = instance(),
            deviceId = deviceId,
        )
    }
}

fun createOpenAquaDI(
    deviceEnginePath: Path,
    hostEnginePath: Path,
    scopeProvider: CoroutineScopeProvider = DefaultCoroutineScopeProvider(),
): DI =
    DI {
        import(createAppModule(deviceEnginePath, hostEnginePath, scopeProvider))
    }

val LocalDI = staticCompositionLocalOf<DI> { error("No DI container provided") }

@Composable
inline fun <reified T : Any> rememberInstance(tag: Any? = null): T {
    val di = LocalDI.current
    return remember(di, tag) { di.direct.instance(tag) }
}

@Composable
inline fun <reified T : Any, reified A : Any> rememberInstance(
    tag: Any? = null,
    arg: A,
): T {
    val di = LocalDI.current
    return remember(di, tag, arg) { di.direct.instance(tag, arg) }
}
