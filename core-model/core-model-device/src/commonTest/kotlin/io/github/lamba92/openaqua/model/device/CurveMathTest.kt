package io.github.lamba92.openaqua.model.device

import kotlin.test.Test
import kotlin.test.assertEquals

class CurveMathTest {
    @Test
    fun testLinearCurveShapeInfer() {
        val points =
            CurveMath.generateCurve(
                tMin = 20.0,
                tMax = 50.0,
                pMin = 20.0,
                pMax = 80.0,
                shape = 0.0,
            )
        assertEquals(16, points.size)
        val inferred = CurveMath.inferShape(points)
        assertEquals(0.0, inferred, 0.1)
    }

    @Test
    fun testProgressiveCurveShapeInfer() {
        val shapesToTest = listOf(0.5, 1.0, 2.0, 5.0, 7.0)
        for (shape in shapesToTest) {
            val points =
                CurveMath.generateCurve(
                    tMin = 30.0,
                    tMax = 60.0,
                    pMin = 25.0,
                    pMax = 100.0,
                    shape = shape,
                )
            val inferred = CurveMath.inferShape(points)
            assertEquals(shape, inferred, 0.2, "Inferred shape for $shape should match")
        }
    }

    @Test
    fun testDigressiveCurveShapeInfer() {
        val shapesToTest = listOf(-0.5, -1.0, -2.0, -4.0)
        for (shape in shapesToTest) {
            val points =
                CurveMath.generateCurve(
                    tMin = 25.0,
                    tMax = 55.0,
                    pMin = 20.0,
                    pMax = 80.0,
                    shape = shape,
                )
            val inferred = CurveMath.inferShape(points)
            assertEquals(shape, inferred, 0.2, "Inferred shape for $shape should match")
        }
    }

    @Test
    fun testFlatAndDegenerateCurves() {
        assertEquals(0.0, CurveMath.inferShape(emptyList()))
        assertEquals(0.0, CurveMath.inferShape(listOf(CurvePoint(20.0, 50.0))))
        val flatPoints = (0 until 16).map { CurvePoint(20.0 + it, 50.0) }
        assertEquals(0.0, CurveMath.inferShape(flatPoints))
    }
}
