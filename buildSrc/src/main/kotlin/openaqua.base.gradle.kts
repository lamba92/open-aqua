val githubEventName = providers.environmentVariable("GITHUB_EVENT_NAME").orNull
val githubRefType = providers.environmentVariable("GITHUB_REF_TYPE").orNull
val githubRefName = providers.environmentVariable("GITHUB_REF_NAME").orNull
val releaseTagEnv = providers.environmentVariable("RELEASE_TAG").orNull
val tagNameEnv = providers.environmentVariable("TAG_NAME").orNull
val releaseTagProp = providers.gradleProperty("releaseTag").orNull

val releaseTag =
    when {
        !releaseTagEnv.isNullOrBlank() -> releaseTagEnv
        !tagNameEnv.isNullOrBlank() -> tagNameEnv
        githubEventName == "release" && !githubRefName.isNullOrBlank() -> githubRefName
        githubRefType == "tag" && !githubRefName.isNullOrBlank() -> githubRefName
        !releaseTagProp.isNullOrBlank() -> releaseTagProp
        else -> null
    }?.removePrefix("v")?.trim()?.takeIf { it.isNotBlank() }

group = "io.github.lamba92.openaqua"
val resolvedVersion = releaseTag ?: "1.0.0-SNAPSHOT"
version = resolvedVersion
rootProject.version = resolvedVersion
