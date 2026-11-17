package io.github.lamba92.openaqua.app.platform

import io.ktor.client.engine.HttpClientEngineFactory

expect val platformHttpClientEngine: HttpClientEngineFactory<*>
