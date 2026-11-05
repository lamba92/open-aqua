plugins {
    base
    openaqua.engine.producer
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

engineBinaries {
    register("windowsX64") {
        binaryPrefix = "open-aqua-host-engine"
        binaryFile = publishWindowsExecutable.map { executablePath.get() }
    }
}
