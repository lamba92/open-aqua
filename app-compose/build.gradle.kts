plugins {
    openaqua.convention.jvm
    openaqua.buildinfo
    openaqua.localization
    id(libs.plugins.kotlin.compose)
    id(libs.plugins.compose.multiplatform)
    id(libs.plugins.compose.hot.reload)
}

compose.resources {
    packageOfResClass = "open_aqua.app_compose.generated.resources"
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-XXLanguage:+FullValueClasses")
    }
}

ktlint {
    filter {
        exclude {
            it.file.path.contains("/build/") ||
                it.file.path.contains("/generated/") ||
                it.file.name == "CoroutineScopeProvider.kt"
        }
    }
}

dependencies {
    implementation(projects.coreModel.coreModelDevice)
    implementation(projects.coreModel.coreModelHost)
    api(projects.coreTheme)
    implementation(getCurrentPlatformComposeRuntime())
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.components.resources)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.kodein.di)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.io.core)
    implementation(libs.kermit)
}

fun getCurrentPlatformComposeRuntime() =
    when (SupportedTargets.getCurrent()) {
        SupportedTargets.LinuxX64 -> libs.compose.desktop.linux.x64
        SupportedTargets.LinuxArm64 -> libs.compose.desktop.linux.arm64
        SupportedTargets.WindowsX64 -> libs.compose.desktop.windows.x64
        SupportedTargets.MacOSX64 -> libs.compose.desktop.macos.x64
        SupportedTargets.MacOSArm64 -> libs.compose.desktop.macos.arm64
    }
