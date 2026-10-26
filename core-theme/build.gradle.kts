plugins {
    id(libs.plugins.kotlin.jvm)
    id(libs.plugins.kotlin.serialization)
    id(libs.plugins.ktlint)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}

ktlint {
    filter {
        exclude("**/build/**")
        exclude("**/generated/**")
    }
}
