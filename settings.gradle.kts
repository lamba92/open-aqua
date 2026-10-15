enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "open-aqua"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(":core-model")
include(":engine-native")
