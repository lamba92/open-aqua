plugins {
    openaqua.convention.jvm
    openaqua.buildinfo
    openaqua.localization
    id(libs.plugins.kotlin.compose)
    id(libs.plugins.compose.multiplatform)
    id(libs.plugins.compose.hot.reload)
}

compose.resources {
    packageOfResClass = "open_aqua.app_compose.generated.resources"
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-XXLanguage:+FullValueClasses")
    }
}

dependencies {
    api(getCurrentPlatformComposeRuntime())
    api(libs.coil.compose)
    api(libs.coil.svg)
    api(libs.compose.components.resources)
    api(libs.compose.material.icons.extended)
    api(libs.compose.material3)
    api(libs.kermit)
    api(libs.kermit.io)
    api(libs.kodein.di)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.swing)
    api(libs.kotlinx.datetime)
    api(libs.kotlinx.io.core)
    api(libs.kotlinx.serialization.json)
    api(libs.ktor.client.cio)
    api(libs.ktor.server.cio)
    api(projects.coreModel.coreModelDevice)
    api(projects.coreModel.coreModelHost)
    api(projects.coreTheme)
}
