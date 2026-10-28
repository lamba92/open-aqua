package io.github.lamba92.openaqua.engine.device.mock

import io.github.lamba92.openaqua.engine.device.runOpenAquaEngine

fun main(args: Array<String>) {
    initializeMockStorage()
    runOpenAquaEngine(args) {
        import(mockEngineModule)
    }
}
