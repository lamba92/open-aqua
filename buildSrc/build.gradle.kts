import dev.panuszewski.gradle.pluginMarker

plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

ktlint {
    // Ktlint < 1.7.0 crashes on Kotlin 2.2+ context parameters (e.g. context(collector: FlowCollector<String>)).
    // Version 1.8.0 resolves this parsing bug. If ktlint caching issues occur, delete the .gradle cache folders.
    version.set("1.8.0")
    filter {
        exclude {
            it.file.path.contains("/build/") ||
                it.file.path.contains("/generated/")
        }
    }
}

dependencies {
    implementation(pluginMarker(libs.plugins.kotlin.multiplatform))
    implementation(pluginMarker(libs.plugins.kotlin.jvm))
    implementation(pluginMarker(libs.plugins.kotlin.serialization))
    implementation(pluginMarker(libs.plugins.kotlin.compose))
    implementation(pluginMarker(libs.plugins.compose.multiplatform))
    implementation(pluginMarker(libs.plugins.compose.hot.reload))
    implementation(pluginMarker(libs.plugins.ktlint))
    implementation(libs.xz)
    implementation(libs.kotlinpoet)
}

gradlePlugin {
    plugins {
        register("upx") {
            id = "io.github.lamba92.upx"
            implementationClass = "io.github.lamba92.gradle.plugins.upx.UpxPlugin"
        }
    }
}
