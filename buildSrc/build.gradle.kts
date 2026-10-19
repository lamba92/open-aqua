import dev.panuszewski.gradle.pluginMarker

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(pluginMarker(libs.plugins.kotlin.multiplatform))
    implementation(pluginMarker(libs.plugins.kotlin.jvm))
    implementation(pluginMarker(libs.plugins.kotlin.serialization))
    implementation(pluginMarker(libs.plugins.kotlin.compose))
    implementation(pluginMarker(libs.plugins.compose.multiplatform))
    implementation(pluginMarker(libs.plugins.ktlint))
    implementation("org.tukaani:xz:1.10")
}

gradlePlugin {
    plugins {
        register("upx") {
            id = "io.github.lamba92.upx"
            implementationClass = "io.github.lamba92.gradle.plugins.upx.UpxPlugin"
        }
    }
}
