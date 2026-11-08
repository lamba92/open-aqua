package io.github.lamba92.openaqua.packaging

import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.readText

fun main() {
    val metadataFilePath =
        System.getenv("OPENAQUA_METADATA_FILE")?.let { Path(it) }
            ?: Path("packaging/metadata.json")
    val outputDir =
        System.getenv("OPENAQUA_OUTPUT_DIR")?.let { Path(it) }
            ?: Path("packaging/build/generated/packaging")
    val ver = System.getenv("OPENAQUA_VERSION") ?: "1.0.0"
    val rootDir =
        System.getenv("OPENAQUA_ROOT_DIR")?.let { Path(it) }
            ?: Path(".")

    val metadata = jsonSerializer.decodeFromString<AppMetadata>(metadataFilePath.readText())

    generateMetadataFiles(metadata = metadata, outputDir = outputDir, rootDir = rootDir, version = ver)
    generateFlatpakFiles(metadata = metadata, outputDir = outputDir, version = ver)
    generateSnapFiles(metadata = metadata, outputDir = outputDir, version = ver)
    generateAurFiles(metadata = metadata, outputDir = outputDir, version = ver)
    generateMsixFiles(metadata = metadata, outputDir = outputDir, rootDir = rootDir, version = ver)

    println("Successfully generated packaging descriptors for version $ver from ${metadataFilePath.name} into $outputDir.")
}
