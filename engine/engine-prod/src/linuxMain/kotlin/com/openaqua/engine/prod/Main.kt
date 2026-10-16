package com.openaqua.engine.prod

import com.openaqua.engine.OpenAquaEngine
import com.openaqua.engine.di.engineCoreModule
import org.kodein.di.DI
import org.kodein.di.instance

fun main(args: Array<String>) {
    val di =
        DI {
            import(engineCoreModule)
            import(prodEngineModule)
        }
    val engine by di.instance<OpenAquaEngine>()
    engine.execute(args)
}
