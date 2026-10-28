package io.github.lamba92.openaqua.engine.host.linux

import io.github.lamba92.openaqua.engine.host.linux.di.linuxHostEngineModule
import io.github.lamba92.openaqua.engine.host.runOpenAquaHostEngine

fun main(args: Array<String>) =
    runOpenAquaHostEngine(args) {
        import(linuxHostEngineModule)
    }
