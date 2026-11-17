package io.github.lamba92.openaqua.app.di

import org.kodein.di.DI

expect val platformEngineModule: DI.Module

val nativeAppModule =
    DI.Module("nativeAppModule") {
        import(platformEngineModule)
    }

fun createOpenAquaNativeDI(vararg overrideModules: DI.Module): DI =
    DI {
        import(commonAppModule)
        import(nativeAppModule)
        overrideModules.forEach { import(it, allowOverride = true) }
    }
