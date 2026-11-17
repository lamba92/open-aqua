package io.github.lamba92.openaqua.app.util

import kotlinx.io.files.Path
import platform.posix.system
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
actual fun Path.openInDesktopEnvironment() {
    val cmd =
        when (kotlin.native.Platform.osFamily) {
            kotlin.native.OsFamily.WINDOWS -> "explorer \"$this\""
            kotlin.native.OsFamily.MACOSX -> "open \"$this\""
            else -> "xdg-open \"$this\""
        }
    system(cmd)
}

@OptIn(ExperimentalNativeApi::class)
actual fun openUrlInBrowser(url: String) {
    val cmd =
        when (kotlin.native.Platform.osFamily) {
            kotlin.native.OsFamily.WINDOWS -> "rundll32 url.dll,FileProtocolHandler \"$url\""
            kotlin.native.OsFamily.MACOSX -> "open \"$url\""
            else -> "xdg-open \"$url\""
        }
    system(cmd)
}
