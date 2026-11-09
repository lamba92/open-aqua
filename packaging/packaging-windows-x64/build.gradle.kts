import io.github.lamba92.gradle.plugins.engine.Arch
import io.github.lamba92.gradle.plugins.engine.Os

plugins {
    openaqua.convention.jvm
    application
    openaqua.distribution.consumer
}

application {
    mainClass = "io.github.lamba92.openaqua.packaging.windows.x64.MainKt"
}

platform {
    os = Os.WINDOWS
    arch = Arch.X64
}

dependencies {
    implementation(projects.packaging)
    distribution(projects.appCompose.appComposeWindowsX64)
}
