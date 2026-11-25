package io.github.lamba92.openaqua.app.util

import kotlinx.io.files.Path

expect fun Path.openInDesktopEnvironment()

expect fun openUrlInBrowser(url: String)
