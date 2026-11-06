import io.github.lamba92.gradle.plugins.engine.Arch
import io.github.lamba92.gradle.plugins.engine.Os
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.InputStream
import java.net.URI

plugins {
    openaqua.convention.jvm
    id(libs.plugins.kotlin.compose)
    id(libs.plugins.compose.multiplatform)
    id(libs.plugins.compose.hot.reload)
    openaqua.engine.consumer
}

engineBinaries {
    if (Os.current() == Os.MACOS) {
        os = Os.LINUX
        arch = Arch.X64
    }
}

dependencies {
    implementation(projects.appCompose)
    engineBinaries(projects.engine.engineDevice.engineDeviceProd)
    when (SupportedTargets.getCurrent().os) {
        Os.LINUX -> engineBinaries(projects.engine.engineHost.engineHostLinux)
        Os.WINDOWS -> engineBinaries(projects.engine.engineHost.engineHostWindows)
        else -> Unit
    }
}

compose.desktop {
    application {
        mainClass = "io.github.lamba92.openaqua.app.MainKt"

        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Msi)
            packageName = "open-aqua"
            packageVersion = "1.0.0"

            linux {
                iconFile = rootProject.file("assets/generated/icon-512.png")
            }
            windows {
                iconFile = rootProject.file("assets/generated/favicon.ico")
                perUserInstall = false
            }
            macOS {
                iconFile = rootProject.file("assets/generated/icon.icns")
            }
        }
    }
}

tasks.register<Tar>("packageReleaseTarGz") {
    group = "distribution"
    description = "Packages the release standalone distributable into a tar.gz archive"
    dependsOn("createReleaseDistributable")

    archiveBaseName = "open-aqua"
    archiveVersion = "1.0.0"
    archiveClassifier = "linux-x64"
    archiveExtension = "tar.gz"
    compression = Compression.GZIP
    destinationDirectory = layout.buildDirectory.dir("compose/binaries/main-release/tar")

    into("open-aqua") {
        from(layout.buildDirectory.dir("compose/binaries/main-release/app/open-aqua"))
    }
}

tasks.register<Zip>("packageReleaseZip") {
    group = "distribution"
    description = "Packages the release standalone distributable into a zip archive"
    dependsOn("createReleaseDistributable")

    archiveBaseName = "open-aqua"
    archiveVersion = "1.0.0"
    archiveClassifier = "portable"
    destinationDirectory = layout.buildDirectory.dir("compose/binaries/main-release/zip")

    into("open-aqua") {
        from(layout.buildDirectory.dir("compose/binaries/main-release/app/open-aqua"))
    }
}

tasks.register("packageReleaseAppImage") {
    group = "distribution"
    description = "Packages the release standalone distributable into a self-contained .AppImage file"
    dependsOn("createReleaseDistributable")

    val packagingDir = project.file("packaging/appimage")
    val appDir = layout.buildDirectory.dir("compose/tmp/appimage/open-aqua.AppDir")
    val outputDir = layout.buildDirectory.dir("compose/binaries/main-release/appimage")
    val appimagetoolDir = layout.buildDirectory.dir("compose/tmp/appimage/tool")
    val icon512 = rootProject.file("assets/generated/icon-512.png")
    val icon192 = rootProject.file("assets/generated/icon-192.png")
    val iconSvg = rootProject.file("assets/generated/app-icon-squircle.svg")
    val appDistributable = layout.buildDirectory.dir("compose/binaries/main-release/app/open-aqua")

    inputs.dir(packagingDir)
    inputs.dir(appDistributable)
    inputs.file(icon512)
    outputs.file(outputDir.map { it.file("OpenAqua-1.0.0-x86_64.AppImage") })

    doLast {
        val targetAppDir = appDir.get().asFile
        targetAppDir.deleteRecursively()
        targetAppDir.mkdirs()

        // 1. Copy app into usr/
        val usrDir = File(targetAppDir, "usr")
        usrDir.mkdirs()
        appDistributable.get().asFile.copyRecursively(usrDir, overwrite = true)

        // CRITICAL: Preserve executable permissions for binaries and runtime helper scripts
        appDistributable.get().asFile.walk().forEach { src ->
            if (src.isFile && src.canExecute()) {
                val rel = src.relativeTo(appDistributable.get().asFile)
                File(usrDir, rel.path).setExecutable(true, false)
            }
        }
        File(usrDir, "bin/open-aqua").setExecutable(true, false)
        File(usrDir, "lib/runtime/lib/jspawnhelper").let { if (it.exists()) it.setExecutable(true, false) }
        File(usrDir, "lib/runtime/lib/jexec").let { if (it.exists()) it.setExecutable(true, false) }

        // 2. Copy AppRun launcher from packaging/
        val appRun = File(packagingDir, "AppRun")
        val targetAppRun = File(targetAppDir, "AppRun")
        appRun.copyTo(targetAppRun, overwrite = true)
        targetAppRun.setExecutable(true, false)

        // 3. Desktop files (root and usr/share/applications/)
        val desktopFile = File(packagingDir, "open-aqua.desktop")
        desktopFile.copyTo(File(targetAppDir, "open-aqua.desktop"), overwrite = true)
        val shareAppsDir = File(usrDir, "share/applications").apply { mkdirs() }
        desktopFile.copyTo(File(shareAppsDir, "open-aqua.desktop"), overwrite = true)

        // 4. Icons (root, .DirIcon, and standard FreeDesktop icon theme directories)
        icon512.copyTo(File(targetAppDir, "open-aqua.png"), overwrite = true)
        icon512.copyTo(File(targetAppDir, ".DirIcon"), overwrite = true)

        val iconsDir = File(usrDir, "share/icons/hicolor")
        File(iconsDir, "512x512/apps").apply { mkdirs() }
        icon512.copyTo(File(iconsDir, "512x512/apps/open-aqua.png"), overwrite = true)

        if (icon192.exists()) {
            File(iconsDir, "192x192/apps").apply { mkdirs() }
            icon192.copyTo(File(iconsDir, "192x192/apps/open-aqua.png"), overwrite = true)
        }

        if (iconSvg.exists()) {
            File(iconsDir, "scalable/apps").apply { mkdirs() }
            iconSvg.copyTo(File(iconsDir, "scalable/apps/open-aqua.svg"), overwrite = true)
        }

        // 5. Download and extract appimagetool if not cached
        val toolDir = appimagetoolDir.get().asFile
        val extractedTool = File(toolDir, "squashfs-root/AppRun")
        if (!extractedTool.exists()) {
            toolDir.mkdirs()
            val toolAppImage = File(toolDir, "appimagetool-x86_64.AppImage")
            URI("https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-x86_64.AppImage")
                .toURL()
                .openStream()
                .use { input: InputStream ->
                    toolAppImage.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            toolAppImage.setExecutable(true, false)

            providers.exec {
                workingDir = toolDir
                commandLine(toolAppImage.absolutePath, "--appimage-extract")
            }.result.get()
        }

        // 6. Run appimagetool
        val outDir = outputDir.get().asFile
        outDir.mkdirs()
        val finalAppImage = File(outDir, "OpenAqua-1.0.0-x86_64.AppImage")
        finalAppImage.delete()

        providers.exec {
            environment("ARCH", "x86_64")
            commandLine(
                extractedTool.absolutePath,
                targetAppDir.absolutePath,
                finalAppImage.absolutePath,
            )
        }.result.get()

        finalAppImage.setExecutable(true, false)
        logger.lifecycle("AppImage generated successfully at: ${finalAppImage.absolutePath}")
    }
}

tasks.register("installReleaseAppImage") {
    group = "distribution"
    description = "Installs the AppImage and desktop entry into ~/.local for the current user"
    dependsOn("packageReleaseAppImage")

    val appImageFile = layout.buildDirectory.file("compose/binaries/main-release/appimage/OpenAqua-1.0.0-x86_64.AppImage")
    val desktopFile = project.file("packaging/appimage/open-aqua.desktop")
    val icon512 = rootProject.file("assets/generated/icon-512.png")
    val iconSvg = rootProject.file("assets/generated/app-icon-squircle.svg")

    doLast {
        val userHome = System.getProperty("user.home")
        val binDir = File(userHome, ".local/bin").apply { mkdirs() }
        val appsDir = File(userHome, ".local/share/applications").apply { mkdirs() }
        val iconsDir = File(userHome, ".local/share/icons/hicolor")

        // 1. Copy AppImage to ~/.local/bin/OpenAqua.AppImage
        val installedAppImage = File(binDir, "OpenAqua.AppImage")
        appImageFile.get().asFile.copyTo(installedAppImage, overwrite = true)
        installedAppImage.setExecutable(true, false)

        // 2. Install icons to ~/.local/share/icons/hicolor/
        File(iconsDir, "512x512/apps").apply { mkdirs() }
        icon512.copyTo(File(iconsDir, "512x512/apps/open-aqua.png"), overwrite = true)
        if (iconSvg.exists()) {
            File(iconsDir, "scalable/apps").apply { mkdirs() }
            iconSvg.copyTo(File(iconsDir, "scalable/apps/open-aqua.svg"), overwrite = true)
        }

        // 3. Install .desktop file pointing to installed AppImage
        val installedDesktop = File(appsDir, "open-aqua.desktop")
        val desktopContent = desktopFile.readText()
            .replace("Exec=open-aqua", "Exec=${installedAppImage.absolutePath}")
        installedDesktop.writeText(desktopContent)

        // 4. Update desktop database if available
        runCatching {
            providers.exec {
                commandLine("update-desktop-database", appsDir.absolutePath)
            }.result.get()
        }

        logger.lifecycle("OpenAqua successfully installed to ~/.local for KDE/GNOME application menu.")
    }
}
