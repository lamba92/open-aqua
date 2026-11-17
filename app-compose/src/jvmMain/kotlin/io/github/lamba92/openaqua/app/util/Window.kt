package io.github.lamba92.openaqua.app.util

import androidx.compose.ui.window.WindowState
import java.awt.Desktop
import java.awt.Frame
import java.awt.Window

fun Window.restoreAndFocus(windowState: WindowState) {
    windowState.isMinimized = false
    if (this is Frame && (extendedState and Frame.ICONIFIED) != 0) {
        extendedState = extendedState and Frame.ICONIFIED.inv()
    }
    isVisible = true
    toFront()
    requestFocus()
    try {
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.APP_REQUEST_FOREGROUND)) {
                desktop.requestForeground(true)
            }
        }
    } catch (_: Throwable) {
        // Ignored on systems without Desktop support
    }
    try {
        // Toggling alwaysOnTop helps break through OS focus-stealing prevention on Linux/Windows
        isAlwaysOnTop = true
        isAlwaysOnTop = false
    } catch (_: Throwable) {
        // Ignored if window manager denies alwaysOnTop
    }
}
