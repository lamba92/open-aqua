package io.github.lamba92.openaqua.app.nativeapp

import androidx.compose.runtime.remember
import com.compose.sdl.nativeComposeApp
import io.github.lamba92.openaqua.app.OpenAquaApp
import io.github.lamba92.openaqua.app.di.createOpenAquaNativeDI

fun main() =
    nativeComposeApp {
        val di = remember { createOpenAquaNativeDI() }
        OpenAquaApp(di = di)
    }
