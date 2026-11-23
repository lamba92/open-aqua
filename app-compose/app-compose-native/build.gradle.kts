import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink

plugins {
    openaqua.convention.multiplatform
    id(libs.plugins.kotlin.compose)
    id(libs.plugins.compose.multiplatform)
    id("com.bitsycore.compose-desktop-native.bridge")
}

kotlin {
    linuxX64 {
        binaries.all {
            linkerOpts(
                "-L${System.getProperty("user.home")}/.local/lib",
                "-L/home/linuxbrew/.linuxbrew/lib",
                "-L/usr/lib64",
                "-Wl,--allow-multiple-definition",
            )
        }
    }
    linuxArm64()
    mingwX64()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.appCompose)
        }
        nativeMain.dependencies {
            implementation(libs.compose.desktop.native.window)
        }
    }
}

compose.resources {
    packageOfResClass = "open_aqua.app_compose_native.generated.resources"
}

compose.desktop {
    native {
        entryPoint = "io.github.lamba92.openaqua.app.nativeapp.main"
        icon {
            light.from(rootProject.file("assets/generated/icon-512.png"))
            embedWindowsIcon = false
        }
    }
}
