import io.github.lamba92.gradle.plugins.distribution.PackageFormat
import io.github.lamba92.gradle.plugins.engine.Arch
import io.github.lamba92.gradle.plugins.engine.Os
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask

plugins {
    id("openaqua.compose.desktop")
}

platform {
    os = Os.WINDOWS
    arch = Arch.X64
}

dependencies {
    implementation(projects.appCompose)
    engineBinaries(projects.engine.engineDevice.engineDeviceProd)
    engineBinaries(projects.engine.engineHost.engineHostWindows)
}

compose.desktop {
    application {
        nativeDistributions {
            targetFormats(TargetFormat.Msi)
        }
    }
}

afterEvaluate {
    nativeDistributions {
        register("appDir") {
            format = PackageFormat.APP_DIR
            from(tasks.named<AbstractJPackageTask>("createReleaseDistributable"))
        }
        register("msi") {
            format = PackageFormat.MSI
            from(tasks.named<AbstractJPackageTask>("packageReleaseMsi"))
        }
    }
}
