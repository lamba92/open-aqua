import io.github.lamba92.gradle.plugins.distribution.DISTRIBUTION_TYPE_ATTRIBUTE
import io.github.lamba92.gradle.plugins.distribution.DistributionType
import io.github.lamba92.gradle.plugins.engine.OS_ATTRIBUTE
import io.github.lamba92.gradle.plugins.engine.Os
import org.gradle.api.attributes.Usage
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.named
import java.io.File
import java.io.InputStream
import java.net.URI

val appDistributable =
    configurations.create("appDistributable") {
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>("app-distribution"))
            attribute(OS_ATTRIBUTE, Os.LINUX)
            attribute(DISTRIBUTION_TYPE_ATTRIBUTE, DistributionType.APP_DIR)
        }
    }

val linuxDistributionPackages =
    configurations.create("linuxDistributionPackages") {
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>("app-distribution"))
            attribute(OS_ATTRIBUTE, Os.LINUX)
            attribute(DISTRIBUTION_TYPE_ATTRIBUTE, DistributionType.PACKAGES)
        }
    }

val windowsDistributionPackages =
    configurations.create("windowsDistributionPackages") {
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>("app-distribution"))
            attribute(OS_ATTRIBUTE, Os.WINDOWS)
            attribute(DISTRIBUTION_TYPE_ATTRIBUTE, DistributionType.PACKAGES)
        }
    }

val releasePackageVersion =
    project.version
        .toString()
        .removePrefix("v")
        .substringBefore("-")

val appImageFileName = "open-aqua-$releasePackageVersion-linux-x64.AppImage"
val msixFileName = "open-aqua-$releasePackageVersion-windows-x64.msix"

fun findMakeAppxExecutable(): File? {
    val pathEnv = System.getenv("PATH") ?: ""
    for (dir in pathEnv.split(File.pathSeparator)) {
        val candidate = File(dir, "MakeAppx.exe")
        if (candidate.isFile) return candidate
    }

    val programFilesX86 = System.getenv("ProgramFiles(x86)") ?: "C:\\Program Files (x86)"
    val windowsKitsDir = File(programFilesX86, "Windows Kits\\10\\bin")
    if (windowsKitsDir.isDirectory) {
        val found =
            windowsKitsDir
                .walk()
                .filter { it.isFile && it.name.equals("MakeAppx.exe", ignoreCase = true) }
                .firstOrNull { it.parentFile.name.equals("x64", ignoreCase = true) }
                ?: windowsKitsDir
                    .walk()
                    .firstOrNull { it.isFile && it.name.equals("MakeAppx.exe", ignoreCase = true) }
        if (found != null) return found
    }

    return null
}

val packageReleaseAppImage =
    tasks.register("packageReleaseAppImage") {
        group = "distribution"
        description = "Packages the release standalone distributable into a self-contained .AppImage file"

        val packagingDir = layout.buildDirectory.dir("generated/packaging/appimage")
        val appDir = layout.buildDirectory.dir("tmp/appimage/open-aqua.AppDir")
        val outputDir = layout.buildDirectory.dir("binaries/appimage")
        val appimagetoolDir = layout.buildDirectory.dir("tmp/appimage/tool")
        val icon512 = rootProject.file("assets/generated/icon-512.png")
        val icon192 = rootProject.file("assets/generated/icon-192.png")
        val iconSvg = rootProject.file("assets/generated/app-icon-squircle.svg")

        inputs.dir(packagingDir)
        inputs.files(appDistributable)
        inputs.file(icon512)
        outputs.file(outputDir.map { it.file(appImageFileName) })

        doLast {
            val appDistributableDir =
                if (!appDistributable.isEmpty) {
                    appDistributable.singleFile
                } else {
                    error("No appDistributable dependency was configured for AppImage creation.")
                }
            val targetAppDir = appDir.get().asFile
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

val installReleaseAppImage =
    tasks.register("installReleaseAppImage") {
        group = "distribution"
        description = "Installs the AppImage and desktop entry into ~/.local for the current user"
        dependsOn(packageReleaseAppImage)

        val desktopFile = layout.buildDirectory.file("generated/packaging/appimage/open-aqua.desktop")
        val icon512 = rootProject.file("assets/generated/icon-512.png")
        val iconSvg = rootProject.file("assets/generated/app-icon-squircle.svg")

        doLast {
            val appImageFile =
                packageReleaseAppImage
                    .get()
                    .outputs.files.singleFile
            val userHome = System.getProperty("user.home")
            val binDir = File(userHome, ".local/bin").apply { mkdirs() }
            val appsDir = File(userHome, ".local/share/applications").apply { mkdirs() }
            val iconsDir = File(userHome, ".local/share/icons/hicolor")

            val installedAppImage = File(binDir, "OpenAqua.AppImage")
            appImageFile.copyTo(installedAppImage, overwrite = true)
            installedAppImage.setExecutable(true, false)

            File(iconsDir, "512x512/apps").apply { mkdirs() }
            icon512.copyTo(File(iconsDir, "512x512/apps/open-aqua.png"), overwrite = true)
            if (iconSvg.exists()) {
                File(iconsDir, "scalable/apps").apply { mkdirs() }
                iconSvg.copyTo(File(iconsDir, "scalable/apps/open-aqua.svg"), overwrite = true)
            }

            val installedDesktop = File(appsDir, "open-aqua.desktop")
            val desktopContent =
                desktopFile.get().asFile
                    .readText()
                    .replace("Exec=open-aqua", "Exec=${installedAppImage.absolutePath}")
            installedDesktop.writeText(desktopContent)

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

val packageReleaseMsix =
    tasks.register("packageReleaseMsix") {
        group = "distribution"
        description = "Packages the Windows release distribution into a .msix package using MakeAppx"

        val packagingDir = layout.buildDirectory.dir("generated/packaging/msix")
        val stagingDir = layout.buildDirectory.dir("tmp/msix/layout")
        val outputDir = layout.buildDirectory.dir("binaries/msix")
        val msixFile = outputDir.map { it.file(msixFileName) }

        inputs.dir(packagingDir)
        inputs.files(windowsDistributionPackages)
        outputs.file(msixFile)

        doLast {
            val packagesDir =
                if (!windowsDistributionPackages.isEmpty) {
                    windowsDistributionPackages.singleFile
                } else {
                    error("No windowsDistributionPackages dependency was configured for MSIX creation.")
                }

            val zipFile =
                packagesDir.walk().firstOrNull { it.isFile && it.name.endsWith(".zip") }
                    ?: error("Could not find Windows distribution zip archive in $packagesDir")

            val targetStagingDir = stagingDir.get().asFile
            targetStagingDir.deleteRecursively()
            targetStagingDir.mkdirs()

            // 1. Unpack the zip distributable
            copy {
                from(zipTree(zipFile))
                into(targetStagingDir)
                eachFile {
                    path = path.removePrefix("open-aqua/")
                }
                includeEmptyDirs = false
            }

            // 2. Copy AppxManifest.xml & Assets from generated packaging
            val sourcePackagingDir = packagingDir.get().asFile
            val manifestFile = File(sourcePackagingDir, "AppxManifest.xml")
            if (!manifestFile.exists()) {
                error("MSIX manifest not found at: ${manifestFile.absolutePath}")
            }
            manifestFile.copyTo(File(targetStagingDir, "AppxManifest.xml"), overwrite = true)

            val assetsDir = File(sourcePackagingDir, "Assets")
            if (assetsDir.exists()) {
                assetsDir.copyRecursively(File(targetStagingDir, "Assets"), overwrite = true)
            }

            // 3. Locate MakeAppx.exe
            val makeAppxExe =
                findMakeAppxExecutable()
                    ?: error("MakeAppx.exe could not be found in Windows Kits or PATH. Ensure Windows SDK is installed.")

            logger.lifecycle("Found MakeAppx at: ${makeAppxExe.absolutePath}")

            // 4. Pack MSIX with /nv (no signature verification needed; Microsoft Store signs during ingestion)
            val finalMsix = msixFile.get().asFile
            finalMsix.parentFile.mkdirs()
            finalMsix.delete()

            providers.exec {
                commandLine(
                    makeAppxExe.absolutePath,
                    "pack",
                    "/d",
                    targetStagingDir.absolutePath,
                    "/p",
                    finalMsix.absolutePath,
                    "/nv",
                )
            }.result.get()

            logger.lifecycle("MSIX package generated successfully at: ${finalMsix.absolutePath}")
        }
    }

val releaseLinux =
    tasks.register("releaseLinux") {
        group = "distribution"
        description = "Publishes Linux desktop application packages to root build/releases"
        dependsOn(packageReleaseAppImage)
        inputs.files(linuxDistributionPackages)
        outputs.dir(rootProject.layout.buildDirectory.dir("releases"))

        doLast {
            val appImage =
                packageReleaseAppImage
                    .get()
                    .outputs.files.singleFile
            val otherPackages =
                if (!linuxDistributionPackages.isEmpty) {
                    val packagesDir = linuxDistributionPackages.singleFile
                    packagesDir.walk().filter { it.isFile }.toList()
                } else {
                    emptyList()
                }
            publishReleaseArtifacts(listOf(appImage) + otherPackages)
        }
    }

val releaseWindows =
    tasks.register("releaseWindows") {
        group = "distribution"
        description = "Publishes Windows desktop application packages to root build/releases"
        inputs.files(windowsDistributionPackages)
        outputs.dir(rootProject.layout.buildDirectory.dir("releases"))

        val os = OperatingSystem.current()
        if (os.isWindows) {
            dependsOn(packageReleaseMsix)
        }

        doLast {
            val packages =
                if (!windowsDistributionPackages.isEmpty) {
                    val packagesDir = windowsDistributionPackages.singleFile
                    packagesDir.walk().filter { it.isFile }.toList()
                } else {
                    emptyList()
                }

            val msixPackages =
                if (os.isWindows) {
                    listOf(packageReleaseMsix.get().outputs.files.singleFile)
                } else {
                    emptyList()
                }

            publishReleaseArtifacts(packages + msixPackages)
        }
    }

tasks.register("release") {
    group = "distribution"
    description = "Publishes desktop application packages to root build/releases for the current OS"
    val os = OperatingSystem.current()
    when {
        os.isLinux -> dependsOn(releaseLinux)
        os.isWindows -> dependsOn(releaseWindows)
        else -> Unit
    }
}

afterEvaluate {
    val generatePackagingFiles = tasks.findByName("generatePackagingFiles")
    if (generatePackagingFiles != null) {
        packageReleaseAppImage.configure {
            dependsOn(generatePackagingFiles)
        }
        installReleaseAppImage.configure {
            dependsOn(generatePackagingFiles)
        }
        packageReleaseMsix.configure {
            dependsOn(generatePackagingFiles)
        }
    }
}
