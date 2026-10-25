import io.github.lamba92.gradle.plugins.engine.Arch
import io.github.lamba92.gradle.plugins.engine.Os
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id(libs.plugins.kotlin.jvm)
    id(libs.plugins.kotlin.serialization)
    id(libs.plugins.kotlin.compose)
    id(libs.plugins.compose.multiplatform)
    id(libs.plugins.ktlint)
    id("openaqua.engine.consumer")
}

kotlin {
    jvmToolchain(21)
}

ktlint {
    filter {
        exclude("**/build/**")
        exclude("**/generated/**")
    }
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
    engineBinaries(projects.engine.engineProd)
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
                iconFile = rootProject.file(".github/assets/icon-512.png")
            }
            windows {
                iconFile = rootProject.file(".github/assets/favicon.ico")
            }
            macOS {
                iconFile = rootProject.file(".github/assets/icon.icns")
            }
        }
    }
}
