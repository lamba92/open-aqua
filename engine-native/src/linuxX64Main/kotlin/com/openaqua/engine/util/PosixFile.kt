package com.openaqua.engine.util

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.set
import kotlinx.cinterop.toKString
import platform.posix.closedir
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.opendir
import platform.posix.readdir

@OptIn(ExperimentalForeignApi::class)
fun readFile(path: String): String? {
    val file = fopen(path, "r") ?: return null
    try {
        memScoped {
            val buffer = allocArray<ByteVar>(4096)
            val bytesRead = fread(buffer, 1u, 4095u, file)
            if (bytesRead > 0u) {
                buffer[bytesRead.toInt()] = 0
                return buffer.toKString()
            }
        }
    } finally {
        fclose(file)
    }
    return null
}

fun readInt(
    path: String,
    default: Int = 0,
): Int = readFile(path)?.trim()?.toIntOrNull() ?: default

fun readDouble(
    path: String,
    default: Double = 0.0,
): Double = readFile(path)?.trim()?.toDoubleOrNull() ?: default

@OptIn(ExperimentalForeignApi::class)
fun listDirectory(path: String): List<String> {
    val dir = opendir(path) ?: return emptyList()
    val names = mutableListOf<String>()
    try {
        var entry = readdir(dir)
        while (entry != null) {
            val name = entry.pointed.d_name.toKString()
            if (name != "." && name != "..") {
                names.add(name)
            }
            entry = readdir(dir)
        }
    } finally {
        closedir(dir)
    }
    return names
}
