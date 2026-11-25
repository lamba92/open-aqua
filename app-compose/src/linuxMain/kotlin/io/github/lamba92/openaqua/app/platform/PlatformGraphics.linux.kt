@file:OptIn(ExperimentalForeignApi::class)

package io.github.lamba92.openaqua.app.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.getenv
import platform.posix.setenv

internal actual fun initGraphicsEnvironment() {
    // Skiko's precompiled Linux Skia library is linked against GrGLMakeNativeInterface_glx.
    // Under native Wayland (EGL), DirectContext.makeGL() cannot query GLX and fails,
    // causing Compose to fall back to CPU raster (Software). If DISPLAY is available
    // and SDL_VIDEODRIVER has not been explicitly overridden, route SDL to X11 (XWayland)
    // so Skia can establish a hardware-accelerated GLX context.
    if (getenv("SDL_VIDEODRIVER") == null && getenv("DISPLAY") != null) {
        setenv("SDL_VIDEODRIVER", "x11", 0)
    }
}
