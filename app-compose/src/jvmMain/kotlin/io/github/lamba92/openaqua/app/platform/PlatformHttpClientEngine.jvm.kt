package io.github.lamba92.openaqua.app.platform

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO

actual val platformHttpClientEngine: HttpClientEngineFactory<*> = CIO
