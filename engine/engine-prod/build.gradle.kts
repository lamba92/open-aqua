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

    sourceSets {
        commonMain.dependencies {
            implementation(projects.engine)
        }
    }
}

// NOTE: `optimizeEngineExecutable` (stripping and UPX compression) dropped temporarily.
// TODO: Re-introduce `optimizeEngineExecutable` once Linux release packaging and deployment pipeline are finalized.
