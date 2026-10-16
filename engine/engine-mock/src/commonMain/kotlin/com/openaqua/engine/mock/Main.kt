package com.openaqua.engine.mock

import com.openaqua.engine.openAquaEngine

fun main(args: Array<String>) =
    openAquaEngine(args) {
        import(mockEngineModule)
    }
