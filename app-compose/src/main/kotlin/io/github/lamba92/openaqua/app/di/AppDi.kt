package io.github.lamba92.openaqua.app.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.io.RollingFileLogWriter
import co.touchlab.kermit.io.RollingFileLogWriterConfig
import co.touchlab.kermit.loggerConfigInit
import io.github.lamba92.openaqua.app.coroutine.CoroutineScopeProvider
import io.github.lamba92.openaqua.app.coroutine.DefaultCoroutineScopeProvider
import io.github.lamba92.openaqua.app.feature.device.DeviceViewModel
import io.github.lamba92.openaqua.app.feature.device.fans.FansViewModel
import io.github.lamba92.openaqua.app.feature.device.sensors.SensorsViewModel
import io.github.lamba92.openaqua.app.feature.host.HostViewModel
import io.github.lamba92.openaqua.app.feature.navigation.DeviceNavViewModel
import io.github.lamba92.openaqua.app.feature.settings.SettingsViewModel
import io.github.lamba92.openaqua.app.instance.ForegroundRequestService
import io.github.lamba92.openaqua.app.logging.AppLogService
import io.github.lamba92.openaqua.app.logging.SharedLogSource
import io.github.lamba92.openaqua.app.model.BaseAppDataDir
import io.github.lamba92.openaqua.app.model.LogsDirectory
import io.github.lamba92.openaqua.app.repository.AppDataRepository
import io.github.lamba92.openaqua.app.repository.DeviceDataCacheService
import io.github.lamba92.openaqua.app.repository.OpenAquaDeviceEngineApi
import io.github.lamba92.openaqua.app.repository.OpenAquaHostEngineApi
import io.github.lamba92.openaqua.app.repository.VirtualSensorFeederService
import io.github.lamba92.openaqua.app.util.EngineExtractor
import io.github.lamba92.openaqua.app.util.ExtractedEngines
import io.github.lamba92.openaqua.app.util.nameWithoutExtension
import io.github.lamba92.openaqua.model.device.DeviceId
import org.kodein.di.DI
import org.kodein.di.bindEagerSingleton
import org.kodein.di.bindFactory
import org.kodein.di.bindMultiton
import org.kodein.di.bindSingleton
import org.kodein.di.direct
import org.kodein.di.instance

val appModule =
    DI.Module("appModule") {
        bindSingleton { BaseAppDataDir.default() }
        bindSingleton { instance<BaseAppDataDir>().configFile }
        bindSingleton { instance<BaseAppDataDir>().logsDirectory }
        bindSingleton { instance<BaseAppDataDir>().lockFile }
        bindSingleton { instance<BaseAppDataDir>().portFile }
        bindSingleton { SharedLogSource() }
        bindMultiton { tag: String -> di.direct.instance<Logger>().withTag(tag) }
        bindSingleton<Logger>(tag = "Main") { instance<Logger>().withTag("Main") }
        bindSingleton<Logger> {
            val logsDirectory = instance<LogsDirectory>()
            val rollingFileWriter =
                RollingFileLogWriter(
                    config =
                        RollingFileLogWriterConfig(
                            logFileName = logsDirectory.logFile.nameWithoutExtension,
                            logFilePath = logsDirectory.value,
                            rollOnSize = 5 * 1024 * 1024,
                            maxLogFiles = 1,
                            prependTimestamp = true,
                        ),
                )
            Logger(
                config =
                    loggerConfigInit(
                        CommonWriter(),
                        rollingFileWriter,
                        instance<SharedLogSource>().writer,
                    ),
                tag = "OpenAqua",
            )
        }
        bindSingleton<CoroutineScopeProvider> {
            DefaultCoroutineScopeProvider(baseLogger = instance())
        }
        bindEagerSingleton {
            AppLogService(
                logsDirectory = instance(),
                scopeProvider = instance(),
                logSource = instance(),
            )
        }
        bindSingleton {
            ForegroundRequestService(
                portFile = instance<BaseAppDataDir>().portFile,
                scopeProvider = instance(),
            )
        }
        bindSingleton { EngineExtractor.extract(logger = instance()) }
        bindSingleton { instance<ExtractedEngines>().deviceEngine }
        bindSingleton { instance<ExtractedEngines>().hostEngine }
        bindSingleton { OpenAquaDeviceEngineApi(deviceEnginePath = instance()) }
        bindSingleton { OpenAquaHostEngineApi(hostEnginePath = instance()) }
        bindSingleton { AppDataRepository(coroutineScopeProvider = instance(), baseAppDataDir = instance()) }
        bindSingleton {
            DeviceDataCacheService(
                openAquaDeviceEngineApi = instance(),
                openAquaHostEngineApi = instance(),
                coroutineScopeProvider = instance(),
            )
        }
        bindEagerSingleton {
            VirtualSensorFeederService(
                openAquaDeviceEngineApi = instance(),
                deviceDataCacheService = instance(),
                appDataRepository = instance(),
                coroutineScopeProvider = instance(),
            )
        }
        bindSingleton {
            DeviceNavViewModel(
                dataCacheService = instance(),
                appDataRepository = instance(),
                feederService = instance(),
                scopeProvider = instance(),
            )
        }
        bindSingleton {
            SettingsViewModel(
                appDataRepository = instance(),
                appLogService = instance(),
                scopeProvider = instance(),
            )
        }
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
                feederService = instance(),
                appDataRepository = instance(),
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
                feederService = instance(),
                scopeProvider = instance(),
                deviceId = deviceId,
            )
        }
    }

fun createOpenAquaDI(): DI =
    DI {
        import(appModule)
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
