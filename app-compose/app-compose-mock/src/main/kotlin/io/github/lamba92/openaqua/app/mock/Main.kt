package io.github.lamba92.openaqua.app.mock

import io.github.lamba92.openaqua.app.di.createOpenAquaDI
import io.github.lamba92.openaqua.app.startOpenAquaApplication

fun main() {
    startOpenAquaApplication(createOpenAquaDI(mockEngineModule))
}
