import io.github.lamba92.gradle.plugins.packaging.GeneratePackagingFilesTask
import io.github.lamba92.gradle.plugins.packaging.InstallAppImageTask
import io.github.lamba92.gradle.plugins.packaging.PackageAppImageTask
import io.github.lamba92.gradle.plugins.packaging.PackageMsixTask
import io.github.lamba92.gradle.plugins.packaging.PublishReleaseArtifactsTask
import io.github.lamba92.gradle.plugins.platform.Arch
import io.github.lamba92.gradle.plugins.platform.Os
import io.github.lamba92.gradle.plugins.platform.toOsAndArch
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.KonanTarget

val releasePackageVersion =
    provider {
        project.version
            .toString()
            .removePrefix("v")
            .substringBefore("-")
    }

val rootMetadataFile = rootProject.file("packaging/metadata.json")
val rootIcon512 = rootProject.file("assets/generated/icon-512.png")
val rootIcon192 = rootProject.file("assets/generated/icon-192.png")
val rootIconSvg = rootProject.file("assets/generated/app-icon-squircle.svg")

val release =
    tasks.register("release") {
        group = "distribution"
        description = "Publishes all built desktop release packages to root build/releases"
    }

val generatePackagingFiles =
    tasks.register("generatePackagingFiles") {
        group = "distribution"
        description = "Generates packaging descriptor files for the current host platform"
    }

val packageReleaseAppImage =
    tasks.register("packageReleaseAppImage") {
        group = "distribution"
        description = "Packages the release AppImage for the current Linux architecture"
    }

val installReleaseAppImage =
    tasks.register("installReleaseAppImage") {
        group = "distribution"
        description = "Installs the release AppImage and desktop entry into ~/.local for the current user"
    }

val packageReleaseMsix =
    tasks.register("packageReleaseMsix") {
        group = "distribution"
        description = "Packages the Windows release distribution into a .msix package"
    }

// Common generated packaging dir for CI scripts compatibility
val commonGeneratedPackagingDir = layout.buildDirectory.dir("generated/packaging")

plugins.withId("org.jetbrains.kotlin.multiplatform") {
    extensions.configure<KotlinMultiplatformExtension> {
        targets.withType<KotlinNativeTarget>().all {
            val (os, arch) =
                when (konanTarget) {
                    KonanTarget.LINUX_X64,
                    KonanTarget.LINUX_ARM64,
                    KonanTarget.MINGW_X64,
                    -> konanTarget.toOsAndArch()

                    else -> return@all
                }

            val targetName = name
            val capitalizedTarget = targetName.replaceFirstChar { it.uppercase() }
            val archSuffix = arch.name

            binaries
                .withType<Executable>()
                .matching { it.name == "releaseExecutable" }
                .all {
                    val releaseExecutable = this

                    when (os) {
                        Os.LINUX -> {
                            val distributableDir = layout.buildDirectory.dir("distributions/$targetName/open-aqua")

                            val prepareDistributable =
                                tasks.register<Sync>("prepareDistributable$capitalizedTarget") {
                                    group = "distribution"
                                    description = "Prepares unpacked release application directory for $targetName"
                                    into(distributableDir)
                                    into("bin") {
                                        from(releaseExecutable.linkTaskProvider.flatMap { it.outputFile }) {
                                            rename { "open-aqua" }
                                            filePermissions { unix("rwxr-xr-x") }
                                        }
                                        from(layout.buildDirectory.dir("bin/$targetName/releaseExecutable")) {
                                            include("data.kres")
                                        }
                                    }
                                    dependsOn(releaseExecutable.linkTaskProvider)
                                    dependsOn("packageReleaseComposeResources$capitalizedTarget")
                                }

                            val packageTarGz =
                                tasks.register<Tar>("packageReleaseTarGz$capitalizedTarget") {
                                    group = "distribution"
                                    description = "Packages the release distributable into a tar.gz archive for $targetName"
                                    archiveBaseName = "open-aqua"
                                    archiveVersion = releasePackageVersion
                                    archiveClassifier = "linux-$archSuffix"
                                    archiveExtension = "tar.gz"
                                    compression = Compression.GZIP
                                    destinationDirectory = layout.buildDirectory.dir("distributions/archives")
                                    into("open-aqua") {
                                        from(prepareDistributable)
                                        filesMatching("bin/open-aqua") {
                                            permissions {
                                                unix("rwxr-xr-x")
                                            }
                                        }
                                    }
                                }

                            val genPackaging =
                                tasks.register<GeneratePackagingFilesTask>("generatePackagingFiles$capitalizedTarget") {
                                    metadataFile = rootMetadataFile
                                    version = releasePackageVersion
                                    targetOs = Os.LINUX
                                    rootProjectDir = rootProject.layout.projectDirectory
                                    tarballArchive = packageTarGz.flatMap { it.archiveFile }
                                    outputDir = commonGeneratedPackagingDir
                                    dependsOn(packageTarGz)
                                }

                            val packageAppImageTask =
                                tasks.register<PackageAppImageTask>("packageReleaseAppImage$capitalizedTarget") {
                                    dependsOn(prepareDistributable)
                                    dependsOn(genPackaging)
                                    targetArch = arch
                                    appDistributable = distributableDir
                                    packagingDir = genPackaging.flatMap { it.outputDir }.map { it.dir("appimage") }
                                    icon512 = rootIcon512
                                    icon192 = rootIcon192
                                    iconSvg = rootIconSvg
                                    outputAppImage =
                                        layout.buildDirectory.file(
                                            releasePackageVersion.map { ver ->
                                                "binaries/appimage/$archSuffix/open-aqua-$ver-linux-$archSuffix.AppImage"
                                            },
                                        )
                                    appimagetoolDir = layout.buildDirectory.dir("tmp/appimage/tool/$archSuffix")
                                }

                            val installAppImageTask =
                                tasks.register<InstallAppImageTask>("installReleaseAppImage$capitalizedTarget") {
                                    dependsOn(packageAppImageTask)
                                    appImageFile = packageAppImageTask.flatMap { it.outputAppImage }
                                    desktopFile =
                                        genPackaging.flatMap { it.outputDir }.map { it.file("appimage/open-aqua.desktop") }
                                    icon512 = rootIcon512
                                    iconSvg = rootIconSvg
                                }

                            val releaseTarget =
                                tasks.register<PublishReleaseArtifactsTask>("release$capitalizedTarget") {
                                    group = "distribution"
                                    description = "Publishes desktop packages for $targetName to root build/releases"
                                    releaseFiles.from(packageTarGz.flatMap { it.archiveFile })
                                    if (OperatingSystem.current().isLinux && Arch.current() == arch) {
                                        releaseFiles.from(packageAppImageTask.flatMap { it.outputAppImage })
                                    }
                                    destinationDir = rootProject.layout.buildDirectory.dir("releases")
                                }

                            if (OperatingSystem.current().isLinux && Arch.current() == arch) {
                                generatePackagingFiles.configure { dependsOn(genPackaging) }
                                packageReleaseAppImage.configure { dependsOn(packageAppImageTask) }
                                installReleaseAppImage.configure { dependsOn(installAppImageTask) }
                                release.configure { dependsOn(releaseTarget) }
                            }
                        }

                        Os.WINDOWS -> {
                            val prepareDistributableMingw =
                                tasks.register<Sync>("prepareDistributable$capitalizedTarget") {
                                    group = "distribution"
                                    description = "Prepares unpacked release application directory for $targetName"
                                    into(layout.buildDirectory.dir("distributions/$targetName/open-aqua"))
                                    from(releaseExecutable.linkTaskProvider.flatMap { it.outputFile }) {
                                        rename { "open-aqua.exe" }
                                    }
                                    from(layout.buildDirectory.dir("bin/$targetName/releaseExecutable")) {
                                        include("data.kres", "*.dll")
                                    }
                                    dependsOn(releaseExecutable.linkTaskProvider)
                                    dependsOn("packageReleaseComposeResources$capitalizedTarget")
                                }

                            val packageZip =
                                tasks.register<Zip>("packageReleaseZip$capitalizedTarget") {
                                    group = "distribution"
                                    description = "Packages the release distributable into a zip archive for $targetName"
                                    archiveBaseName = "open-aqua"
                                    archiveVersion = releasePackageVersion
                                    archiveClassifier = "windows-$archSuffix"
                                    destinationDirectory = layout.buildDirectory.dir("distributions/archives")
                                    into("open-aqua") {
                                        from(prepareDistributableMingw)
                                    }
                                }

                            val genPackagingMingw =
                                tasks.register<GeneratePackagingFilesTask>("generatePackagingFiles$capitalizedTarget") {
                                    metadataFile = rootMetadataFile
                                    version = releasePackageVersion
                                    targetOs = Os.WINDOWS
                                    rootProjectDir = rootProject.layout.projectDirectory
                                    outputDir = commonGeneratedPackagingDir
                                }

                            val packageMsixTask =
                                tasks.register<PackageMsixTask>("packageReleaseMsix$capitalizedTarget") {
                                    dependsOn(packageZip)
                                    dependsOn(genPackagingMingw)
                                    zipArchive = packageZip.flatMap { it.archiveFile }
                                    packagingDir = genPackagingMingw.flatMap { it.outputDir }.map { it.dir("msix") }
                                    outputMsix =
                                        layout.buildDirectory.file(
                                            releasePackageVersion.map { ver ->
                                                "binaries/msix/open-aqua-$ver-windows-$archSuffix.msix"
                                            },
                                        )
                                }

                            val releaseMingw =
                                tasks.register<PublishReleaseArtifactsTask>("release$capitalizedTarget") {
                                    group = "distribution"
                                    description = "Publishes desktop packages for $targetName to root build/releases"
                                    releaseFiles.from(packageZip.flatMap { it.archiveFile })
                                    if (OperatingSystem.current().isWindows) {
                                        releaseFiles.from(packageMsixTask.flatMap { it.outputMsix })
                                    }
                                    destinationDir = rootProject.layout.buildDirectory.dir("releases")
                                }

                            if (OperatingSystem.current().isWindows) {
                                generatePackagingFiles.configure { dependsOn(genPackagingMingw) }
                                packageReleaseMsix.configure { dependsOn(packageMsixTask) }
                                release.configure { dependsOn(releaseMingw) }
                            }
                        }

                        Os.MACOS -> {}
                    }
                }
        }
    }
}
