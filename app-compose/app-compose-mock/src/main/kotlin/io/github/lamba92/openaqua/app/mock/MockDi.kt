package io.github.lamba92.openaqua.app.mock

import io.github.lamba92.openaqua.app.repository.OpenAquaDeviceEngineApi
import io.github.lamba92.openaqua.app.repository.OpenAquaHostEngineApi
import org.kodein.di.DI
import org.kodein.di.bindSingleton

val mockEngineModule =
    DI.Module("mockEngineModule") {
        bindSingleton<OpenAquaDeviceEngineApi>(overrides = true) { MockOpenAquaDeviceEngineApi() }
        bindSingleton<OpenAquaHostEngineApi>(overrides = true) { MockOpenAquaHostEngineApi() }
    }
