package io.github.lamba92.gradle.plugins.distribution

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

abstract class InstallAppImageTask
    @Inject
    constructor(
        private val execOperations: ExecOperations,
    ) : DefaultTask() {
        init {
            group = "distribution"
            description = "Installs the AppImage and desktop entry into ~/.local for the current user"
        }

        @get:InputFile
        abstract val appImageFile: RegularFileProperty

        @get:InputFile
        abstract val desktopFile: RegularFileProperty

        @get:InputFile
        abstract val icon512: RegularFileProperty

        @get:InputFile
        @get:Optional
        abstract val iconSvg: RegularFileProperty

        @TaskAction
        fun install() {
            val appImage = appImageFile.get().asFile
            val userHome = System.getProperty("user.home")
            val binDir = File(userHome, ".local/bin").apply { mkdirs() }
            val appsDir = File(userHome, ".local/share/applications").apply { mkdirs() }
            val iconsDir = File(userHome, ".local/share/icons/hicolor")

            val installedAppImage = File(binDir, "OpenAqua.AppImage")
            appImage.copyTo(installedAppImage, overwrite = true)
            installedAppImage.setExecutable(true, false)

            File(iconsDir, "512x512/apps").apply { mkdirs() }
            icon512.get().asFile.copyTo(File(iconsDir, "512x512/apps/open-aqua.png"), overwrite = true)

            val svg = iconSvg.orNull?.asFile
            if (svg != null && svg.exists()) {
                File(iconsDir, "scalable/apps").apply { mkdirs() }
                svg.copyTo(File(iconsDir, "scalable/apps/open-aqua.svg"), overwrite = true)
            }

            val installedDesktop = File(appsDir, "open-aqua.desktop")
            val desktopContent =
                desktopFile
                    .get()
                    .asFile
                    .readText()
                    .replace("Exec=open-aqua", "Exec=${installedAppImage.absolutePath}")
            installedDesktop.writeText(desktopContent)

            runCatching {
                execOperations.exec {
                    commandLine("update-desktop-database", appsDir.absolutePath)
                }
            }

            logger.lifecycle("OpenAqua successfully installed to ~/.local for KDE/GNOME application menu.")
        }
    }
