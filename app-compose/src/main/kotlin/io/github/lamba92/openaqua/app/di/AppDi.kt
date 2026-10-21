package io.github.lamba92.openaqua.app.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.DefaultCoroutineScopeProvider
import io.github.lamba92.openaqua.app.feature.fans.FansViewModel
import io.github.lamba92.openaqua.app.feature.info.DeviceInfoViewModel
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavViewModel
import io.github.lamba92.openaqua.app.feature.sensors.SensorsViewModel
import io.github.lamba92.openaqua.app.repository.OpenAquaEngineApi
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.direct
import org.kodein.di.instance
import java.nio.file.Path

fun createAppModule(
    enginePath: Path,
    scopeProvider: CoroutineScopeProvider = DefaultCoroutineScopeProvider(),
) = DI.Module("appModule") {
    bindSingleton<CoroutineScopeProvider> { scopeProvider }
    bindSingleton<OpenAquaEngineApi> {
        OpenAquaEngineApi(
            enginePath = enginePath,
        )
    }
    bindSingleton { DeviceNavViewModel(instance(), instance()) }
    bindSingleton { DeviceInfoViewModel(instance(), instance()) }
    bindSingleton { FansViewModel(instance(), instance()) }
    bindSingleton { SensorsViewModel(instance(), instance()) }
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
    return remember(di, tag) { di.direct.instance<T>(tag) }
}
