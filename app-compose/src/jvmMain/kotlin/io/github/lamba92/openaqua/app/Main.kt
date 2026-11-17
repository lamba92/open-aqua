package io.github.lamba92.openaqua.app

import androidx.compose.ui.window.application
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.github.lamba92.openaqua.app.di.createOpenAquaDI
import io.github.lamba92.openaqua.app.instance.acquireOrNotify
import kotlinx.coroutines.runBlocking
import open_aqua.app_compose.generated.resources.Res
import org.kodein.di.DI
import java.awt.Taskbar
import javax.imageio.ImageIO

class OpenAquaAppCommand : CliktCommand(name = "open-aqua") {
    val minimized by option("-m", "--minimized", help = "Start OpenAqua minimized to the system tray").flag(default = false)

    override fun run() {
        acquireOrNotify()
        startOpenAquaApplication(createOpenAquaDI(), startMinimized = minimized)
    }
}

fun main(args: Array<String>) {
    System.setProperty("apple.awt.application.name", "OpenAqua")
    OpenAquaAppCommand().main(args)
}

fun startOpenAquaApplication(
    di: DI = createOpenAquaDI(),
    startMinimized: Boolean = false,
) {
    configureTaskbarIcon()
    application {
        OpenAquaApp(di = di, startMinimized = startMinimized)
    }
}

private fun configureTaskbarIcon() {
    runCatching {
        if (Taskbar.isTaskbarSupported()) {
            val taskbar = Taskbar.getTaskbar()
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                val iconBytes = runBlocking { Res.readBytes("drawable/ic_app_icon.png") }
                val awtImage = ImageIO.read(iconBytes.inputStream())
                if (awtImage != null) {
                    taskbar.iconImage = awtImage
                }
            }
        }
    }
}
