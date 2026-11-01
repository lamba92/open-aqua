package io.github.lamba92.openaqua.model.device

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.round

@Serializable
data class CurvePoint(
    val temp: Double,
    val pwm: Double,
)

@Serializable
data class AutoCurveParams(
    val tMin: Double,
    val tMax: Double,
    val pMin: Double,
    val pMax: Double,
    val shape: Double = 0.0,
    val count: Int = CurveMath.DEFAULT_CURVE_POINTS,
) {
    fun toPoints(): List<CurvePoint> =
        CurveMath.generateCurve(
            tMin = tMin,
            tMax = tMax,
            pMin = pMin,
            pMax = pMax,
            shape = shape,
            count = count,
        )
}

object CurveMath {
    const val DEFAULT_CURVE_POINTS = 16

    /**
     * Generates an Aquasuite-compatible 16-point fan curve:
     * - shape == 0.0: Linear interpolation (y = x)
     * - shape > 0.0: Progressive / Exponential (y = x^(1 + shape))
     * - shape < 0.0: Digressive / Logarithmic (y = 1 - (1 - x)^(1 + |shape|))
     */
    fun generateCurve(
        tMin: Double,
        tMax: Double,
        pMin: Double,
        pMax: Double,
        shape: Double = 0.0,
        count: Int = DEFAULT_CURVE_POINTS,
    ): List<CurvePoint> {
        val spanT = tMax - tMin
        val spanP = pMax - pMin

        return (0 until count).map { i ->
            val tNorm = if (count > 1) i.toDouble() / (count - 1) else 0.0
            val temp = tMin + tNorm * spanT

            val factor =
                when {
                    abs(shape) < 1e-6 -> tNorm
                    shape > 0.0 -> tNorm.pow(1.0 + shape)
                    else -> 1.0 - (1.0 - tNorm).pow(1.0 + abs(shape))
                }

            val pwm = (pMin + factor * spanP).coerceIn(0.0, 100.0)
            CurvePoint(
                temp = round(temp * 10.0) / 10.0,
                pwm = round(pwm * 10.0) / 10.0,
            )
        }
    }

    /**
     * Deduces the curve shape exponent from a list of points (e.g. 16-point table).
     * Returns 0.0 for linear/flat curves, > 0.0 for progressive curves, and < 0.0 for digressive curves.
     */
    fun inferShape(points: List<CurvePoint>): Double {
        if (points.size < 2) return 0.0
        val pMin = points.first().pwm
        val pMax = points.last().pwm
        val spanP = pMax - pMin
        if (abs(spanP) < 1e-4) return 0.0

        val n = points.size
        val shapes = mutableListOf<Double>()

        for (i in 1 until n - 1) {
            val tNorm = i.toDouble() / (n - 1)
            val y = ((points[i].pwm - pMin) / spanP).coerceIn(0.0, 1.0)
            if (tNorm in 0.001..0.999 && y in 0.001..0.999) {
                val s =
                    when {
                        y < tNorm - 1e-4 -> (ln(y) / ln(tNorm)) - 1.0
                        y > tNorm + 1e-4 -> -((ln(1.0 - y) / ln(1.0 - tNorm)) - 1.0)
                        else -> 0.0
                    }
                shapes.add(s)
            }
        }

        if (shapes.isEmpty()) return 0.0
        shapes.sort()
        val median =
            if (shapes.size % 2 == 1) {
                shapes[shapes.size / 2]
            } else {
                (shapes[shapes.size / 2 - 1] + shapes[shapes.size / 2]) / 2.0
            }
        return round(median * 10.0) / 10.0
    }
}
