@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        maven("https://maven.pkg.github.com/lamba92/compose-desktop-native") {
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: providers.gradleProperty("gpr.user").orNull ?: ""
                password = System.getenv("GITHUB_TOKEN") ?: providers.gradleProperty("gpr.key").orNull ?: ""
            }
            content { includeGroupAndSubgroups("com.bitsycore") }
        }
        maven("https://maven.bitsycore.com/releases") {
            content { includeGroupAndSubgroups("com.bitsycore") }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("com.gradle.develocity") version "3.19.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
val isCI = System.getenv("CI")?.equals("true", ignoreCase = true) == true
develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
        publishing.onlyIf { isCI }
        if (isCI) {
            tag("CI")
            System.getenv("GITHUB_WORKFLOW")?.let { tag(it) }
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "open-aqua"

dependencyResolutionManagement {
    repositories {
        maven("https://maven.pkg.github.com/lamba92/compose-desktop-native") {
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: providers.gradleProperty("gpr.user").orNull ?: ""
                password = System.getenv("GITHUB_TOKEN") ?: providers.gradleProperty("gpr.key").orNull ?: ""
            }
            content { includeGroupAndSubgroups("com.bitsycore") }
        }
        maven("https://maven.bitsycore.com/releases") {
            content { includeGroupAndSubgroups("com.bitsycore") }
        }
        mavenCentral()
        google()
    }
}

include(
    ":app-compose",
    ":app-compose:app-compose-mock",
    ":app-compose:app-compose-screenshots",
    ":assets",
    ":core-model",
    ":core-model:core-model-device",
    ":core-model:core-model-host",
    ":core-theme",
    ":engine",
    ":engine:engine-device",
    ":engine:engine-host",
    ":packaging",
    ":packaging:packaging-linux-x64",
    ":packaging:packaging-linux-arm64",
    ":packaging:packaging-windows-x64",
)