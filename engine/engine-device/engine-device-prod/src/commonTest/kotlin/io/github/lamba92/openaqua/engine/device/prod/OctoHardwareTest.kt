package io.github.lamba92.openaqua.engine.device.prod

import io.github.lamba92.openaqua.engine.device.hardware.HardwareController
import io.github.lamba92.openaqua.model.device.AutoCurveFanConfig
import io.github.lamba92.openaqua.model.device.AutoCurveParams
import io.github.lamba92.openaqua.model.device.CoolingChannelTelemetry
import io.github.lamba92.openaqua.model.device.CurveFanConfig
import io.github.lamba92.openaqua.model.device.DetectedHardwareDevice
import io.github.lamba92.openaqua.model.device.DeviceConfig
import io.github.lamba92.openaqua.model.device.DeviceType
import io.github.lamba92.openaqua.model.device.FanChannelConfig
import io.github.lamba92.openaqua.model.device.FanLimits
import io.github.lamba92.openaqua.model.device.FixedFanConfig
import io.github.lamba92.openaqua.model.device.FlashConfigRequest
import io.github.lamba92.openaqua.model.device.LimitsOnlyFanConfig
import io.github.lamba92.openaqua.model.device.SensorTelemetry
import io.github.lamba92.openaqua.model.device.TargetTempFanConfig
import io.github.lamba92.openaqua.model.device.TwoPointFanConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end hardware tests tailored specifically for lamba92's workstation environment
 * featuring a physical Aquacomputer OCTO controller with:
 *  - Fans 1-4 actively connected/occupied (channels 0..3) -> Protected from writing
 *  - Sensor 1 actively connected/occupied (channel 0) -> Protected from offset modification
 *  - Fans 5-8 unoccupied (channels 4..7) -> Allowed for flash tests
 *  - Virtual Sensors 5-19 -> Allowed for feeding tests
 *  - RGBpx: Flashing is not yet tested here as report byte offsets and encoding for RGBpx
 *    hardware controller memory are pending implementation in ConfigWriter.
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
    private val lamba92sOctoAllowedSpec =
        DeviceAllowedSpecs(
            fans =
                mapOf(
                    0 to false,
                    1 to false,
                    2 to false,
                    3 to false,
                    4 to true, // Fan 5 (unoccupied)
                    5 to true, // Fan 6 (unoccupied)
                    6 to true, // Fan 7 (unoccupied)
                    7 to true, // Fan 8 (unoccupied)
                ),
            sensorOffsets =
                mapOf(
                    0 to false,
                    1 to true, // Sensor 2 (unoccupied)
                    2 to true, // Sensor 3 (unoccupied)
                    3 to true, // Sensor 4 (unoccupied)
                ),
            virtualSensors = (0..19).associateWith { true },
            // RGBpx ports 0..1 will be enabled here once RGBpx encoding is added to ConfigWriter
            rgbpx = emptyMap(),
        )

    private class OctoTestScope(
        val controller: HardwareController,
        val octo: DetectedHardwareDevice,
        val allowedSpecs: DeviceAllowedSpecs,
    ) {
        fun flashAndVerify(request: FlashConfigRequest): DeviceConfig {
            request.fans?.keys?.forEach { channel ->
                require(allowedSpecs.isFanAllowed(channel)) {
                    "Safety check failed: Fan channel $channel is not allowed in AllowedSpecs"
                }
            }
            request.sensorOffsets?.keys?.forEach { channel ->
                require(allowedSpecs.isSensorOffsetAllowed(channel)) {
                    "Safety check failed: Sensor channel $channel is not allowed in AllowedSpecs"
                }
            }
            return controller.flashConfig(octo, request)
        }

        fun feedVirtualSensors(sensors: Map<Int, Double>) {
            sensors.keys.forEach { channel ->
                require(allowedSpecs.isVirtualSensorAllowed(channel)) {
                    "Safety check failed: Virtual sensor channel $channel is not allowed in AllowedSpecs"
                }
            }
            controller.feedVirtualSensors(octo, sensors)
        }
    }

    private fun withOctoSafeScope(block: OctoTestScope.() -> Unit) {
        val octo =
            controller.scanDevices().firstOrNull { it.type == DeviceType.OCTO }
                ?: error("OCTO not found")
        val initialConfig = controller.readConfig(octo)
        val scope = OctoTestScope(controller, octo, lamba92sOctoAllowedSpec)

        try {
            scope.block()
        } finally {
            println("Safety Scope: Restoring initial OCTO configuration for allowed test channels...")
            val restoreRequest =
                FlashConfigRequest(
                    fans =
                        initialConfig.fans
                            .filter { lamba92sOctoAllowedSpec.isFanAllowed(it.channel) }
                            .associate { it.channel to it.toSafeRestorableConfig() },
                    sensorOffsets =
                        initialConfig.sensorOffsets
                            .filter { lamba92sOctoAllowedSpec.isSensorOffsetAllowed(it.channel) }
                            .associate { it.channel to it.offsetCelsius },
                )
            controller.flashConfig(octo, restoreRequest)
            println("Safety Scope: Successfully restored original OCTO configuration")
        }
    }

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

            println("Telemetry received with ${telemetry.fans.size} fans and ${telemetry.sensors.size} sensors")

            // Fans 1-4 are occupied (indices 0..3)
            for (channel in 0..3) {
                val fan = telemetry.fans.firstOrNull { it.channel == channel }
                assertNotNull(fan, "Fan ${channel + 1} (channel $channel) must be present in telemetry")
                assertTrue(fan is CoolingChannelTelemetry.Connected, "Fan ${channel + 1} must be connected")
                println(
                    "Fan ${channel + 1}: rpm=${fan.rpm}, pwm=${fan.pwmPercent}%, voltage=${fan.voltageVolts}V, " +
                        "current=${fan.currentAmps}A, power=${fan.powerWatts}W",
                )
                assertTrue(fan.voltageVolts > 0.0, "Fan ${channel + 1} voltage should be > 0V")
            }

            val currentConfig = controller.readConfig(octo)
            println("Sensor Offsets: ${currentConfig.sensorOffsets}")
            val fan4 = telemetry.fans.firstOrNull { it.channel == 3 }
            if (fan4 is CoolingChannelTelemetry.Connected) {
                println("Fan 4: pwm=${fan4.pwmPercent}%, rpm=${fan4.rpm}")
            }
            for (s in telemetry.sensors) {
                if (s is SensorTelemetry.Connected) {
                    println("Sensor ${s.channel}: ${s.tempCelsius}°C (${s.kind})")
                }
            }

            // Sensor 1 is occupied (channel 0)
            val sensor1 = telemetry.sensors.firstOrNull { it.channel == 0 }
            assertNotNull(sensor1, "Sensor 1 (channel 0) must be present in telemetry")
            assertTrue(sensor1 is SensorTelemetry.Connected, "Sensor 1 must be connected")
            val temp = sensor1.tempCelsius
            println("Sensor 1 temp: $temp°C")
            assertTrue(
                temp in 5.0..80.0,
                "Sensor 1 temperature ($temp°C) should be in realistic range",
            )
        }

    @Test
    fun testFlashFixedPowerOnAllowedFans() =
        withOctoSafeScope {
            val testChannel = 4 // Fan 5
            val testTargetPower = 35.0
            val testFixedConfig = FixedFanConfig(channel = testChannel, powerPercent = testTargetPower)
            val testRequest = FlashConfigRequest(fans = mapOf(testChannel to testFixedConfig))

            println("Testing Fixed Power on allowed Fan ${testChannel + 1} ($testTargetPower%)...")
            val readback = flashAndVerify(testRequest)
            val updated = readback.fans[testChannel]
            assertTrue(updated is FixedFanConfig)
            assertEquals(testTargetPower, updated.powerPercent, 0.5)
            println("Verified Fixed Power flashed and read back successfully")
        }

    @Test
    fun testFlashCurveOnAllowedFan() =
        withOctoSafeScope {
            val testChannel = 5 // Fan 6
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
            val testRequest = FlashConfigRequest(fans = mapOf(testChannel to testCurveConfig))

            println("Testing 16-point Curve on allowed Fan ${testChannel + 1}...")
            val readback = flashAndVerify(testRequest)
            val updated = readback.fans[testChannel]
            assertTrue(updated is CurveFanConfig)
            assertEquals(0, updated.sensorIndex)
            assertEquals(16, updated.points.size)
            assertEquals(30.0, updated.points.first().pwm, 0.5)
            assertEquals(90.0, updated.points.last().pwm, 0.5)
            println("Verified 16-point Curve flashed and read back successfully")
        }

    @Test
    fun testFlashAutoCurveOnAllowedFan() =
        withOctoSafeScope {
            val testChannel = 6 // Fan 7
            val testAutoCurve =
                AutoCurveFanConfig(
                    channel = testChannel,
                    sensorIndex = 0,
                    tMin = 25.0,
                    tMax = 45.0,
                    pMin = 20.0,
                    pMax = 80.0,
                    shape = 7.0,
                )
            val testRequest = FlashConfigRequest(fans = mapOf(testChannel to testAutoCurve))

            println("Testing AutoCurveFanConfig with shape=7.0 on allowed Fan ${testChannel + 1}...")
            val readback = flashAndVerify(testRequest)
            val updated = readback.fans[testChannel]
            assertTrue(updated is CurveFanConfig)
            assertEquals(0, updated.sensorIndex)
            assertEquals(16, updated.points.size)
            assertEquals(20.0, updated.points.first().pwm, 0.5)
            assertEquals(80.0, updated.points.last().pwm, 0.5)
            println("Verified AutoCurveFanConfig flashed and verified successfully")
        }

    @Test
    fun testFlashSensorOffsetOnAllowedSensor() =
        withOctoSafeScope {
            val testSensorChannel = 1 // Sensor 2 (unoccupied)
            val testOffset = 2.5
            println("Testing Sensor Offset on allowed Sensor ${testSensorChannel + 1} ($testOffset°C)...")
            val testRequest = FlashConfigRequest(sensorOffsets = mapOf(testSensorChannel to testOffset))
            val readback = flashAndVerify(testRequest)
            val updated = readback.sensorOffsets.first { it.channel == testSensorChannel }
            assertEquals(testOffset, updated.offsetCelsius, 0.2)
            println("Verified Sensor Offset flashed and read back successfully")
        }

    @Test
    fun testFeedVirtualSensorsOnAllowedChannels() =
        withOctoSafeScope {
            val testChannel = 5 // Virtual sensor slot 1 (channel 5)
            for (testTemp in listOf(28.5, 31.2, 34.0)) {
                println("Feeding virtual sensor channel $testChannel with $testTemp°C...")
                feedVirtualSensors(mapOf(testChannel to testTemp))
                val telemetry = runBlocking { controller.streamTelemetry(octo).first() }
                val sensor = telemetry.sensors.firstOrNull { it.channel == testChannel }
                assertNotNull(sensor, "Virtual sensor channel $testChannel must be present in telemetry")
                assertTrue(sensor is SensorTelemetry.Connected, "Virtual sensor channel $testChannel must be connected")
                assertEquals(
                    expected = testTemp,
                    actual = sensor.tempCelsius,
                    absoluteTolerance = 0.2,
                    message = "Virtual sensor channel $testChannel readback temperature should match fed value",
                )
            }
            println("Virtual sensor packets verified successfully on real hardware")
        }

    @Test
    fun testFlashFanLimitsOnAllowedFan() =
        withOctoSafeScope {
            val testChannel = 4 // Fan 5 (unoccupied)
            val testLimits =
                FanLimits(
                    minPowerPercent = 10.0,
                    maxPowerPercent = 95.0,
                    startBoost = true,
                    holdMinPower = false,
                    fallbackPowerPercent = 80.0,
                )
            val testRequest =
                FlashConfigRequest(
                    fans = mapOf(testChannel to LimitsOnlyFanConfig(channel = testChannel, limits = testLimits)),
                )

            println("Testing Fan Limits on allowed Fan ${testChannel + 1} (min=10%, max=95%, fallback=80%)...")
            val readback = flashAndVerify(testRequest)
            val updated = readback.fans.firstOrNull { it.channel == testChannel }
            assertNotNull(updated, "Fan config for channel $testChannel must be present")
            val readbackLimits = updated.limits
            assertNotNull(readbackLimits, "Fan limits for channel $testChannel must be present")
            assertEquals(10.0, readbackLimits.minPowerPercent, 0.2)
            assertEquals(95.0, readbackLimits.maxPowerPercent, 0.2)
            assertEquals(80.0, readbackLimits.fallbackPowerPercent, 0.2)
            assertTrue(readbackLimits.startBoost)
            assertFalse(readbackLimits.holdMinPower)
            println("Verified Fan Limits flashed and read back successfully")
        }

    @Test
    fun testFlashCombinedFanConfigAndLimitsOnAllowedFan() =
        withOctoSafeScope {
            val testChannel = 4 // Fan 5 (unoccupied)
            val testLimits =
                FanLimits(
                    minPowerPercent = 15.0,
                    maxPowerPercent = 90.0,
                    startBoost = false,
                    holdMinPower = true,
                    fallbackPowerPercent = 75.0,
                )
            val testFixedConfig =
                FixedFanConfig(
                    channel = testChannel,
                    powerPercent = 40.0,
                    limits = testLimits,
                )
            val testRequest = FlashConfigRequest(fans = mapOf(testChannel to testFixedConfig))

            println("Testing combined FixedFanConfig and FanLimits on allowed Fan ${testChannel + 1}...")
            val readback = flashAndVerify(testRequest)
            val updated = readback.fans.firstOrNull { it.channel == testChannel }
            assertNotNull(updated, "Fan config for channel $testChannel must be present")
            assertTrue(updated is FixedFanConfig, "Fan config must be FixedFanConfig")
            assertEquals(40.0, updated.powerPercent, 0.5)

            val readbackLimits = updated.limits
            assertNotNull(readbackLimits, "Fan limits for channel $testChannel must be present")
            assertEquals(15.0, readbackLimits.minPowerPercent, 0.2)
            assertEquals(90.0, readbackLimits.maxPowerPercent, 0.2)
            assertEquals(75.0, readbackLimits.fallbackPowerPercent, 0.2)
            assertFalse(readbackLimits.startBoost)
            assertTrue(readbackLimits.holdMinPower)
            println("Verified combined FanConfig and FanLimits flashed and read back successfully on Fan ${testChannel + 1}")
        }

    companion object {
        private fun FanChannelConfig.toSafeRestorableConfig(): FanChannelConfig =
            when (this) {
                is FixedFanConfig -> this
                is CurveFanConfig -> this
                is AutoCurveFanConfig -> this
                is TargetTempFanConfig -> this
                is TwoPointFanConfig -> this
                else -> FixedFanConfig(channel = channel, powerPercent = 50.0, limits = limits)
            }
    }
}
