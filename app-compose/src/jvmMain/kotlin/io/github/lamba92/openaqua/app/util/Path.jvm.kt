package io.github.lamba92.openaqua.app.util

import kotlinx.io.files.Path
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.nio.file.Path as JavaPath

actual fun Path.openInDesktopEnvironment() {
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
        Desktop.getDesktop().open(File(toString()))
    } else {
        val os = System.getProperty("os.name").lowercase()
        val cmd =
            when {
                os.contains("mac") -> listOf("open", toString())
                os.contains("win") -> listOf("explorer", toString())
                else -> listOf("xdg-open", toString())
            }
        ProcessBuilder(cmd).start()
    }
}

actual fun openUrlInBrowser(url: String) {
    val uri = URI(url)
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(uri)
    } else {
        val os = System.getProperty("os.name").lowercase()
        val cmd =
            when {
                os.contains("mac") -> listOf("open", url)
                os.contains("win") -> listOf("rundll32", "url.dll,FileProtocolHandler", url)
                else -> listOf("xdg-open", url)
            }
        ProcessBuilder(cmd).start()
    }
}

fun Path.toJavaPath(): JavaPath = JavaPath.of(toString())

fun JavaPath.toKotlinxPath(): Path = Path(toString())
