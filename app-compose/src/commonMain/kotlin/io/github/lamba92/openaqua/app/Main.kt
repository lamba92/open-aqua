package io.github.lamba92.openaqua.app

import com.compose.sdl.nativeComposeApp
import io.github.lamba92.openaqua.app.di.rememberOpenAquaDI
import io.github.lamba92.openaqua.app.instance.acquireOrNotify
import io.github.lamba92.openaqua.app.platform.initGraphicsEnvironment
import org.kodein.di.DI

fun main(args: Array<String>) = runOpenAquaApp(args = args)

fun runOpenAquaApp(
    vararg overrideModules: DI.Module,
    args: Array<String> = emptyArray(),
) {
    initGraphicsEnvironment()
    acquireOrNotify()
    nativeComposeApp {
        OpenAquaApp(
            di = rememberOpenAquaDI(overrideModules = overrideModules),
            startMinimized = "--minimized" in args,
        )
    }
}
