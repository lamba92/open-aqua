import io.github.lamba92.gradle.plugins.engine.Arch
import io.github.lamba92.gradle.plugins.engine.Os

plugins {
    openaqua.convention.jvm
    application
    openaqua.distribution.consumer
}

application {
    mainClass = "io.github.lamba92.openaqua.packaging.linux.arm64.MainKt"
}

platform {
    os = Os.LINUX
    arch = Arch.ARM64
}

dependencies {
    implementation(projects.packaging)
    distribution(projects.appCompose.appComposeLinuxArm64)
}
