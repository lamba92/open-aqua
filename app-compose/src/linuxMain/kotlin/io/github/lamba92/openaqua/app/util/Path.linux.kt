package io.github.lamba92.openaqua.app.util

import kotlinx.io.files.Path
import platform.posix.system

actual fun Path.openInDesktopEnvironment() {
    system("xdg-open \"$this\"")
}

actual fun openUrlInBrowser(url: String) {
    system("xdg-open \"$url\"")
}
