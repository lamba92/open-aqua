package io.github.lamba92.gradle.plugins.distribution

import io.github.lamba92.gradle.plugins.engine.Arch
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import java.io.InputStream
import java.net.URI
import javax.inject.Inject

abstract class PackageAppImageTask
    @Inject
    constructor(
        private val execOperations: ExecOperations,
    ) : DefaultTask() {
        init {
            group = "distribution"
        }

        @get:InputFiles
        abstract val appDistributable: ConfigurableFileCollection

        @get:InputDirectory
        abstract val packagingDir: DirectoryProperty

        @get:InputFile
        abstract val icon512: RegularFileProperty

        @get:InputFile
        @get:Optional
        abstract val icon192: RegularFileProperty

        @get:InputFile
        @get:Optional
        abstract val iconSvg: RegularFileProperty

        @get:Input
        abstract val targetArch: Property<Arch>

        @get:OutputFile
        abstract val outputAppImage: RegularFileProperty

        @get:Internal
        abstract val appimagetoolDir: DirectoryProperty

        @TaskAction
        fun packageAppImage() {
            val appDistributableDir =
                if (!appDistributable.isEmpty) {
                    appDistributable.singleFile
                } else {
                    error("No distributable dependency was configured for AppImage creation.")
                }

            val targetAppDir = temporaryDir.resolve("open-aqua.AppDir")
            targetAppDir.deleteRecursively()
            targetAppDir.mkdirs()

            // 1. Copy app into usr/
            val usrDir = File(targetAppDir, "usr")
            usrDir.mkdirs()
            appDistributableDir.copyRecursively(usrDir, overwrite = true)

            // CRITICAL: Preserve executable permissions for binaries and runtime helper scripts
            appDistributableDir.walk().forEach { src: File ->
                if (src.isFile && src.canExecute()) {
                    val rel = src.relativeTo(appDistributableDir)
                    File(usrDir, rel.path).setExecutable(true, false)
                }
            }
            File(usrDir, "bin/open-aqua").setExecutable(true, false)
            File(usrDir, "lib/runtime/lib/jspawnhelper").let { if (it.exists()) it.setExecutable(true, false) }
            File(usrDir, "lib/runtime/lib/jexec").let { if (it.exists()) it.setExecutable(true, false) }

            // 2. Copy AppRun launcher from packaging/
            val targetPackagingDir = packagingDir.get().asFile
            val appRun = File(targetPackagingDir, "AppRun")
            val targetAppRun = File(targetAppDir, "AppRun")
            appRun.copyTo(targetAppRun, overwrite = true)
            targetAppRun.setExecutable(true, false)

            // 3. Desktop files (root and usr/share/applications/)
            val desktopFile = File(targetPackagingDir, "open-aqua.desktop")
            desktopFile.copyTo(File(targetAppDir, "open-aqua.desktop"), overwrite = true)
            val shareAppsDir = File(usrDir, "share/applications").apply { mkdirs() }
            desktopFile.copyTo(File(shareAppsDir, "open-aqua.desktop"), overwrite = true)

            // 4. Icons (root, .DirIcon, and standard FreeDesktop icon theme directories)
            val icon512File = icon512.get().asFile
            icon512File.copyTo(File(targetAppDir, "open-aqua.png"), overwrite = true)
            icon512File.copyTo(File(targetAppDir, ".DirIcon"), overwrite = true)

            val iconsDir = File(usrDir, "share/icons/hicolor")
            File(iconsDir, "512x512/apps").apply { mkdirs() }
            icon512File.copyTo(File(iconsDir, "512x512/apps/open-aqua.png"), overwrite = true)

            val icon192File = icon192.orNull?.asFile
            if (icon192File != null && icon192File.exists()) {
                File(iconsDir, "192x192/apps").apply { mkdirs() }
                icon192File.copyTo(File(iconsDir, "192x192/apps/open-aqua.png"), overwrite = true)
            }

            val iconSvgFile = iconSvg.orNull?.asFile
            if (iconSvgFile != null && iconSvgFile.exists()) {
                File(iconsDir, "scalable/apps").apply { mkdirs() }
                iconSvgFile.copyTo(File(iconsDir, "scalable/apps/open-aqua.svg"), overwrite = true)
            }

            // 5. Download and extract appimagetool if not cached
            val toolDir = appimagetoolDir.get().asFile
            val extractedTool = File(toolDir, "squashfs-root/AppRun")
            val arch = targetArch.get()
            val toolArch = if (arch == Arch.ARM64) "aarch64" else "x86_64"
            val appImageEnvArch = if (arch == Arch.ARM64) "arm_aarch64" else "x86_64"
            if (!extractedTool.exists()) {
                toolDir.mkdirs()
                val toolAppImage = File(toolDir, "appimagetool-$toolArch.AppImage")
                URI("https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-$toolArch.AppImage")
                    .toURL()
                    .openStream()
                    .use { input: InputStream ->
                        toolAppImage.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                toolAppImage.setExecutable(true, false)

                execOperations.exec {
                    workingDir = toolDir
                    commandLine(toolAppImage.absolutePath, "--appimage-extract")
                }
            }

            // 6. Run appimagetool
            val finalAppImage = outputAppImage.get().asFile
            finalAppImage.parentFile.mkdirs()
            finalAppImage.delete()

            execOperations.exec {
                environment("ARCH", appImageEnvArch)
                commandLine(
                    extractedTool.absolutePath,
                    targetAppDir.absolutePath,
                    finalAppImage.absolutePath,
                )
            }

            finalAppImage.setExecutable(true, false)
            logger.lifecycle("AppImage generated successfully at: ${finalAppImage.absolutePath}")
        }
    }
