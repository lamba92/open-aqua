package io.github.lamba92.openaqua.app

import androidx.compose.runtime.remember
import com.compose.sdl.nativeComposeApp
import io.github.lamba92.openaqua.app.di.createOpenAquaDI
import io.github.lamba92.openaqua.app.instance.acquireOrNotify

fun main(args: Array<String>) {
    acquireOrNotify()
    nativeComposeApp {
        val di = remember { createOpenAquaDI() }
        OpenAquaApp(di = di, "--minimized" in args)
    }
}
