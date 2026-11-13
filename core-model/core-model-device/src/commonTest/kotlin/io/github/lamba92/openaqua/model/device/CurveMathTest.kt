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

    @Test
    fun testAdjustTMinAndTMax() {
        // Normal adjustment: tMin < tMax, neither shifts the other
        val (tMin1, tMax1) = CurveMath.adjustTMin(30.0, 60.0)
        assertEquals(30.0, tMin1)
        assertEquals(60.0, tMax1)

        // tMin pushed up past tMax -> shifts tMax
        val (tMin2, tMax2) = CurveMath.adjustTMin(65.0, 60.0)
        assertEquals(65.0, tMin2)
        assertEquals(66.0, tMax2)

        // tMax clamped to maxAllowed
        val (tMin3, tMax3) = CurveMath.adjustTMin(95.0, 60.0, maxAllowed = 95.0)
        assertEquals(95.0, tMin3)
        assertEquals(95.0, tMax3)

        // tMax pushed down past tMin -> shifts tMin
        val (tMin4, tMax4) = CurveMath.adjustTMax(25.0, 40.0)
        assertEquals(24.0, tMin4)
        assertEquals(25.0, tMax4)

        // tMin clamped to minAllowed
        val (tMin5, tMax5) = CurveMath.adjustTMax(10.0, 40.0, minAllowed = 10.0)
        assertEquals(10.0, tMin5)
        assertEquals(10.0, tMax5)
    }
}
