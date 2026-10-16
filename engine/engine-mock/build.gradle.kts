plugins {
    openaqua.engine
}

kotlin {
    linuxX64 {
        binaries {
            executable {
                baseName = "open-aqua-engine-mock"
                entryPoint = "com.openaqua.engine.mock.main"
            }
        }
    }
    linuxArm64 {
        binaries {
            executable {
                baseName = "open-aqua-engine-mock"
                entryPoint = "com.openaqua.engine.mock.main"
            }
        }
    }
    macosArm64 {
        binaries {
            executable {
                baseName = "open-aqua-engine-mock"
                entryPoint = "com.openaqua.engine.mock.main"
            }
        }
    }
    mingwX64 {
        binaries {
            executable {
                baseName = "open-aqua-engine-mock"
                entryPoint = "com.openaqua.engine.mock.main"
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.engine)
        }
    }
}
