package io.github.lamba92.openaqua.app.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.DefaultCoroutineScopeProvider
import io.github.lamba92.openaqua.app.feature.device.d5next.D5NextViewModel
import io.github.lamba92.openaqua.app.feature.device.octo.OctoViewModel
import io.github.lamba92.openaqua.app.feature.device.quadro.QuadroViewModel
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavViewModel
import io.github.lamba92.openaqua.app.repository.DeviceDataCacheService
import io.github.lamba92.openaqua.app.repository.OpenAquaEngineApi
import io.github.lamba92.openaqua.model.DeviceId
import org.kodein.di.DI
import org.kodein.di.bindMultiton
import org.kodein.di.bindSingleton
import org.kodein.di.direct
import org.kodein.di.instance
import java.nio.file.Path

fun createAppModule(
    enginePath: Path,
    scopeProvider: CoroutineScopeProvider = DefaultCoroutineScopeProvider(),
) = DI.Module("appModule") {
    bindSingleton<CoroutineScopeProvider> { scopeProvider }
    bindSingleton<OpenAquaEngineApi> { OpenAquaEngineApi(enginePath = enginePath) }
    bindSingleton { DeviceDataCacheService(openAquaEngineApi = instance(), coroutineScopeProvider = instance()) }
    bindSingleton { DeviceNavViewModel(dataCacheService = instance(), scopeProvider = instance()) }
    bindMultiton { deviceId: DeviceId ->
        OctoViewModel(
            repository = instance(),
            cacheService = instance(),
            scopeProvider = instance(),
            deviceId = deviceId,
        )
    }
    bindMultiton { deviceId: DeviceId ->
        QuadroViewModel(
            repository = instance(),
            cacheService = instance(),
            scopeProvider = instance(),
            deviceId = deviceId,
        )
    }
    bindMultiton { deviceId: DeviceId ->
        D5NextViewModel(
            repository = instance(),
            cacheService = instance(),
            scopeProvider = instance(),
            deviceId = deviceId,
        )
    }
}

fun createOpenAquaDI(
    enginePath: Path,
    scopeProvider: CoroutineScopeProvider = DefaultCoroutineScopeProvider(),
): DI =
    DI {
        import(createAppModule(enginePath, scopeProvider))
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
