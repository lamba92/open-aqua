package io.github.lamba92.openaqua.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lamba92.openaqua.app.theme.CyanBright
import io.github.lamba92.openaqua.app.theme.ElectricCyan
import io.github.lamba92.openaqua.app.theme.StatusNominal
import io.github.lamba92.openaqua.app.theme.SurfaceContainerHigh
import io.github.lamba92.openaqua.app.theme.SurfaceContainerLowest
import io.github.lamba92.openaqua.model.DeviceType

@Composable
fun DeviceIllustration(
    type: DeviceType,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .background(SurfaceContainerLowest, RoundedCornerShape(16.dp))
                .border(1.dp, SurfaceContainerHigh, RoundedCornerShape(16.dp))
                .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (type) {
                DeviceType.OCTO -> drawOctoBoard()
                DeviceType.QUADRO -> drawQuadroBoard()
                DeviceType.D5_NEXT -> drawD5NextPump()
                else -> drawGenericBoard()
            }
        }
        // Overlay label in bottom corner
        Text(
            text = type.name,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
            color = CyanBright.copy(alpha = 0.6f),
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

private fun DrawScope.drawOctoBoard() {
    val pcbWidth = size.width * 0.9f
    val pcbHeight = size.height * 0.85f
    val left = (size.width - pcbWidth) / 2f
    val top = (size.height - pcbHeight) / 2f

    // PCB Body (Matte dark obsidian)
    drawRoundRect(
        color = Color(0xFF141923),
        topLeft = Offset(left, top),
        size = Size(pcbWidth, pcbHeight),
        cornerRadius = CornerRadius(14f, 14f),
    )
    drawRoundRect(
        color = Color(0xFF263238),
        topLeft = Offset(left, top),
        size = Size(pcbWidth, pcbHeight),
        cornerRadius = CornerRadius(14f, 14f),
        style = Stroke(width = 2f),
    )

    // Mounting holes
    val holeRadius = 7f
    val holeMargin = 16f
    listOf(
        Offset(left + holeMargin, top + holeMargin),
        Offset(left + pcbWidth - holeMargin, top + holeMargin),
        Offset(left + holeMargin, top + pcbHeight - holeMargin),
        Offset(left + pcbWidth - holeMargin, top + pcbHeight - holeMargin),
    ).forEach { pos ->
        drawCircle(Color(0xFF0A0D14), radius = holeRadius, center = pos)
        drawCircle(Color(0xFFD4AF37), radius = holeRadius + 3f, center = pos, style = Stroke(2f))
    }

    // Circuit traces (cyan accent)
    drawLine(
        color = ElectricCyan.copy(alpha = 0.25f),
        start = Offset(left + 30f, top + 60f),
        end = Offset(left + pcbWidth - 30f, top + 60f),
        strokeWidth = 2f,
    )
    drawLine(
        color = ElectricCyan.copy(alpha = 0.25f),
        start = Offset(left + 60f, top + pcbHeight - 60f),
        end = Offset(left + pcbWidth - 60f, top + pcbHeight - 60f),
        strokeWidth = 2f,
    )

    // Center Aquacomputer Aluminum Heatsink / Shield
    val heatsinkW = pcbWidth * 0.45f
    val heatsinkH = pcbHeight * 0.40f
    val heatsinkX = left + (pcbWidth - heatsinkW) / 2f
    val heatsinkY = top + (pcbHeight - heatsinkH) / 2f
    drawRoundRect(
        brush =
            Brush.linearGradient(
                colors = listOf(Color(0xFF2B3342), Color(0xFF1C222D)),
                start = Offset(heatsinkX, heatsinkY),
                end = Offset(heatsinkX + heatsinkW, heatsinkY + heatsinkH),
            ),
        topLeft = Offset(heatsinkX, heatsinkY),
        size = Size(heatsinkW, heatsinkH),
        cornerRadius = CornerRadius(8f, 8f),
    )
    drawRoundRect(
        color = ElectricCyan.copy(alpha = 0.4f),
        topLeft = Offset(heatsinkX, heatsinkY),
        size = Size(heatsinkW, heatsinkH),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(1.5f),
    )

    // Heatsink Fin slots
    for (i in 1..4) {
        val lineY = heatsinkY + (heatsinkH / 5f) * i
        drawLine(
            color = Color(0xFF141923),
            start = Offset(heatsinkX + 15f, lineY),
            end = Offset(heatsinkX + heatsinkW - 15f, lineY),
            strokeWidth = 3f,
        )
    }

    // 8 Fan headers (4 along the top, 4 along the bottom)
    val fanHeaderW = (pcbWidth - 80f) / 4f
    for (i in 0..3) {
        val hX = left + 40f + i * fanHeaderW
        // Top fan headers
        drawRoundRect(
            color = Color(0xFF090D14),
            topLeft = Offset(hX + 4f, top + 10f),
            size = Size(fanHeaderW - 12f, 22f),
            cornerRadius = CornerRadius(3f, 3f),
        )
        // 4 pins each
        for (p in 0..3) {
            drawCircle(
                color = Color(0xFFFFD700),
                radius = 2.5f,
                center = Offset(hX + 12f + p * 6f, top + 21f),
            )
        }

        // Bottom fan headers
        drawRoundRect(
            color = Color(0xFF090D14),
            topLeft = Offset(hX + 4f, top + pcbHeight - 32f),
            size = Size(fanHeaderW - 12f, 22f),
            cornerRadius = CornerRadius(3f, 3f),
        )
        for (p in 0..3) {
            drawCircle(
                color = Color(0xFFFFD700),
                radius = 2.5f,
                center = Offset(hX + 12f + p * 6f, top + pcbHeight - 21f),
            )
        }
    }

    // Status LED (Pulsing Green)
    drawCircle(
        color = StatusNominal,
        radius = 5f,
        center = Offset(left + 25f, top + pcbHeight / 2f),
    )
    drawCircle(
        color = StatusNominal.copy(alpha = 0.35f),
        radius = 10f,
        center = Offset(left + 25f, top + pcbHeight / 2f),
    )
}

private fun DrawScope.drawQuadroBoard() {
    val pcbWidth = size.width * 0.85f
    val pcbHeight = size.height * 0.75f
    val left = (size.width - pcbWidth) / 2f
    val top = (size.height - pcbHeight) / 2f

    drawRoundRect(
        color = Color(0xFF141923),
        topLeft = Offset(left, top),
        size = Size(pcbWidth, pcbHeight),
        cornerRadius = CornerRadius(12f, 12f),
    )
    drawRoundRect(
        color = Color(0xFF263238),
        topLeft = Offset(left, top),
        size = Size(pcbWidth, pcbHeight),
        cornerRadius = CornerRadius(12f, 12f),
        style = Stroke(2f),
    )

    // 4 Fan headers along the top
    val fanHeaderW = (pcbWidth - 60f) / 4f
    for (i in 0..3) {
        val hX = left + 30f + i * fanHeaderW
        drawRoundRect(
            color = Color(0xFF090D14),
            topLeft = Offset(hX + 4f, top + 10f),
            size = Size(fanHeaderW - 8f, 22f),
            cornerRadius = CornerRadius(3f, 3f),
        )
        for (p in 0..3) {
            drawCircle(
                color = Color(0xFFFFD700),
                radius = 2.5f,
                center = Offset(hX + 10f + p * 6f, top + 21f),
            )
        }
    }

    // Center chip
    val chipSize = 50f
    val chipX = left + (pcbWidth - chipSize) / 2f
    val chipY = top + (pcbHeight - chipSize) / 2f + 10f
    drawRect(
        color = Color(0xFF1E2430),
        topLeft = Offset(chipX, chipY),
        size = Size(chipSize, chipSize),
    )
    drawRect(
        color = ElectricCyan.copy(alpha = 0.4f),
        topLeft = Offset(chipX, chipY),
        size = Size(chipSize, chipSize),
        style = Stroke(1.5f),
    )

    // Status LED
    drawCircle(
        color = StatusNominal,
        radius = 5f,
        center = Offset(left + 20f, top + pcbHeight / 2f),
    )
}

private fun DrawScope.drawD5NextPump() {
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val radius = minOf(size.width, size.height) * 0.38f

    // Outer pump body ring
    drawCircle(
        color = Color(0xFF141923),
        radius = radius,
        center = Offset(centerX, centerY),
    )
    drawCircle(
        color = Color(0xFF2B3342),
        radius = radius,
        center = Offset(centerX, centerY),
        style = Stroke(4f),
    )

    // RGB Ring glow
    drawCircle(
        brush =
            Brush.sweepGradient(
                colors = listOf(CyanBright, ElectricCyan, Color(0xFF04B4A2), CyanBright),
                center = Offset(centerX, centerY),
            ),
        radius = radius * 0.88f,
        center = Offset(centerX, centerY),
        style = Stroke(5f),
    )

    // Inner OLED display screen
    val oledW = radius * 1.0f
    val oledH = radius * 0.65f
    drawRoundRect(
        color = Color(0xFF080C12),
        topLeft = Offset(centerX - oledW / 2f, centerY - oledH / 2f),
        size = Size(oledW, oledH),
        cornerRadius = CornerRadius(6f, 6f),
    )
    drawRoundRect(
        color = ElectricCyan.copy(alpha = 0.5f),
        topLeft = Offset(centerX - oledW / 2f, centerY - oledH / 2f),
        size = Size(oledW, oledH),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(1.5f),
    )

    // Display text lines simulation
    drawLine(
        color = CyanBright,
        start = Offset(centerX - oledW * 0.3f, centerY - 6f),
        end = Offset(centerX + oledW * 0.3f, centerY - 6f),
        strokeWidth = 3f,
    )
    drawLine(
        color = ElectricCyan.copy(alpha = 0.7f),
        start = Offset(centerX - oledW * 0.2f, centerY + 8f),
        end = Offset(centerX + oledW * 0.2f, centerY + 8f),
        strokeWidth = 2f,
    )
}

private fun DrawScope.drawGenericBoard() {
    val pcbWidth = size.width * 0.8f
    val pcbHeight = size.height * 0.8f
    val left = (size.width - pcbWidth) / 2f
    val top = (size.height - pcbHeight) / 2f

    drawRoundRect(
        color = Color(0xFF141923),
        topLeft = Offset(left, top),
        size = Size(pcbWidth, pcbHeight),
        cornerRadius = CornerRadius(12f, 12f),
    )
    drawRoundRect(
        color = ElectricCyan.copy(alpha = 0.3f),
        topLeft = Offset(left, top),
        size = Size(pcbWidth, pcbHeight),
        cornerRadius = CornerRadius(12f, 12f),
        style = Stroke(2f),
    )
}
