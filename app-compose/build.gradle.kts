plugins {
    openaqua.convention.multiplatform
    openaqua.buildinfo
    openaqua.localization
    id(libs.plugins.kotlin.compose)
    id(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.desktop.native.bridge)
}

compose.resources {
    packageOfResClass = "open_aqua.app_compose.generated.resources"
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
            api(libs.compose.components.resources)
            api(libs.compose.material3)
            api(libs.compose.desktop.native.window)
            api(libs.kermit)
            api(libs.kermit.io)
            api(libs.kodein.di)
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.io.core)
            api(libs.kotlinx.serialization.json)
            api(libs.ktor.client.content.negotiation)
            api(libs.ktor.client.curl)
            api(libs.ktor.serialization.kotlinx.json)
            api(libs.ktor.server.cio)
            api(libs.ktor.server.content.negotiation)
            api(projects.coreModel.coreModelDevice)
            api(projects.coreModel.coreModelHost)
            api(projects.coreTheme)
            api(projects.engine.engineDevice)
            api(projects.engine.engineHost)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

compose.desktop {
    native {
        entryPoint = "io.github.lamba92.openaqua.app.main"
        icon {
            light.from(rootProject.file("assets/generated/icon-512.png"))
            embedWindowsIcon = false
        }
    }
}
