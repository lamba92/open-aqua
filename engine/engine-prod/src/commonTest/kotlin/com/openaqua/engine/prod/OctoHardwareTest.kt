package com.openaqua.engine.prod

import com.openaqua.model.AutoCurveParams
import com.openaqua.model.CurveFanConfig
import com.openaqua.model.DeviceType
import com.openaqua.model.FanChannelConfig
import com.openaqua.model.FixedFanConfig
import com.openaqua.model.FlashOctoConfig
import com.openaqua.model.OctoAllowedSpecs
import com.openaqua.model.OctoConfig
import com.openaqua.model.OctoTelemetry
import com.openaqua.model.TargetTempFanConfig
import com.openaqua.model.TwoPointFanConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end hardware tests tailored specifically for lamba92's workstation environment
 * featuring a physical Aquacomputer OCTO controller with:
 *  - Fans 1-4 actively connected/occupied (channels 0..3) -> Protected from writing
 *  - Sensor 1 actively connected/occupied (channel 0) -> Protected from offset modification
 *  - Fans 5-8 unoccupied (channels 4..7) -> Allowed for flash tests
 *  - Virtual Sensors 5-19 -> Allowed for feeding tests
 *
 * NOTE: These tests interact with real physical USB hardware and are excluded by default
 * from `./gradlew check` and `./gradlew test`. To run them explicitly, pass `-Ptest.hardware=true`
 * (e.g. `./gradlew.bat :engine:engine-prod:mingwX64Test -Ptest.hardware=true` or `./gradlew :engine:engine-prod:linuxX64Test -Ptest.hardware=true`).
 */
class OctoHardwareTest {
    private val controller = getPlatformHardwareController()

    /**
     * Declares which components on lamba92's OCTO are allowed to be modified during tests.
     * Fans 1-4 (channels 0..3) and Sensor 1 (channel 0) are omitted/false to protect active hardware.
     */
    private val allowedSpecs =
        OctoAllowedSpecs(
            fans =
                mapOf(
                    4 to true, // Fan 5 (unoccupied)
                    5 to true, // Fan 6 (unoccupied)
                    6 to true, // Fan 7 (unoccupied)
                    7 to true, // Fan 8 (unoccupied)
                ),
            sensorOffsets =
                mapOf(
                    1 to true, // Sensor 2 (unoccupied)
                    2 to true, // Sensor 3 (unoccupied)
                    3 to true, // Sensor 4 (unoccupied)
                ),
            virtualSensors =
                (4..19).associateWith { true },
        )

    @Test
    fun testOctoDetection() {
        val devices = controller.scanDevices()
        val octo = devices.firstOrNull { it.type == DeviceType.OCTO }
        assertNotNull(octo, "Aquacomputer OCTO must be detected")
        println("Detected OCTO: id=${octo.id}, serial=${octo.serial}, fw=${octo.firmwareVersion}")

        assertNotNull(octo.serial, "OCTO serial number string should be readable")
        val fwVersion = octo.firmwareVersion
        assertNotNull(fwVersion, "OCTO firmware version should be readable")
        assertTrue(fwVersion > 0, "OCTO firmware version should be a positive integer")
    }

    @Test
    fun testTelemetryReadsOccupiedFansAndSensors() =
        runBlocking {
            val octo =
                controller.scanDevices().firstOrNull { it.type == DeviceType.OCTO }
                    ?: error("OCTO not found")

            val telemetry = controller.streamTelemetry(octo).first()
            assertTrue(telemetry is OctoTelemetry, "Telemetry must be OctoTelemetry")

            println("Telemetry received with ${telemetry.fans.size} fans and ${telemetry.sensors.size} sensors")

            // Fans 1-4 are occupied (indices 0..3)
            for (channel in 0..3) {
                val fan = telemetry.fans.firstOrNull { it.channel == channel }
                assertNotNull(fan, "Fan ${channel + 1} (channel $channel) must be present in telemetry")
                println(
                    "Fan ${channel + 1}: rpm=${fan.rpm}, voltage=${fan.voltageVolts}V, " +
                        "current=${fan.currentAmps}A, power=${fan.powerWatts}W",
                )
                assertTrue(fan.voltageVolts > 0.0, "Fan ${channel + 1} voltage should be > 0V")
            }

            // Sensor 1 is occupied (channel 0)
            val sensor1 = telemetry.sensors.firstOrNull { it.channel == 0 }
            assertNotNull(sensor1, "Sensor 1 (channel 0) must be present in telemetry")
            val temp = sensor1.tempCelsius
            assertNotNull(temp, "Sensor 1 temperature must not be null")
            println("Sensor 1 temp: $temp°C")
            assertTrue(
                temp in 5.0..80.0,
                "Sensor 1 temperature ($temp°C) should be in realistic range",
            )
        }

    @Test
    fun testFlashFixedPowerOnAllowedFans() {
        val octo =
            controller.scanDevices().firstOrNull { it.type == DeviceType.OCTO }
                ?: error("OCTO not found")

        val initialConfig = controller.readConfig(octo) as OctoConfig
        val testChannel = 4 // Fan 5
        require(allowedSpecs.isFanAllowed(testChannel)) {
            "Safety check failed: Channel $testChannel is not allowed in AllowedSpecs"
        }

        val originalConfig = initialConfig.fans[testChannel]
        try {
            val testTargetPower = 35.0
            val testFixedConfig = FixedFanConfig(channel = testChannel, powerPercent = testTargetPower)
            val testRequest = FlashOctoConfig(fans = mapOf(testChannel to testFixedConfig))

            println("Testing Fixed Power on allowed Fan ${testChannel + 1} ($testTargetPower%)...")
            val readback = controller.flashConfig(octo, testRequest) as OctoConfig
            val updated = readback.fans[testChannel]
            assertTrue(updated is FixedFanConfig)
            assertEquals(testTargetPower, updated.powerPercent, 0.5)
            println("Verified Fixed Power flashed and read back successfully")
        } finally {
            restoreFanConfig(octo, testChannel, originalConfig)
        }
    }

    @Test
    fun testFlashCurveOnAllowedFan() {
        val octo =
            controller.scanDevices().firstOrNull { it.type == DeviceType.OCTO }
                ?: error("OCTO not found")

        val initialConfig = controller.readConfig(octo) as OctoConfig
        val testChannel = 5 // Fan 6
        require(allowedSpecs.isFanAllowed(testChannel)) {
            "Safety check failed: Channel $testChannel is not allowed in AllowedSpecs"
        }

        val originalConfig = initialConfig.fans[testChannel]
        try {
            val curvePoints =
                AutoCurveParams(
                    tMin = 20.0,
                    tMax = 50.0,
                    pMin = 30.0,
                    pMax = 90.0,
                    shape = 0.5,
                ).toPoints()

            val testCurveConfig =
                CurveFanConfig(
                    channel = testChannel,
                    sensorIndex = 0, // Bound to Sensor 1 for reference
                    points = curvePoints,
                )
            val testRequest = FlashOctoConfig(fans = mapOf(testChannel to testCurveConfig))

            println("Testing 16-point Curve on allowed Fan ${testChannel + 1}...")
            val readback = controller.flashConfig(octo, testRequest) as OctoConfig
            val updated = readback.fans[testChannel]
            assertTrue(updated is CurveFanConfig)
            assertEquals(0, updated.sensorIndex)
            assertEquals(16, updated.points.size)
            assertEquals(30.0, updated.points.first().pwm, 0.5)
            assertEquals(90.0, updated.points.last().pwm, 0.5)
            println("Verified 16-point Curve flashed and read back successfully")
        } finally {
            restoreFanConfig(octo, testChannel, originalConfig)
        }
    }

    @Test
    fun testFlashSensorOffsetOnAllowedSensor() {
        val octo =
            controller.scanDevices().firstOrNull { it.type == DeviceType.OCTO }
                ?: error("OCTO not found")

        val initialConfig = controller.readConfig(octo) as OctoConfig
        val testSensorChannel = 1 // Sensor 2 (unoccupied)
        require(allowedSpecs.isSensorOffsetAllowed(testSensorChannel)) {
            "Safety check failed: Sensor channel $testSensorChannel is not allowed in AllowedSpecs"
        }

        val originalOffset = initialConfig.sensorOffsets.firstOrNull { it.channel == testSensorChannel }?.offsetCelsius ?: 0.0
        try {
            val testOffset = 2.5
            println("Testing Sensor Offset on allowed Sensor ${testSensorChannel + 1} ($testOffset°C)...")
            val testRequest = FlashOctoConfig(sensorOffsets = mapOf(testSensorChannel to testOffset))
            val readback = controller.flashConfig(octo, testRequest) as OctoConfig
            val updated = readback.sensorOffsets.first { it.channel == testSensorChannel }
            assertEquals(testOffset, updated.offsetCelsius, 0.2)
            println("Verified Sensor Offset flashed and read back successfully")
        } finally {
            println("Restoring Sensor ${testSensorChannel + 1} offset to $originalOffset°C...")
            controller.flashConfig(octo, FlashOctoConfig(sensorOffsets = mapOf(testSensorChannel to originalOffset)))
        }
    }

    @Test
    fun testFeedVirtualSensorsOnAllowedChannels() {
        val octo =
            controller.scanDevices().firstOrNull { it.type == DeviceType.OCTO }
                ?: error("OCTO not found")

        val testChannel = 5 // Virtual sensor slot 2
        require(allowedSpecs.isVirtualSensorAllowed(testChannel)) {
            "Safety check failed: Virtual sensor channel $testChannel is not allowed in AllowedSpecs"
        }

        val testTemp = 28.5
        println("Feeding virtual sensor channel $testChannel with $testTemp°C...")
        controller.feedVirtualSensors(octo, mapOf(testChannel to testTemp))
        println("Virtual sensor packet sent successfully")
    }

    private fun restoreFanConfig(
        octo: com.openaqua.model.DetectedHardwareDevice,
        channel: Int,
        config: FanChannelConfig,
    ) {
        println("Restoring Fan ${channel + 1} to original config...")
        val restoreRequest =
            FlashOctoConfig(
                fans = mapOf(channel to config.toSafeRestorableConfig(channel)),
            )
        controller.flashConfig(octo, restoreRequest)
        val restored = (controller.readConfig(octo) as OctoConfig).fans[channel]
        println("Restored Fan ${channel + 1}: $restored")
    }

    private fun FanChannelConfig.toSafeRestorableConfig(channel: Int): FanChannelConfig =
        when (this) {
            is FixedFanConfig -> this
            is CurveFanConfig -> this
            is TargetTempFanConfig -> this
            is TwoPointFanConfig -> this
            else -> FixedFanConfig(channel = channel, powerPercent = 50.0)
        }
}
