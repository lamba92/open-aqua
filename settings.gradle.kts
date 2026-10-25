@file:Suppress("UnstableApiUsage")

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
        google()
    }
}

include(":core-model")
include(":core-theme")
include(":engine")
include(":engine:engine-mock")
include(":engine:engine-prod")
include(":app-compose")
include(":app-compose:app-compose-prod")
include(":app-compose:app-compose-mock")
include(":brand-assets")
