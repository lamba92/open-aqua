package io.github.lamba92.openaqua.brand

import io.github.lamba92.openaqua.theme.OpenAquaPalette
import io.github.lamba92.openaqua.theme.toAwtColor
import java.awt.BasicStroke
import java.awt.Font
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.RenderingHints
import java.awt.font.TextAttribute
import java.awt.geom.Arc2D
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.ImageIO

data class MarkTransform(
    val translateX: Double,
    val translateY: Double,
    val scale: Double,
)

object BrandTransforms {
    val transparentIcon = MarkTransform(translateX = 30.72, translateY = 20.64, scale = 0.88)
    val squircleAppIcon = MarkTransform(translateX = 71.68, translateY = 63.36, scale = 0.72)
    val faviconOptimized = MarkTransform(translateX = 61.44, translateY = 53.76, scale = 0.76)
    val socialBanner = MarkTransform(translateX = 140.0, translateY = 115.0, scale = 0.78)
    val readmeBanner = MarkTransform(translateX = 20.0, translateY = 5.0, scale = 0.60)
}

fun main(args: Array<String>) {
    val assetsDir = File(args.getOrNull(0) ?: "assets").apply { mkdirs() }
    val composeResourcesDir = File(args.getOrNull(1) ?: "app-compose/src/main/composeResources")
    val composeDrawableDir = File(composeResourcesDir, "drawable").apply { mkdirs() }

    // 1. Generate Master SVGs
    val iconTransparentSvg = SvgGenerator.iconTransparent()
    val appIconSquircleSvg = SvgGenerator.appIconSquircle()
    val faviconOptimizedSvg = SvgGenerator.faviconOptimized()
    val bannerSocialSvg = SvgGenerator.bannerSocial()
    val bannerReadmeLightSvg = SvgGenerator.bannerReadme(darkMode = false)
    val bannerReadmeDarkSvg = SvgGenerator.bannerReadme(darkMode = true)

    File(assetsDir, "icon-transparent.svg").writeText(iconTransparentSvg)
    File(assetsDir, "app-icon-squircle.svg").writeText(appIconSquircleSvg)
    File(assetsDir, "favicon-optimized.svg").writeText(faviconOptimizedSvg)
    File(assetsDir, "banner-social-1200x630.svg").writeText(bannerSocialSvg)
    File(assetsDir, "banner-readme-light.svg").writeText(bannerReadmeLightSvg)
    File(assetsDir, "banner-readme-dark.svg").writeText(bannerReadmeDarkSvg)

    // 2. Rasterize PNGs
    val appleTouchPng = RasterRenderer.renderSquircleIconPng(180)
    val icon192Png = RasterRenderer.renderSquircleIconPng(192)
    val icon512Png = RasterRenderer.renderSquircleIconPng(512)
    val bannerSocialPng = RasterRenderer.renderSocialBannerPng()

    File(assetsDir, "apple-touch-icon.png").writeBytes(appleTouchPng)
    File(assetsDir, "icon-192.png").writeBytes(icon192Png)
    File(assetsDir, "icon-512.png").writeBytes(icon512Png)
    File(assetsDir, "banner-social-1200x630.png").writeBytes(bannerSocialPng)

    // 3. Generate multi-resolution ICO & macOS ICNS containers
    val faviconIco = BinaryContainerEncoder.encodeIco(listOf(16, 32, 48, 64, 256))
    val macOsIcns = BinaryContainerEncoder.encodeIcns()

    File(assetsDir, "favicon.ico").writeBytes(faviconIco)
    File(assetsDir, "icon.icns").writeBytes(macOsIcns)

    // 4. Sync runtime icons into Compose Multiplatform resources
    File(composeDrawableDir, "ic_openaqua.svg").writeText(iconTransparentSvg)
    File(composeDrawableDir, "ic_app_icon.png").writeBytes(icon512Png)

    println("Generated and synced OpenAqua brand assets to:")
    println("  - ${assetsDir.absolutePath}")
    println("  - ${composeDrawableDir.absolutePath}")
}

object SvgGenerator {
    private val deepNavyHex = OpenAquaPalette.DeepNavy.hex
    private val aquaBlueHex = OpenAquaPalette.AquaBlue.hex
    private val pureWhiteHex = OpenAquaPalette.PureWhite.hex

    private fun primaryMarkGroup(transform: MarkTransform): String =
        """
        <g transform="translate(${formatNum(
            transform.translateX,
        )}, ${formatNum(transform.translateY)}) scale(${formatNum(transform.scale)})">
          <path
            d="M 256 64 C 256 64 112 236 112 332 A 144 144 0 0 0 400 332 C 400 236 256 64 256 64 Z"
            fill="none"
            stroke="$aquaBlueHex"
            stroke-width="30"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
          <polyline
            points="222,276 168,330 222,384"
            fill="none"
            stroke="$aquaBlueHex"
            stroke-width="28"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
          <polyline
            points="290,276 344,330 290,384"
            fill="none"
            stroke="$aquaBlueHex"
            stroke-width="28"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </g>
        """.trimIndent()

    fun iconTransparent(): String =
        """
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
${primaryMarkGroup(BrandTransforms.transparentIcon)}
</svg>
        """.trimIndent() + "\n"

    fun appIconSquircle(): String =
        """
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
  <rect width="512" height="512" rx="115" fill="$deepNavyHex" />
${primaryMarkGroup(BrandTransforms.squircleAppIcon)}
</svg>
        """.trimIndent() + "\n"

    fun faviconOptimized(): String {
        val t = BrandTransforms.faviconOptimized
        return """
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
  <rect width="512" height="512" rx="96" fill="$deepNavyHex" />
  <g transform="translate(${formatNum(t.translateX)}, ${formatNum(t.translateY)}) scale(${formatNum(t.scale)})">
    <path
      d="M 256 56 C 256 56 104 236 104 336 A 152 152 0 0 0 408 336 C 408 236 256 56 256 56 Z"
      fill="none"
      stroke="$aquaBlueHex"
      stroke-width="40"
      stroke-linecap="round"
      stroke-linejoin="round"
    />
    <polyline
      points="218,272 162,328 218,384"
      fill="none"
      stroke="$aquaBlueHex"
      stroke-width="38"
      stroke-linecap="round"
      stroke-linejoin="round"
    />
    <polyline
      points="294,272 350,328 294,384"
      fill="none"
      stroke="$aquaBlueHex"
      stroke-width="38"
      stroke-linecap="round"
      stroke-linejoin="round"
    />
  </g>
</svg>
            """.trimIndent() + "\n"
    }

    fun bannerSocial(): String =
        """
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 630" width="1200" height="630">
  <rect width="1200" height="630" fill="$deepNavyHex" />
${primaryMarkGroup(BrandTransforms.socialBanner)}
  <text x="500" y="355" font-family="Inter, Montserrat, system-ui, -apple-system, sans-serif" font-size="112" font-weight="700" letter-spacing="-2"><tspan fill="$pureWhiteHex">open-</tspan><tspan fill="$aquaBlueHex">aqua</tspan></text>
</svg>
        """.trimIndent() + "\n"

    fun bannerReadme(darkMode: Boolean): String {
        val openColor = if (darkMode) pureWhiteHex else deepNavyHex
        return """
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1000 320" width="1000" height="320">
${primaryMarkGroup(BrandTransforms.readmeBanner)}
  <text x="310" y="200" font-family="Inter, Montserrat, system-ui, -apple-system, sans-serif" font-size="108" font-weight="700" letter-spacing="-2"><tspan fill="$openColor">open-</tspan><tspan fill="$aquaBlueHex">aqua</tspan></text>
</svg>
            """.trimIndent() + "\n"
    }

    private fun formatNum(value: Double): String =
        if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            value.toBigDecimal().stripTrailingZeros().toPlainString()
        }
}

object RasterRenderer {
    private val deepNavyAwt = OpenAquaPalette.DeepNavy.toAwtColor()
    private val aquaBlueAwt = OpenAquaPalette.AquaBlue.toAwtColor()
    private val pureWhiteAwt = OpenAquaPalette.PureWhite.toAwtColor()

    fun renderSquircleIconPng(size: Int): ByteArray =
        renderToPng(size, size) { g ->
            val scale = size / 512.0
            g.scale(scale, scale)
            g.color = deepNavyAwt
            g.fill(RoundRectangle2D.Double(0.0, 0.0, 512.0, 512.0, 230.0, 230.0))
            drawMark(g, BrandTransforms.squircleAppIcon, optimized = false)
        }

    fun renderFaviconOptimizedPng(size: Int): ByteArray =
        renderToPng(size, size) { g ->
            val scale = size / 512.0
            g.scale(scale, scale)
            g.color = deepNavyAwt
            g.fill(RoundRectangle2D.Double(0.0, 0.0, 512.0, 512.0, 192.0, 192.0))
            drawMark(g, BrandTransforms.faviconOptimized, optimized = true)
        }

    fun renderSocialBannerPng(): ByteArray =
        renderToPng(1200, 630) { g ->
            g.color = deepNavyAwt
            g.fillRect(0, 0, 1200, 630)

            val savedTransform = g.transform
            drawMark(g, BrandTransforms.socialBanner, optimized = false)
            g.transform = savedTransform

            val font = resolveBrandFont(112f)
            g.font = font
            val fm = g.fontMetrics
            val openText = "open-"
            val aquaText = "aqua"

            g.color = pureWhiteAwt
            g.drawString(openText, 500f, 355f)

            val openWidth = fm.stringWidth(openText)
            g.color = aquaBlueAwt
            g.drawString(aquaText, 500f + openWidth, 355f)
        }

    private fun drawMark(
        g: Graphics2D,
        transform: MarkTransform,
        optimized: Boolean,
    ) {
        g.translate(transform.translateX, transform.translateY)
        g.scale(transform.scale, transform.scale)
        g.color = aquaBlueAwt

        val dropletStroke = if (optimized) 40f else 30f
        val bracketStroke = if (optimized) 38f else 28f

        g.stroke = BasicStroke(dropletStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.draw(createDropletPath(optimized))

        g.stroke = BasicStroke(bracketStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        if (optimized) {
            g.draw(createPolyline(218.0, 272.0, 162.0, 328.0, 218.0, 384.0))
            g.draw(createPolyline(294.0, 272.0, 350.0, 328.0, 294.0, 384.0))
        } else {
            g.draw(createPolyline(222.0, 276.0, 168.0, 330.0, 222.0, 384.0))
            g.draw(createPolyline(290.0, 276.0, 344.0, 330.0, 290.0, 384.0))
        }
    }

    private fun createDropletPath(optimized: Boolean): Path2D.Double {
        val topY = if (optimized) 56.0 else 64.0
        val leftX = if (optimized) 104.0 else 112.0
        val rightX = if (optimized) 408.0 else 400.0
        val arcY = if (optimized) 336.0 else 332.0
        val radius = if (optimized) 152.0 else 144.0

        val path = Path2D.Double()
        path.moveTo(256.0, topY)
        path.curveTo(256.0, topY, leftX, 236.0, leftX, arcY)
        val arc =
            Arc2D.Double(
                256.0 - radius,
                arcY - radius,
                radius * 2.0,
                radius * 2.0,
                180.0,
                180.0,
                Arc2D.OPEN,
            )
        path.append(arc, true)
        path.curveTo(rightX, 236.0, 256.0, topY, 256.0, topY)
        path.closePath()
        return path
    }

    private fun createPolyline(
        x1: Double,
        y1: Double,
        x2: Double,
        y2: Double,
        x3: Double,
        y3: Double,
    ): Path2D.Double =
        Path2D.Double().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
            lineTo(x3, y3)
        }

    private fun resolveBrandFont(sizePx: Float): Font {
        val available =
            GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .availableFontFamilyNames
                .toSet()
        val family =
            listOf("Inter", "Montserrat", ".AppleSystemUIFont", "SF Pro Display", "Segoe UI", "Roboto", "SansSerif")
                .firstOrNull { it in available } ?: Font.SANS_SERIF

        val base = Font(family, Font.BOLD, sizePx.toInt()).deriveFont(sizePx)
        return base.deriveFont(
            mapOf(
                TextAttribute.WEIGHT to TextAttribute.WEIGHT_BOLD,
                TextAttribute.TRACKING to (-2f / sizePx),
            ),
        )
    }

    private fun renderToPng(
        width: Int,
        height: Int,
        draw: (Graphics2D) -> Unit,
    ): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
            draw(g)
        } finally {
            g.dispose()
        }
        val out = ByteArrayOutputStream()
        ImageIO.write(image, "PNG", out)
        return out.toByteArray()
    }
}

object BinaryContainerEncoder {
    fun encodeIco(sizes: List<Int>): ByteArray {
        val payloads = sizes.map { RasterRenderer.renderFaviconOptimizedPng(it) }
        val totalBytes = 6 + (sizes.size * 16) + payloads.sumOf { it.size }
        val buf = ByteBuffer.allocate(totalBytes).order(ByteOrder.LITTLE_ENDIAN)

        // ICONDIR header
        buf.putShort(0) // Reserved
        buf.putShort(1) // Type: 1 = ICO
        buf.putShort(sizes.size.toShort())

        var offset = 6 + (sizes.size * 16)
        sizes.forEachIndexed { index, size ->
            val payload = payloads[index]
            buf.put(if (size >= 256) 0 else size.toByte())
            buf.put(if (size >= 256) 0 else size.toByte())
            buf.put(0) // Palette count
            buf.put(0) // Reserved
            buf.putShort(1) // Color planes
            buf.putShort(32) // Bits per pixel
            buf.putInt(payload.size)
            buf.putInt(offset)
            offset += payload.size
        }

        payloads.forEach { buf.put(it) }
        return buf.array()
    }

    fun encodeIcns(): ByteArray {
        // Apple ICNS modern PNG chunk types (16x16 through 512x512@2x)
        val entries =
            listOf(
                "icp4" to 16,
                "icp5" to 32,
                "icp6" to 64,
                "ic07" to 128,
                "ic08" to 256,
                "ic09" to 512,
                "ic10" to 1024,
                "ic11" to 32,
                "ic12" to 64,
                "ic13" to 256,
                "ic14" to 512,
            )

        val pngBySize = entries.map { it.second }.distinct().associateWith { RasterRenderer.renderSquircleIconPng(it) }
        val chunks =
            entries.map { (osType, size) ->
                val data = pngBySize.getValue(size)
                osType.toByteArray(Charsets.US_ASCII) to data
            }

        val totalLength = 8 + chunks.sumOf { 8 + it.second.size }
        val buf = ByteBuffer.allocate(totalLength).order(ByteOrder.BIG_ENDIAN)
        buf.put("icns".toByteArray(Charsets.US_ASCII))
        buf.putInt(totalLength)

        chunks.forEach { (typeBytes, data) ->
            buf.put(typeBytes)
            buf.putInt(8 + data.size)
            buf.put(data)
        }
        return buf.array()
    }
}
