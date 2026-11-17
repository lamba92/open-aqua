package io.github.lamba92.openaqua.app.model

import androidx.compose.ui.graphics.Color
import io.github.lamba92.openaqua.app.ui.components.lerpColor
import kotlinx.serialization.Serializable

@Serializable
data class TemperatureThresholds(
    val cool: Double,
    val nominal: Double,
    val warm: Double,
    val hot: Double,
) {
    companion object {
        val DIE =
            TemperatureThresholds(
                cool = 40.0,
                nominal = 60.0,
                warm = 75.0,
                hot = 90.0,
            )

        val WATER =
            TemperatureThresholds(
                cool = 20.0,
                nominal = 30.0,
                warm = 42.0,
                hot = 50.0,
            )
    }
}

@Serializable
enum class SensorTargetType {
    DIE,
    WATER,
}

val SensorTargetType.thresholds: TemperatureThresholds
    get() =
        when (this) {
            SensorTargetType.DIE -> TemperatureThresholds.DIE
            SensorTargetType.WATER -> TemperatureThresholds.WATER
        }

fun SensorTargetType.color(tempCelsius: Double?): Color = thresholds.color(tempCelsius)

fun SensorTargetType.progressFraction(tempCelsius: Double?): Float = thresholds.progressFraction(tempCelsius)

fun TemperatureThresholds.progressFraction(tempCelsius: Double?): Float {
    if (tempCelsius == null) return 0f
    return ((tempCelsius - cool) / (hot - cool)).toFloat().coerceIn(0f, 1f)
}

fun TemperatureThresholds.color(tempCelsius: Double?): Color {
    if (tempCelsius == null) return Color(0xFF0288D1)
    return when {
        tempCelsius <= cool -> {
            Color(0xFF0288D1)
        }

        tempCelsius <= nominal -> {
            val f = ((tempCelsius - cool) / (nominal - cool)).toFloat().coerceIn(0f, 1f)
            if (f < 0.5f) {
                lerpColor(Color(0xFF0288D1), Color(0xFF00B4D8), f * 2f)
            } else {
                lerpColor(Color(0xFF00B4D8), Color(0xFF10B981), (f - 0.5f) * 2f)
            }
        }

        tempCelsius <= warm -> {
            val f = ((tempCelsius - nominal) / (warm - nominal)).toFloat().coerceIn(0f, 1f)
            lerpColor(Color(0xFF10B981), Color(0xFFFFB703), f)
        }

        tempCelsius <= hot -> {
            val f = ((tempCelsius - warm) / (hot - warm)).toFloat().coerceIn(0f, 1f)
            lerpColor(Color(0xFFFFB703), Color(0xFFE63946), f)
        }

        else -> {
            Color(0xFFE63946)
        }
    }
}
