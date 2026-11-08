import io.github.lamba92.gradle.plugins.engine.Arch
import io.github.lamba92.gradle.plugins.engine.Os
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    openaqua.convention.jvm
    id(libs.plugins.kotlin.compose)
    id(libs.plugins.compose.multiplatform)
    id(libs.plugins.compose.hot.reload)
    openaqua.engine.consumer
    openaqua.distribution.producer
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
