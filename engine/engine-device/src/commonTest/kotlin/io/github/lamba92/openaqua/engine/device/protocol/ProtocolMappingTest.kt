package io.github.lamba92.openaqua.engine.device.protocol

import io.github.lamba92.openaqua.engine.device.util.crc16Usb
import io.github.lamba92.openaqua.model.device.CurveFanConfig
import io.github.lamba92.openaqua.model.device.CurvePoint
import io.github.lamba92.openaqua.model.device.DeviceRegistry
import io.github.lamba92.openaqua.model.device.TargetTempFanConfig
import io.github.lamba92.openaqua.model.device.UnconfiguredFanConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ProtocolMappingTest {
    @Test
    fun testOctoVirtualSensorsReportLayoutAndCrc() {
        val octo = DeviceRegistry.OCTO
        assertEquals(51, octo.virtualSensorsReportSize)

        val temperatures =
            mapOf(
                0 to 31.25,
                1 to 42.50,
            )

        val report = VirtualSensorsWriter.buildReport(octo, temperatures)
        assertEquals(51, report.size)
        assertEquals(0x04u.toUByte(), report[0])

        // Slot 0 (centi-C: 3125 = 0x0C35)
        assertEquals(0x0Cu.toUByte(), report[1])
        assertEquals(0x35u.toUByte(), report[2])
        // Slot 1 (centi-C: 4250 = 0x109A)
        assertEquals(0x10u.toUByte(), report[3])
        assertEquals(0x9Au.toUByte(), report[4])
        // Slot 2 (disabled: 0x7FFF)
        assertEquals(0x7Fu.toUByte(), report[5])
        assertEquals(0xFFu.toUByte(), report[6])

        // Types at offset 0x21 (33)
        assertEquals(3u.toUByte(), report[0x21])
        assertEquals(3u.toUByte(), report[0x21 + 1])
        assertEquals(0u.toUByte(), report[0x21 + 2])

        // CRC over bytes [1..48] (length 48) stored at [49..50]
        val expectedCrc = crc16Usb(report, 1, 48)
        val readCrc = ((report[49].toInt() shl 8) or report[50].toInt()) and 0xFFFF
        assertEquals(expectedCrc.toInt(), readCrc)
    }

    @Test
    fun testQuadroVirtualSensorsReportLayout() {
        val quadro = DeviceRegistry.QUADRO
        assertEquals(67, quadro.virtualSensorsReportSize)

        val report = VirtualSensorsWriter.buildReport(quadro, mapOf(0 to 25.0))
        assertEquals(67, report.size)
        assertEquals(0x04u.toUByte(), report[0])

        // Unknown 0x64 block at offset 0x31..0x40 (49..64)
        for (i in 0 until 16) {
            assertEquals(0x64u.toUByte(), report[0x31 + i])
        }

        // CRC over bytes [1..64] (length 64) stored at [65..66]
        val expectedCrc = crc16Usb(report, 1, 64)
        val readCrc = ((report[65].toInt() shl 8) or report[66].toInt()) and 0xFFFF
        assertEquals(expectedCrc.toInt(), readCrc)
    }

    @Test
    fun testFanSensorHardwareIndexMappingForOcto() {
        val octo = DeviceRegistry.OCTO
        val report = UByteArray(octo.ctrlReportLength)
        val baseOffset = 0x5A // Fan 1 ctrl offset

        // 1. Physical sensor 0 (Sensor 1) -> hw index 0
        val curveSensor0 =
            CurveFanConfig(
                channel = 0,
                sensorIndex = 0,
                points = (0 until 16).map { CurvePoint(20.0 + it, 30.0 + it) },
            )
        ConfigWriter.encodeFanChannelConfig(report, baseOffset, curveSensor0, octo)
        val hwSensor0 = ((report[baseOffset + 3].toInt() shl 8) or report[baseOffset + 4].toInt())
        assertEquals(0, hwSensor0)

        val parsedSensor0 = ConfigParser.parseFanChannelConfig(report, baseOffset, channel = 0, descriptor = octo)
        assertIs<CurveFanConfig>(parsedSensor0)
        assertEquals(0, parsedSensor0.sensorIndex)

        // 2. Physical sensor 3 (Sensor 4) -> hw index 3
        val curveSensor3 = curveSensor0.copy(sensorIndex = 3)
        ConfigWriter.encodeFanChannelConfig(report, baseOffset, curveSensor3, octo)
        val hwSensor3 = ((report[baseOffset + 3].toInt() shl 8) or report[baseOffset + 4].toInt())
        assertEquals(3, hwSensor3)

        val parsedSensor3 = ConfigParser.parseFanChannelConfig(report, baseOffset, channel = 0, descriptor = octo)
        assertIs<CurveFanConfig>(parsedSensor3)
        assertEquals(3, parsedSensor3.sensorIndex)

        // 3. Virtual sensor 1 (Channel 4) -> hw index 5 (Flow sensor is 4)
        val curveVirtual1 = curveSensor0.copy(sensorIndex = 4)
        ConfigWriter.encodeFanChannelConfig(report, baseOffset, curveVirtual1, octo)
        val hwSensorVirt1 = ((report[baseOffset + 3].toInt() shl 8) or report[baseOffset + 4].toInt())
        assertEquals(5, hwSensorVirt1)

        val parsedVirt1 = ConfigParser.parseFanChannelConfig(report, baseOffset, channel = 0, descriptor = octo)
        assertIs<CurveFanConfig>(parsedVirt1)
        assertEquals(4, parsedVirt1.sensorIndex)

        // 4. Virtual sensor 16 (Channel 19) -> hw index 20
        val curveVirtual16 = curveSensor0.copy(sensorIndex = 19)
        ConfigWriter.encodeFanChannelConfig(report, baseOffset, curveVirtual16, octo)
        val hwSensorVirt16 = ((report[baseOffset + 3].toInt() shl 8) or report[baseOffset + 4].toInt())
        assertEquals(20, hwSensorVirt16)

        val parsedVirt16 = ConfigParser.parseFanChannelConfig(report, baseOffset, channel = 0, descriptor = octo)
        assertIs<CurveFanConfig>(parsedVirt16)
        assertEquals(19, parsedVirt16.sensorIndex)

        // 5. Hardware index 4 (Flow sensor) -> parsed as UnconfiguredFanConfig
        report[baseOffset + 0] = ConfigParser.MODE_CURVE.toUByte()
        report[baseOffset + 3] = 0x00u
        report[baseOffset + 4] = 0x04u
        val parsedFlow = ConfigParser.parseFanChannelConfig(report, baseOffset, channel = 0, descriptor = octo)
        assertIs<UnconfiguredFanConfig>(parsedFlow)

        // 6. Target temp with Virtual sensor 1 (Channel 4)
        val targetTempConfig = TargetTempFanConfig(channel = 0, sensorIndex = 4, targetTempCelsius = 35.0)
        ConfigWriter.encodeFanChannelConfig(report, baseOffset, targetTempConfig, octo)
        val hwTargetTempSensor = ((report[baseOffset + 3].toInt() shl 8) or report[baseOffset + 4].toInt())
        assertEquals(5, hwTargetTempSensor)

        val parsedTargetTemp = ConfigParser.parseFanChannelConfig(report, baseOffset, channel = 0, descriptor = octo)
        assertIs<TargetTempFanConfig>(parsedTargetTemp)
        assertEquals(4, parsedTargetTemp.sensorIndex)
        assertEquals(35.0, parsedTargetTemp.targetTempCelsius)
    }
}
