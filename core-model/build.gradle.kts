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
            api(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
