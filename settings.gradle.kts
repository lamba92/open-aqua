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

include(
    ":app-compose",
    ":app-compose:app-compose-mock",
    ":app-compose:app-compose-prod",
    ":assets",
    ":core-model",
    ":core-model:core-model-device",
    ":core-theme",
    ":engine:engine-device",
    ":engine:engine-device:engine-device-mock",
    ":engine:engine-device:engine-device-prod"
)
