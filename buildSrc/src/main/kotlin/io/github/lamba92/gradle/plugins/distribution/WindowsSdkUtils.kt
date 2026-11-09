package io.github.lamba92.gradle.plugins.distribution

import java.io.File

object WindowsSdkUtils {
    fun findMakeAppxExecutable(): File? {
        val pathEnv = System.getenv("PATH") ?: ""
        for (dir in pathEnv.split(File.pathSeparator)) {
            val candidate = File(dir, "MakeAppx.exe")
            if (candidate.isFile) return candidate
        }

        val programFilesX86 = System.getenv("ProgramFiles(x86)") ?: "C:\\Program Files (x86)"
        val windowsKitsDir = File(programFilesX86, "Windows Kits\\10\\bin")
        if (windowsKitsDir.isDirectory) {
            val found =
                windowsKitsDir
                    .walk()
                    .filter { it.isFile && it.name.equals("MakeAppx.exe", ignoreCase = true) }
                    .firstOrNull { it.parentFile.name.equals("x64", ignoreCase = true) }
                    ?: windowsKitsDir
                        .walk()
                        .firstOrNull { it.isFile && it.name.equals("MakeAppx.exe", ignoreCase = true) }
            if (found != null) return found
        }

        return null
    }
}
