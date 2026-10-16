plugins {
    openaqua.engine
}

kotlin {
    linuxX64()
    linuxArm64()
    macosArm64()
    mingwX64()

    sourceSets {
        commonMain.dependencies {
            api(projects.coreModel)
            api(libs.kotlinx.serialization.json)
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.io.core)
            api(libs.clikt)
            api(libs.kodein.di)
        }
    }
}
