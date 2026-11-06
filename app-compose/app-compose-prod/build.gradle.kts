import io.github.lamba92.gradle.plugins.engine.Arch
import io.github.lamba92.gradle.plugins.engine.Os
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

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
    implementation(getCurrentPlatformComposeRuntime())
    engineBinaries(projects.engine.engineDevice.engineDeviceProd)
    val currentOs = if (Os.current() == Os.MACOS) Os.LINUX else SupportedTargets.getCurrent().os
    when (currentOs) {
        Os.LINUX -> engineBinaries(projects.engine.engineHost.engineHostLinux)
        Os.WINDOWS -> engineBinaries(projects.engine.engineHost.engineHostWindows)
        else -> Unit
    }
}

fun getCurrentPlatformComposeRuntime() =
    when (SupportedTargets.getCurrent()) {
        SupportedTargets.LinuxX64 -> libs.compose.desktop.linux.x64
        SupportedTargets.LinuxArm64 -> libs.compose.desktop.linux.arm64
        SupportedTargets.WindowsX64 -> libs.compose.desktop.windows.x64
        SupportedTargets.MacOSX64 -> libs.compose.desktop.macos.x64
        SupportedTargets.MacOSArm64 -> libs.compose.desktop.macos.arm64
    }

compose.desktop {
    application {
        mainClass = "io.github.lamba92.openaqua.app.MainKt"

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
