import dev.panuszewski.gradle.pluginMarker

plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

repositories {
    maven("https://maven.pkg.github.com/lamba92/compose-desktop-native") {
        credentials {
            username = System.getenv("GITHUB_ACTOR") ?: providers.gradleProperty("gpr.user").orNull ?: ""
            password = System.getenv("GITHUB_TOKEN") ?: providers.gradleProperty("gpr.key").orNull ?: ""
        }
        content { includeGroupAndSubgroups("com.bitsycore") }
    }
    maven("https://maven.bitsycore.com/releases") {
        content { includeGroupAndSubgroups("com.bitsycore") }
    }
    mavenCentral()
    gradlePluginPortal()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
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

dependencies {
    implementation(pluginMarker(libs.plugins.kotlin.multiplatform))
    implementation(pluginMarker(libs.plugins.kotlin.jvm))
    implementation(pluginMarker(libs.plugins.kotlin.serialization))
    implementation(pluginMarker(libs.plugins.kotlin.compose))
    implementation(pluginMarker(libs.plugins.compose.multiplatform))
    implementation(pluginMarker(libs.plugins.compose.desktop.native.bridge))
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
