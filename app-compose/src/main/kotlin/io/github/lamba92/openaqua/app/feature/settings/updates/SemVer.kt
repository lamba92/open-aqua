package io.github.lamba92.openaqua.app.feature.settings.updates

data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String? = null,
) : Comparable<SemVer> {
    companion object {
        private val REGEX = Regex("""^[vV]?(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:-([0-9A-Za-z.-]+))?""")

        fun parseOrNull(version: String): SemVer? {
            val trimmed = version.trim()
            val match = REGEX.find(trimmed) ?: return null
            val major = match.groupValues[1].toIntOrNull() ?: return null
            val minor = match.groupValues[2].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
            val patch = match.groupValues[3].takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
            val pre = match.groupValues[4].takeIf { it.isNotEmpty() }
            return SemVer(major, minor, patch, pre)
        }
    }

    override fun compareTo(other: SemVer): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        if (patch != other.patch) return patch.compareTo(other.patch)
        val preA = preRelease
        val preB = other.preRelease
        return when {
            preA == null && preB == null -> 0

            preA == null -> 1

            // Final release is newer than a prerelease
            preB == null -> -1

            // Prerelease is older than a final release
            else -> preA.compareTo(preB)
        }
    }

    override fun toString(): String =
        buildString {
            append("$major.$minor.$patch")
            if (!preRelease.isNullOrBlank()) {
                append("-$preRelease")
            }
        }
}
