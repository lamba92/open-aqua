import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    openaqua.convention.jvm
    id(libs.plugins.kotlin.compose)
    id(libs.plugins.compose.multiplatform)
    id(libs.plugins.compose.hot.reload)
    openaqua.engine.consumer
}

dependencies {
    implementation(projects.appCompose)
    engineBinaries(projects.engine.engineDevice.engineDeviceMock)
    engineBinaries(projects.engine.engineHost.engineHostMock)
}

compose.desktop {
    application {
        mainClass = "io.github.lamba92.openaqua.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "open-aqua-mock"
            packageVersion = "1.0.0"

            linux {
                iconFile = rootProject.file("assets/generated/icon-512.png")
            }
            windows {
                iconFile = rootProject.file("assets/generated/favicon.ico")
            }
            macOS {
                iconFile = rootProject.file("assets/generated/icon.icns")
            }
        }
    }
}
