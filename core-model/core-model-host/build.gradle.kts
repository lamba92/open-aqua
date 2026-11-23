plugins {
    openaqua.convention.multiplatform
}

kotlin {
    linuxX64()
    linuxArm64()
    mingwX64()
    macosArm64()

    sourceSets {
        commonMain.dependencies {
            api(projects.coreModel)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
