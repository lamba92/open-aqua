plugins {
    openaqua.convention.native.app
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.appCompose)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

compose.desktop {
    native {
        entryPoint = "io.github.lamba92.openaqua.app.mock.main"
    }
}
