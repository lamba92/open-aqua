import io.github.lamba92.gradle.plugins.distribution.DISTRIBUTION_TYPE_ATTRIBUTE
import io.github.lamba92.gradle.plugins.distribution.DistributionType
import io.github.lamba92.gradle.plugins.distribution.InstallAppImageTask
import io.github.lamba92.gradle.plugins.distribution.PackageAppImageTask
import io.github.lamba92.gradle.plugins.distribution.PackageMsixTask
import io.github.lamba92.gradle.plugins.distribution.PublishReleaseArtifactsTask
import io.github.lamba92.gradle.plugins.engine.Arch
import io.github.lamba92.gradle.plugins.engine.Os
import io.github.lamba92.gradle.plugins.platform.PlatformExtension
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.internal.os.OperatingSystem
import java.io.File

plugins {
    id("openaqua.platform")
}

val platform = extensions.getByType<PlatformExtension>()

// 1. Declaration configuration
val distribution =
    configurations.create("distribution") {
        isCanBeConsumed = false
        isCanBeResolved = false
    }

// 2. Resolving configurations
val appDir =
    configurations.create("appDir") {
        isCanBeConsumed = false
        isCanBeResolved = true
        extendsFrom(distribution)
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>("app-distribution"))
            attribute(DISTRIBUTION_TYPE_ATTRIBUTE, DistributionType.APP_DIR)
        }
    }

val packages =
    configurations.create("packages") {
        isCanBeConsumed = false
        isCanBeResolved = true
        extendsFrom(distribution)
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>("app-distribution"))
            attribute(DISTRIBUTION_TYPE_ATTRIBUTE, DistributionType.PACKAGES)
        }
    }

// 3. Consumable configuration
val packagesElements =
    configurations.create("packagesElements") {
        isCanBeConsumed = true
        isCanBeResolved = false
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named<Usage>("app-packages"))
        }
    }

val releasePackageVersion =
    project.version
        .toString()
        .removePrefix("v")
        .substringBefore("-")

val generatedPackagingDir = layout.buildDirectory.dir("generated/packaging")

val generatePackagingFiles by tasks.registering(JavaExec::class) {
    group = "distribution"
    description = "Generates packaging descriptor files for the target platform"
    outputs.dir(generatedPackagingDir)
    val metadataFile = rootProject.file("packaging/metadata.json")
    inputs.file(metadataFile)
    environment("OPENAQUA_METADATA_FILE", metadataFile.absolutePath)
    environment("OPENAQUA_OUTPUT_DIR", generatedPackagingDir.get().asFile.absolutePath)
    environment("OPENAQUA_VERSION", releasePackageVersion)
    environment("OPENAQUA_ROOT_DIR", rootProject.projectDir.absolutePath)
}

plugins.withId("application") {
    val app = extensions.getByType<JavaApplication>()
    val javaExt = extensions.getByType<JavaPluginExtension>()
    generatePackagingFiles.configure {
        classpath = javaExt.sourceSets.getByName("main").runtimeClasspath
        mainClass = app.mainClass
    }
}

// Linux packaging tasks
val packageReleaseAppImage by tasks.registering(PackageAppImageTask::class) {
    dependsOn(generatePackagingFiles)
    targetArch = platform.arch
    appDistributable.from(appDir)
    packagingDir = generatedPackagingDir.map { it.dir("appimage") }
    icon512 = rootProject.file("assets/generated/icon-512.png")
    icon192 = rootProject.file("assets/generated/icon-192.png")
    iconSvg = rootProject.file("assets/generated/app-icon-squircle.svg")
    val archSuffix = platform.arch.map { if (it == Arch.ARM64) "arm64" else "x64" }
    outputAppImage.set(
        layout.buildDirectory.file(
            archSuffix.map { "binaries/appimage/$it/open-aqua-$releasePackageVersion-linux-$it.AppImage" },
        ),
    )
    appimagetoolDir.set(
        layout.buildDirectory.dir(
            archSuffix.map { "tmp/appimage/tool/$it" },
        ),
    )
}

val installReleaseAppImage by tasks.registering(InstallAppImageTask::class) {
    dependsOn(generatePackagingFiles)
    dependsOn(packageReleaseAppImage)
    appImageFile = packageReleaseAppImage.flatMap { it.outputAppImage }
    desktopFile = generatedPackagingDir.map { it.file("appimage/open-aqua.desktop") }
    icon512 = rootProject.file("assets/generated/icon-512.png")
    iconSvg = rootProject.file("assets/generated/app-icon-squircle.svg")
}

// Windows packaging tasks
val packageReleaseMsix by tasks.registering(PackageMsixTask::class) {
    dependsOn(generatePackagingFiles)
    distributionPackages.from(packages)
    packagingDir = generatedPackagingDir.map { it.dir("msix") }
    outputMsix.set(
        layout.buildDirectory.file("binaries/msix/open-aqua-$releasePackageVersion-windows-x64.msix"),
    )
}

val standalonePackage =
    files(
        platform.os.flatMap { targetOs ->
            when (targetOs) {
                Os.LINUX -> {
                    packageReleaseAppImage.map { listOf(it.outputAppImage.get().asFile) }
                }

                Os.WINDOWS -> {
                    when {
                        OperatingSystem.current().isWindows -> {
                            packageReleaseMsix.map { listOf(it.outputMsix.get().asFile) }
                        }

                        else -> {
                            provider { emptyList<File>() }
                        }
                    }
                }

                else -> {
                    provider { emptyList<File>() }
                }
            }
        },
    )

val release by tasks.registering(PublishReleaseArtifactsTask::class) {
    group = "distribution"
    description = "Publishes desktop application packages to root build/releases"
    releaseFiles.from(packages)
    releaseFiles.from(standalonePackage)
    destinationDir = rootProject.layout.buildDirectory.dir("releases")
}

packagesElements.outgoing.artifact(release.flatMap { it.destinationDir })
