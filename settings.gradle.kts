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
include(":engine:engine-device")
include(":engine:engine-device:engine-device-mock")
include(":engine:engine-device:engine-device-prod")
include(":app-compose")
include(":app-compose:app-compose-prod")
include(":app-compose:app-compose-mock")
include(":assets")
