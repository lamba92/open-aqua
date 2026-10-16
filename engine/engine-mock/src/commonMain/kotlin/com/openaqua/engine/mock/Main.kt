package com.openaqua.engine.mock

import com.openaqua.engine.runOpenAquaEngine

fun main(args: Array<String>) =
    runOpenAquaEngine(args) {
        import(mockEngineModule)
    }
