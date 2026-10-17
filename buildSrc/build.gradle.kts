import dev.panuszewski.gradle.pluginMarker

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(pluginMarker(libs.plugins.kotlin.multiplatform))
    implementation(pluginMarker(libs.plugins.kotlin.jvm))
    implementation(pluginMarker(libs.plugins.kotlin.serialization))
    implementation(pluginMarker(libs.plugins.kotlin.compose))
    implementation(pluginMarker(libs.plugins.compose.multiplatform))
    implementation(pluginMarker(libs.plugins.ktlint))
}
