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
    ":app-compose:app-compose-screenshots",
    ":assets",
    ":core-model",
    ":core-model:core-model-device",
    ":core-model:core-model-host",
    ":core-theme",
    ":engine",
    ":engine:engine-device",
    ":engine:engine-device:engine-device-mock",
    ":engine:engine-device:engine-device-prod",
    ":engine:engine-host",
    ":engine:engine-host:engine-host-linux",
    ":engine:engine-host:engine-host-mock",
    ":engine:engine-host:engine-host-windows",
    ":packaging",
)
