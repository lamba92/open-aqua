package io.github.lamba92.openaqua.model.device

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.round

/**
 * A discrete point on a temperature-to-PWM fan curve.
 *
 * @property temp Temperature value in degrees Celsius.
 * @property pwm Fan PWM power percentage (0.0..100.0%).
 */
@Serializable
data class CurvePoint(
    val temp: Double,
    val pwm: Double,
)

/**
 * Parametric specifications for generating a multi-point fan curve.
 *
 * @property tMin Temperature in Celsius at the curve's starting point.
 * @property tMax Temperature in Celsius at the curve's ending point.
 * @property pMin Fan PWM power percentage at [tMin] (0.0..100.0%).
 * @property pMax Fan PWM power percentage at [tMax] (0.0..100.0%).
 * @property shape Curvature shape exponent:
 *                 - `0.0`: Linear interpolation.
 *                 - `> 0.0`: Progressive / exponential response (quieter at low temps).
 *                 - `< 0.0`: Digressive / logarithmic response (aggressive cooling at onset).
 * @property count Total number of discrete points to generate (defaults to [CurveMath.DEFAULT_CURVE_POINTS]).
 */
@Serializable
data class AutoCurveParams(
    val tMin: Double,
    val tMax: Double,
    val pMin: Double,
    val pMax: Double,
    val shape: Double = 0.0,
    val count: Int = CurveMath.DEFAULT_CURVE_POINTS,
) {
    /**
     * Evaluates this parametric curve into a discrete list of [CurvePoint] entries.
     *
     * @return List of generated [CurvePoint] instances.
     */
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

/**
 * Mathematical utilities for evaluating and reverse-engineering Aquasuite-compatible fan curves.
 */
object CurveMath {
    /** Standard number of points in Aquacomputer hardware lookup curves (16 points). */
    const val DEFAULT_CURVE_POINTS = 16

    /**
     * Generates an Aquasuite-compatible multi-point fan curve:
     * - `shape == 0.0`: Linear interpolation (`y = x`)
     * - `shape > 0.0`: Progressive / Exponential (`y = x^(1 + shape)`)
     * - `shape < 0.0`: Digressive / Logarithmic (`y = 1 - (1 - x)^(1 + |shape|)`)
     *
     * @param tMin Starting temperature in degrees Celsius.
     * @param tMax Ending temperature in degrees Celsius.
     * @param pMin Starting PWM power percentage (0..100%).
     * @param pMax Ending PWM power percentage (0..100%).
     * @param shape Curvature exponent (-10.0..10.0).
     * @param count Number of discrete points to compute.
     * @return List of calculated [CurvePoint] objects.
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
     *
     * @param points Ordered list of [CurvePoint] entries.
     * @return Deduced shape parameter rounded to one decimal place.
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
