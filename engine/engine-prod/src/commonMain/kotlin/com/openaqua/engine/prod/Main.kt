package com.openaqua.engine.prod

import com.openaqua.engine.runOpenAquaEngine

fun main(args: Array<String>) =
    runOpenAquaEngine(args) {
        import(prodEngineModule)
    }
