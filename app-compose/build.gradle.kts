plugins {
    id(libs.plugins.kotlin.jvm)
    id(libs.plugins.kotlin.serialization)
    id(libs.plugins.kotlin.compose)
    id(libs.plugins.compose.multiplatform)
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

dependencies {
    implementation(projects.coreModel)
    when (SupportedTargets.getCurrent()) {
        SupportedTargets.LinuxX64 -> implementation(libs.compose.desktop.linux.x64)
        SupportedTargets.LinuxArm64 -> implementation(libs.compose.desktop.linux.arm64)
        SupportedTargets.WindowsX64 -> implementation(libs.compose.desktop.windows.x64)
        SupportedTargets.MacOSX64 -> implementation(libs.compose.desktop.macos.x64)
        SupportedTargets.MacOSArm64 -> implementation(libs.compose.desktop.macos.arm64)
    }
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.components.resources)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.serialization.json)
}

compose.desktop {
    application {
        mainClass = "io.github.lamba92.openaqua.app.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Rpm,
            )
            packageName = "open-aqua"
            packageVersion = "1.0.0"
        }
    }
}
