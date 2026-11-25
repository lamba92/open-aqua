@file:Suppress("UnstableApiUsage")

rootProject.name = "buildSrc"

plugins {
    id("dev.panuszewski.typesafe-conventions") version "0.11.1"
}

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
        gradlePluginPortal()
    }
}
