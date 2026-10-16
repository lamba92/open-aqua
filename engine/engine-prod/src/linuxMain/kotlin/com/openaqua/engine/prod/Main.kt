package com.openaqua.engine.prod

import com.openaqua.engine.openAquaEngine

fun main(args: Array<String>) =
    openAquaEngine(args) {
        import(prodEngineModule)
    }
