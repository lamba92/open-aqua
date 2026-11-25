@file:OptIn(ExperimentalForeignApi::class)

package io.github.lamba92.openaqua.app.platform

import kotlinx.cinterop.ExperimentalForeignApi
import sdl3.SDL_GetDisplayContentScale
import sdl3.SDL_GetPrimaryDisplay

internal expect fun initGraphicsEnvironment()

internal fun getPlatformDisplayScale(): Float {
    val display = SDL_GetPrimaryDisplay()
    val scale = if (display != 0u) SDL_GetDisplayContentScale(display) else 1f
    return if (scale > 0f) scale else 1f
}
