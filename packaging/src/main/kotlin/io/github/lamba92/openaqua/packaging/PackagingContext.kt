package io.github.lamba92.openaqua.packaging

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

data class PackagingContext(
    val metadata: AppMetadata,
    val outputDir: Path,
    val version: String,
    val rootDir: Path,
)

fun loadAppMetadata(file: Path? = null): AppMetadata {
    if (file == null) {
        val rootDir = System.getenv("OPENAQUA_ROOT_DIR")?.let { Path(it) } ?: Path(".")
        return loadAppMetadata(rootDir.resolve("packaging/metadata.json"))
    }
    if (!file.exists()) {
        error("Metadata file not found at: ${file.toAbsolutePath()}")
    }
    return jsonSerializer.decodeFromString<AppMetadata>(file.readText())
}

inline fun withMetadata(
    metadataFile: Path? = null,
    action: (PackagingContext) -> Unit,
) {
    val metadata = loadAppMetadata(metadataFile)
    val outputDir =
        System.getenv("OPENAQUA_OUTPUT_DIR")?.let { Path(it) }
            ?: Path("build/generated/packaging")
    val version = System.getenv("OPENAQUA_VERSION") ?: "1.0.0"
    val rootDir =
        System.getenv("OPENAQUA_ROOT_DIR")?.let { Path(it) }
            ?: Path(".")
    val context =
        PackagingContext(
            metadata = metadata,
            outputDir = outputDir,
            version = version,
            rootDir = rootDir,
        )
    action(context)
}

fun linuxStoresMetadata(context: PackagingContext) {
    generateMetadataFiles(context.metadata, context.outputDir, context.rootDir, context.version)
    generateFlatpakFiles(context.metadata, context.outputDir, context.version)
    generateSnapFiles(context.metadata, context.outputDir, context.version)
    generateAurFiles(context.metadata, context.outputDir, context.version)
}

fun windowsStoresMetadata(context: PackagingContext) {
    generateMsixFiles(context.metadata, context.outputDir, context.rootDir, context.version)
}
