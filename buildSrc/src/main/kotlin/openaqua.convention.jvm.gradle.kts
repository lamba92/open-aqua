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
    // Ktlint < 1.7.0 crashes on Kotlin 2.2+ context parameters (e.g. context(collector: FlowCollector<String>)).
    // Version 1.8.0 resolves this parsing bug. If ktlint caching issues occur, delete the .gradle cache folders.
    version = "1.8.0"
    filter {
        exclude {
            it.file.path.contains("/build/") ||
                it.file.path.contains("/generated/")
        }
    }
}
