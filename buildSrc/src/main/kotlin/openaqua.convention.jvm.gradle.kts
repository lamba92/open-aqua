plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    id("openaqua.base")
}

kotlin {
    jvmToolchain(21)
}

ktlint {
    filter {
        exclude { it.file.path.contains("/build/") || it.file.path.contains("/generated/") }
    }
}
