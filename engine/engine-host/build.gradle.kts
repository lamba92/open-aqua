plugins {
    openaqua.engine
}

kotlin {
    linuxX64()
    linuxArm64()
    macosArm64()
    mingwX64()

    sourceSets {
        commonMain.dependencies {
            api(projects.engine)
            api(projects.coreModel.coreModelHost)
            api(libs.kodein.di)
        }
    }
}
