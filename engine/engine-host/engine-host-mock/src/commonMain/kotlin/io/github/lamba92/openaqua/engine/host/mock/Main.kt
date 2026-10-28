package io.github.lamba92.openaqua.engine.host.mock

import io.github.lamba92.openaqua.engine.host.runOpenAquaHostEngine

fun main(args: Array<String>) =
    runOpenAquaHostEngine(args) {
        import(mockHostEngineModule)
    }
