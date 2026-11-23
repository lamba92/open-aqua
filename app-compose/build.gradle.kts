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
    linuxX64()
    linuxArm64()
    mingwX64()

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.components.resources)
            api(libs.compose.material3)
            api(libs.kermit)
            api(libs.kermit.io)
            api(libs.kodein.di)
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.io.core)
            api(libs.kotlinx.serialization.json)
            api(libs.ktor.client.content.negotiation)
            api(libs.ktor.serialization.kotlinx.json)
            api(projects.coreModel.coreModelDevice)
            api(projects.coreModel.coreModelHost)
            api(projects.coreTheme)
            api(libs.clikt)
        }
        nativeMain.dependencies {
            api(projects.engine.engineDevice.engineDeviceProd)
            api(libs.compose.desktop.native.window)
            api(libs.ktor.client.curl)
        }
        linuxMain.dependencies {
            implementation(projects.engine.engineHost.engineHostLinux)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
