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

dependencies {
    implementation(projects.appCompose)
    implementation(getCurrentPlatformComposeRuntime())
    "engineBinaries"(projects.engine.engineMock)
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
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "open-aqua-mock"
            packageVersion = "1.0.0"
        }
    }
}
