import io.github.lamba92.gradle.plugins.upx.UpxTask

plugins {
    id("base")
    id("openaqua.engine.producer")
    id("io.github.lamba92.upx")
}

upx {
    bruteLevel = UpxTask.BruteLevel.ULTRA_BRUTE
}

val executablePath = layout.buildDirectory.file("publish/win-x64/open-aqua-host-engine.exe")
val publishWindowsExecutable by tasks.registering(Exec::class) {
    group = "build"
    description = "Publishes the Windows host engine using dotnet"
    workingDir = projectDir
    inputs.dir("src/main/csharp")
    inputs.file("OpenAqua.Engine.Host.Windows.csproj")
    val dotnetPath =
        file("${System.getProperty("user.home")}/.dotnet/dotnet").takeIf { it.exists() }?.absolutePath
            ?: "dotnet"
    commandLine(
        dotnetPath,
        "publish",
        "OpenAqua.Engine.Host.Windows.csproj",
        "-c",
        "Release",
        "-r",
        "win-x64",
        "-o",
        layout.buildDirectory.dir("publish/win-x64").get().asFile.absolutePath,
    )
    outputs.file(executablePath)
}

val compressReleaseExecutableWindowsX64 by tasks.registering(UpxTask::class) {
    upxExecutableFile = upx.upxExecutableProvider
    bruteLevel.convention(upx.bruteLevel)
    inputExecutable = publishWindowsExecutable.map { executablePath.get() }
    outputExecutable = layout.buildDirectory.file("upx/windowsX64/open-aqua-host-engine.exe")
    strip = false
    additionalOptions.add("--force")
}

engineBinaries {
    register("windowsX64") {
        binaryPrefix = "open-aqua-host-engine"
        binaryFile = compressReleaseExecutableWindowsX64.flatMap { it.outputExecutable }
    }
}
