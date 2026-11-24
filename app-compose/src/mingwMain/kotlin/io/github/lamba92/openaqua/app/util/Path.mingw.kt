package io.github.lamba92.openaqua.app.util

import kotlinx.io.files.Path
import platform.posix.system

actual fun Path.openInDesktopEnvironment() {
    system("explorer \"$this\"")
}

actual fun openUrlInBrowser(url: String) {
    system("rundll32 url.dll,FileProtocolHandler \"$url\"")
}
