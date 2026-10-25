plugins {
    id(libs.plugins.kotlin.jvm)
    id(libs.plugins.ktlint)
}

kotlin {
    jvmToolchain(21)
}

ktlint {
    filter {
        exclude("**/build/**")
        exclude("**/generated/**")
    }
}
