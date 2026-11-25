plugins {
    openaqua.convention.native.app
    openaqua.buildinfo
    openaqua.localization
    openaqua.packaging
}

compose.resources {
    packageOfResClass = "open_aqua.app_compose.generated.resources"
}

kotlin {

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
            api(projects.util.kotlinxIo)
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
    }
}
