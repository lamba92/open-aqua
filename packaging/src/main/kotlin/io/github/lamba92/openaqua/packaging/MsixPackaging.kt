package io.github.lamba92.openaqua.packaging

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.io.path.writeText

const val APPX_FOUNDATION_NS = "http://schemas.microsoft.com/appx/manifest/foundation/windows10"
const val APPX_UAP_NS = "http://schemas.microsoft.com/appx/manifest/uap/windows10"
const val APPX_RESCAP_NS = "http://schemas.microsoft.com/appx/manifest/foundation/windows10/restrictedcapabilities"

@Serializable
@XmlSerialName("Package", APPX_FOUNDATION_NS, "")
data class AppxPackage(
    @XmlSerialName("IgnorableNamespaces", "", "")
    val ignorableNamespaces: String = "uap rescap",
    @XmlElement(true)
    @XmlSerialName("Identity", APPX_FOUNDATION_NS, "")
    val identity: AppxIdentity,
    @XmlElement(true)
    @XmlSerialName("Properties", APPX_FOUNDATION_NS, "")
    val properties: AppxProperties,
    @XmlElement(true)
    @XmlSerialName("Dependencies", APPX_FOUNDATION_NS, "")
    val dependencies: AppxDependencies,
    @XmlElement(true)
    @XmlSerialName("Resources", APPX_FOUNDATION_NS, "")
    val resources: AppxResources,
    @XmlElement(true)
    @XmlSerialName("Applications", APPX_FOUNDATION_NS, "")
    val applications: AppxApplications,
    @XmlElement(true)
    @XmlSerialName("Capabilities", APPX_FOUNDATION_NS, "")
    val capabilities: AppxCapabilities,
)

@Serializable
@XmlSerialName("Identity", APPX_FOUNDATION_NS, "")
data class AppxIdentity(
    val Name: String,
    val Publisher: String,
    val Version: String,
    val ProcessorArchitecture: String = "x64",
)

@Serializable
@XmlSerialName("Properties", APPX_FOUNDATION_NS, "")
data class AppxProperties(
    @XmlElement(true)
    @XmlSerialName("DisplayName", APPX_FOUNDATION_NS, "")
    val displayName: String,
    @XmlElement(true)
    @XmlSerialName("PublisherDisplayName", APPX_FOUNDATION_NS, "")
    val publisherDisplayName: String,
    @XmlElement(true)
    @XmlSerialName("Logo", APPX_FOUNDATION_NS, "")
    val logo: String = "Assets\\StoreLogo.png",
)

@Serializable
@XmlSerialName("Dependencies", APPX_FOUNDATION_NS, "")
data class AppxDependencies(
    @XmlElement(true)
    @XmlSerialName("TargetDeviceFamily", APPX_FOUNDATION_NS, "")
    val targetDeviceFamily: AppxTargetDeviceFamily = AppxTargetDeviceFamily(),
)

@Serializable
@XmlSerialName("TargetDeviceFamily", APPX_FOUNDATION_NS, "")
data class AppxTargetDeviceFamily(
    val Name: String = "Windows.Desktop",
    val MinVersion: String = "10.0.17763.0",
    val MaxVersionTested: String = "10.0.22621.0",
)

@Serializable
@XmlSerialName("Resources", APPX_FOUNDATION_NS, "")
data class AppxResources(
    @XmlElement(true)
    @XmlSerialName("Resource", APPX_FOUNDATION_NS, "")
    val resource: AppxResource = AppxResource(),
)

@Serializable
@XmlSerialName("Resource", APPX_FOUNDATION_NS, "")
data class AppxResource(
    val Language: String = "x-generate",
)

@Serializable
@XmlSerialName("Applications", APPX_FOUNDATION_NS, "")
data class AppxApplications(
    @XmlElement(true)
    @XmlSerialName("Application", APPX_FOUNDATION_NS, "")
    val application: AppxApplication,
)

@Serializable
@XmlSerialName("Application", APPX_FOUNDATION_NS, "")
data class AppxApplication(
    val Id: String,
    val Executable: String,
    val EntryPoint: String = "Windows.FullTrustApplication",
    @XmlElement(true)
    @XmlSerialName("VisualElements", APPX_UAP_NS, "uap")
    val visualElements: AppxVisualElements,
)

@Serializable
@XmlSerialName("VisualElements", APPX_UAP_NS, "uap")
data class AppxVisualElements(
    val DisplayName: String,
    val Description: String,
    val BackgroundColor: String = "transparent",
    val Square150x150Logo: String = "Assets\\Square150x150Logo.png",
    val Square44x44Logo: String = "Assets\\Square44x44Logo.png",
    @XmlElement(true)
    @XmlSerialName("DefaultTile", APPX_UAP_NS, "uap")
    val defaultTile: AppxDefaultTile = AppxDefaultTile(),
)

@Serializable
@XmlSerialName("DefaultTile", APPX_UAP_NS, "uap")
data class AppxDefaultTile(
    val Wide310x150Logo: String = "Assets\\Wide310x150Logo.png",
)

@Serializable
@XmlSerialName("Capabilities", APPX_FOUNDATION_NS, "")
data class AppxCapabilities(
    @XmlElement(true)
    @XmlSerialName("Capability", APPX_RESCAP_NS, "rescap")
    val capability: AppxCapability = AppxCapability(),
)

@Serializable
@XmlSerialName("Capability", APPX_RESCAP_NS, "rescap")
data class AppxCapability(
    val Name: String = "runFullTrust",
)

fun generateMsixFiles(
    metadata: AppMetadata,
    outputDir: Path,
    rootDir: Path,
    version: String,
) {
    val msixDir = (outputDir / "msix").createDirectories()
    val msixFile = msixDir / "AppxManifest.xml"
    msixFile.writeText(generateAppxManifest(metadata, version))
    generateMsixAssets(rootDir, msixDir)
}

fun generateAppxManifest(
    metadata: AppMetadata,
    version: String,
): String {
    val clean = version.removePrefix("v").substringBefore("-")
    val parts = clean.split(".").filter { it.isNotBlank() }
    val fourPartVersion =
        when (parts.size) {
            1 -> "${parts[0]}.0.0.0"
            2 -> "${parts[0]}.${parts[1]}.0.0"
            3 -> "${parts[0]}.${parts[1]}.${parts[2]}.0"
            else -> parts.take(4).joinToString(".")
        }

    val pkg =
        AppxPackage(
            identity =
                AppxIdentity(
                    Name = metadata.msix.identityName,
                    Publisher = metadata.msix.publisher,
                    Version = fourPartVersion,
                    ProcessorArchitecture = "x64",
                ),
            properties =
                AppxProperties(
                    displayName = metadata.name,
                    publisherDisplayName = metadata.msix.publisherDisplayName,
                    logo = "Assets\\StoreLogo.png",
                ),
            dependencies = AppxDependencies(),
            resources = AppxResources(),
            applications =
                AppxApplications(
                    application =
                        AppxApplication(
                            Id = metadata.name,
                            Executable = "${metadata.executable}.exe",
                            visualElements =
                                AppxVisualElements(
                                    DisplayName = metadata.name,
                                    Description = metadata.summary,
                                    Square150x150Logo = "Assets\\Square150x150Logo.png",
                                    Square44x44Logo = "Assets\\Square44x44Logo.png",
                                    defaultTile = AppxDefaultTile(Wide310x150Logo = "Assets\\Wide310x150Logo.png"),
                                ),
                        ),
                ),
            capabilities = AppxCapabilities(),
        )

    val rawXml = xmlSerializer.encodeToString(AppxPackage.serializer(), pkg)
    val fixedXml =
        rawXml.replaceFirst(
            "<Package ",
            "<Package xmlns:uap=\"$APPX_UAP_NS\" xmlns:rescap=\"$APPX_RESCAP_NS\" ",
        )
    return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n$fixedXml\n"
}

fun generateMsixAssets(
    rootDir: Path,
    msixDir: Path,
) {
    val iconSrc = rootDir / "assets/generated/icon-512.png"
    if (!iconSrc.exists()) return
    val assetsDir = (msixDir / "Assets").createDirectories()
    val srcImage = iconSrc.inputStream().use { ImageIO.read(it) } ?: return

    fun saveResized(
        targetW: Int,
        targetH: Int,
        fileName: String,
    ) {
        val target = BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB)
        val g = target.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val scale = minOf(targetW.toDouble() / srcImage.width, targetH.toDouble() / srcImage.height)
        val w = (srcImage.width * scale).toInt()
        val h = (srcImage.height * scale).toInt()
        val x = (targetW - w) / 2
        val y = (targetH - h) / 2
        g.drawImage(srcImage, x, y, w, h, null)
        g.dispose()
        (assetsDir / fileName).outputStream().use { out ->
            ImageIO.write(target, "png", out)
        }
    }

    saveResized(50, 50, "StoreLogo.png")
    saveResized(150, 150, "Square150x150Logo.png")
    saveResized(44, 44, "Square44x44Logo.png")
    saveResized(310, 150, "Wide310x150Logo.png")
}
