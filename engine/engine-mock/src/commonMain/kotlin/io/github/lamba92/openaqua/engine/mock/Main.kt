package io.github.lamba92.openaqua.engine.mock

import io.github.lamba92.openaqua.engine.runOpenAquaEngine

fun main(args: Array<String>) =
    runOpenAquaEngine(args) {
        import(mockEngineModule)
    }
