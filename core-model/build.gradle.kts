plugins {
    id(libs.plugins.kotlin.multiplatform)
    id(libs.plugins.kotlin.serialization)
    id(libs.plugins.ktlint)
}

kotlin {
    jvmToolchain(21)
    jvm()
    linuxX64()
    linuxArm64()
    mingwX64()
    macosArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
