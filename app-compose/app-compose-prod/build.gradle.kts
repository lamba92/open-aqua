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

val releasePackageVersion =
    project.version
        .toString()
        .removePrefix("v")
        .substringBefore("-")

compose.desktop {
    application {
        mainClass = "io.github.lamba92.openaqua.app.MainKt"

        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Msi)
            packageName = "open-aqua"
            packageVersion = releasePackageVersion

            linux {
                iconFile = rootProject.file("assets/generated/icon-512.png")
                rpmPackageVersion = releasePackageVersion
            }
            windows {
                iconFile = rootProject.file("assets/generated/favicon.ico")
                msiPackageVersion = releasePackageVersion
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
    archiveVersion = project.version.toString()
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
    archiveVersion = project.version.toString()
    archiveClassifier = "portable"
    destinationDirectory = layout.buildDirectory.dir("compose/binaries/main-release/zip")

    into("open-aqua") {
        from(layout.buildDirectory.dir("compose/binaries/main-release/app/open-aqua"))
    }
}

val appImageVersion = project.version.toString().removePrefix("v")
val appImageFileName = "OpenAqua-$appImageVersion-x86_64.AppImage"

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
    outputs.file(outputDir.map { it.file(appImageFileName) })

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

            providers
                .exec {
                    workingDir = toolDir
                    commandLine(toolAppImage.absolutePath, "--appimage-extract")
                }.result
                .get()
        }

        // 6. Run appimagetool
        val outDir = outputDir.get().asFile
        outDir.mkdirs()
        val finalAppImage = File(outDir, appImageFileName)
        finalAppImage.delete()

        providers
            .exec {
                environment("ARCH", "x86_64")
                commandLine(
                    extractedTool.absolutePath,
                    targetAppDir.absolutePath,
                    finalAppImage.absolutePath,
                )
            }.result
            .get()

        finalAppImage.setExecutable(true, false)
        logger.lifecycle("AppImage generated successfully at: ${finalAppImage.absolutePath}")
    }
}

tasks.register("installReleaseAppImage") {
    group = "distribution"
    description = "Installs the AppImage and desktop entry into ~/.local for the current user"
    dependsOn("packageReleaseAppImage")

    val appImageFile = layout.buildDirectory.file("compose/binaries/main-release/appimage/$appImageFileName")
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
        val desktopContent =
            desktopFile
                .readText()
                .replace("Exec=open-aqua", "Exec=${installedAppImage.absolutePath}")
        installedDesktop.writeText(desktopContent)

        // 4. Update desktop database if available
        runCatching {
            providers
                .exec {
                    commandLine("update-desktop-database", appsDir.absolutePath)
                }.result
                .get()
        }

        logger.lifecycle("OpenAqua successfully installed to ~/.local for KDE/GNOME application menu.")
    }
}

tasks.register("releaseLinux") {
    group = "distribution"
    description = "Publishes Linux desktop application packages to root build/releases"
    dependsOn(
        "packageReleaseAppImage",
        "packageReleaseDeb",
        "packageReleaseRpm",
        "packageReleaseTarGz",
    )
    val composeBinDir = layout.buildDirectory.dir("compose/binaries/main-release")
    inputs.dir(composeBinDir)
    outputs.dir(rootProject.layout.buildDirectory.dir("releases"))

    doLast {
        val allowedExtensions = listOf("AppImage", "deb", "rpm", "tar.gz")
        val files =
            composeBinDir
                .get()
                .asFile
                .walk()
                .filter { file -> file.isFile && allowedExtensions.any { file.name.endsWith(".$it") } }
                .toList()
        publishReleaseArtifacts(files)
    }
}

tasks.register("releaseWindows") {
    group = "distribution"
    description = "Publishes Windows desktop application packages to root build/releases"
    dependsOn(
        "packageReleaseMsi",
        "packageReleaseZip",
    )
    val composeBinDir = layout.buildDirectory.dir("compose/binaries/main-release")
    inputs.dir(composeBinDir)
    outputs.dir(rootProject.layout.buildDirectory.dir("releases"))

    doLast {
        val allowedExtensions = listOf("msi", "zip")
        val files =
            composeBinDir
                .get()
                .asFile
                .walk()
                .filter { file -> file.isFile && allowedExtensions.any { file.name.endsWith(".$it") } }
                .toList()
        publishReleaseArtifacts(files)
    }
}
