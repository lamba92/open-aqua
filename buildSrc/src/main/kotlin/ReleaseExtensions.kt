import org.gradle.api.Project
import java.io.File
import java.security.MessageDigest

fun File.sha256Hex(): String {
    val md = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(8192)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            md.update(buffer, 0, read)
        }
    }
    return md.digest().joinToString("") { "%02x".format(it) }
}

fun Project.publishReleaseArtifact(
    file: File,
    destinationName: String = file.name,
): File {
    val targetDir =
        rootProject.layout.buildDirectory
            .dir("releases")
            .get()
            .asFile
    targetDir.mkdirs()
    val dest = File(targetDir, destinationName)
    file.copyTo(dest, overwrite = true)
    val hash = dest.sha256Hex()
    File(targetDir, "${dest.name}.sha256").writeText("$hash  ${dest.name}\n")
    logger.lifecycle("Published release asset: ${dest.name} (${dest.length() / 1024} KB)")
    return dest
}

fun Project.publishReleaseArtifacts(files: Iterable<File>) {
    files.forEach { publishReleaseArtifact(it) }
}
