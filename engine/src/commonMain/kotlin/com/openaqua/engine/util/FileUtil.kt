package com.openaqua.engine.util

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString

fun readFile(pathString: String): String? {
    val path = Path(pathString)
    if (!SystemFileSystem.exists(path)) return null
    return try {
        SystemFileSystem.source(path).buffered().use { it.readString() }
    } catch (_: Exception) {
        null
    }
}

fun readInt(
    path: String,
    default: Int = 0,
): Int = readFile(path)?.trim()?.toIntOrNull() ?: default

fun readDouble(
    path: String,
    default: Double = 0.0,
): Double = readFile(path)?.trim()?.toDoubleOrNull() ?: default

fun listDirectory(pathString: String): List<String> {
    val path = Path(pathString)
    if (!SystemFileSystem.exists(path)) return emptyList()
    return try {
        SystemFileSystem.list(path).map { it.name }
    } catch (_: Exception) {
        emptyList()
    }
}
