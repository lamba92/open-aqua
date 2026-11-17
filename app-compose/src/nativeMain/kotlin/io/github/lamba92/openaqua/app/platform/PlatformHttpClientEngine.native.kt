package io.github.lamba92.openaqua.app.platform

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.curl.Curl

actual val platformHttpClientEngine: HttpClientEngineFactory<*> = Curl
