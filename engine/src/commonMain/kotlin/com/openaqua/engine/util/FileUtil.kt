package com.openaqua.engine.util

import kotlinx.io.buffered
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString

fun Path.readText(fileSystem: FileSystem = SystemFileSystem): String = fileSystem.source(this).buffered().use { it.readString() }

fun Path.readTextOrNull(fileSystem: FileSystem = SystemFileSystem): String? =
    try {
        if (!fileSystem.exists(this)) null
        else fileSystem.source(this).buffered().use { it.readString() }
    } catch (_: Exception) {
        null
    }

fun Path.listDirectory(fileSystem: FileSystem = SystemFileSystem): List<Path> =
    if (!fileSystem.exists(this)) emptyList()
    else try {
        fileSystem.list(this).toList()
    } catch (_: Exception) {
        emptyList()
    }
