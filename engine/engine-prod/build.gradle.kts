plugins {
    openaqua.engine
}

kotlin {
    linuxX64 {
        binaries {
            executable {
                baseName = "open-aqua-engine"
                entryPoint = "com.openaqua.engine.prod.main"
            }
        }
    }
    linuxArm64 {
        binaries {
            executable {
                baseName = "open-aqua-engine"
                entryPoint = "com.openaqua.engine.prod.main"
            }
        }
    }
    mingwX64 {
        compilations.getByName("main") {
            cinterops {
                val winhid by creating {
                    defFile(file("src/mingwMain/cinterop/winhid.def"))
                }
            }
        }
        binaries {
            executable {
                baseName = "open-aqua-engine"
                entryPoint = "com.openaqua.engine.prod.main"
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.engine)
        }
    }
}
