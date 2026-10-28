package io.github.lamba92.openaqua.engine.device.prod

import io.github.lamba92.openaqua.engine.device.runOpenAquaEngine

fun main(args: Array<String>) =
    runOpenAquaEngine(args) {
        import(prodEngineModule)
    }
