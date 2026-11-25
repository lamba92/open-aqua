plugins {
    id("openaqua.convention.multiplatform")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
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
}

compose.desktop {
    native {
        icon {
            light.from(rootProject.file("assets/generated/icon-512.png"))
            embedWindowsIcon = false
        }
    }
}
