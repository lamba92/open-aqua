package io.github.lamba92.openaqua.model.device

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceLayoutRegistryTest {
    @Test
    fun testFirmwareRange() {
        val any = FirmwareRange.ANY
        assertTrue(1000 in any)
        assertTrue(0 in any)

        val range = FirmwareRange(1000, 1020)
        assertTrue(1000 in range)
        assertTrue(1015 in range)
        assertTrue(1020 in range)
        assertFalse(999 in range)
        assertFalse(1021 in range)

        val upTo = FirmwareRange.upTo(1019)
        assertTrue(0 in upTo)
        assertTrue(1019 in upTo)
        assertFalse(1020 in upTo)

        val from = FirmwareRange.from(1020)
        assertTrue(1020 in from)
        assertTrue(2000 in from)
        assertFalse(1019 in from)
    }

    @Test
    fun testResolveDescriptorDefault() {
        val octo = DeviceLayoutRegistry.resolveDescriptor(DeviceType.OCTO, null)
        assertEquals(DeviceType.OCTO, octo.type)
        assertEquals(0xF011, octo.productId)

        val quadro = DeviceLayoutRegistry.resolveDescriptor(DeviceType.QUADRO, 1000)
        assertEquals(DeviceType.QUADRO, quadro.type)
        assertEquals(0xF00D, quadro.productId)

        val d5Next = DeviceLayoutRegistry.resolveDescriptor(DeviceType.D5_NEXT, 1010)
        assertEquals(DeviceType.D5_NEXT, d5Next.type)
        assertEquals(0xF00E, d5Next.productId)

        val farbwerk360 = DeviceLayoutRegistry.resolveDescriptor(DeviceType.FARBWERK360, 1010)
        assertEquals(DeviceType.FARBWERK360, farbwerk360.type)
        assertEquals(0xF010, farbwerk360.productId)

        val farbwerk = DeviceLayoutRegistry.resolveDescriptor(DeviceType.FARBWERK, null)
        assertEquals(DeviceType.FARBWERK, farbwerk.type)
        assertEquals(0xF00A, farbwerk.productId)

        val highFlowNext = DeviceLayoutRegistry.resolveDescriptor(DeviceType.HIGH_FLOW_NEXT, 1010)
        assertEquals(DeviceType.HIGH_FLOW_NEXT, highFlowNext.type)
        assertEquals(0xF012, highFlowNext.productId)

        val leakshield = DeviceLayoutRegistry.resolveDescriptor(DeviceType.LEAKSHIELD, 1000)
        assertEquals(DeviceType.LEAKSHIELD, leakshield.type)
        assertEquals(0xF014, leakshield.productId)

        val aquaero = DeviceLayoutRegistry.resolveDescriptor(DeviceType.AQUAERO, 6000)
        assertEquals(DeviceType.AQUAERO, aquaero.type)
        assertEquals(0xF001, aquaero.productId)

        val highFlowUsb = DeviceLayoutRegistry.resolveDescriptor(DeviceType.HIGH_FLOW_USB, null)
        assertEquals(DeviceType.HIGH_FLOW_USB, highFlowUsb.type)
        assertEquals(0xF003, highFlowUsb.productId)

        val poweradjust3 = DeviceLayoutRegistry.resolveDescriptor(DeviceType.POWERADJUST3, null)
        assertEquals(DeviceType.POWERADJUST3, poweradjust3.type)
        assertEquals(0xF0BD, poweradjust3.productId)

        val aquastreamXt = DeviceLayoutRegistry.resolveDescriptor(DeviceType.AQUASTREAM_XT, 1018)
        assertEquals(DeviceType.AQUASTREAM_XT, aquastreamXt.type)
        assertEquals(0xF0B6, aquastreamXt.productId)

        val aquastreamUlt = DeviceLayoutRegistry.resolveDescriptor(DeviceType.AQUASTREAM_ULTIMATE, 1010)
        assertEquals(DeviceType.AQUASTREAM_ULTIMATE, aquastreamUlt.type)
        assertEquals(0xF00B, aquastreamUlt.productId)
    }

    @Test
    fun testAllDeviceTypesRegisteredInDeviceRegistry() {
        for (type in DeviceType.entries) {
            val descriptor = DeviceRegistry.findByType(type)
            assertNotNull(descriptor, "Descriptor for $type must be registered")
            assertEquals(type, descriptor.type)
            val byPid = DeviceRegistry.findByProductId(descriptor.productId)
            assertNotNull(byPid, "Lookup by PID 0x${descriptor.productId.toString(16)} failed")
            assertEquals(descriptor, byPid)
        }
    }

    @Test
    fun testFirmwareVersionOffsets() {
        assertEquals(0x0D, DeviceRegistry.OCTO.protocol.firmwareVersionOffset)
        assertEquals(0x0D, DeviceRegistry.QUADRO.protocol.firmwareVersionOffset)
        assertEquals(0x0D, DeviceRegistry.D5_NEXT.protocol.firmwareVersionOffset)
        assertEquals(0x0D, DeviceRegistry.FARBWERK360.protocol.firmwareVersionOffset)
        assertEquals(0x0D, DeviceRegistry.FARBWERK.protocol.firmwareVersionOffset)
        assertEquals(0x0D, DeviceRegistry.HIGH_FLOW_NEXT.protocol.firmwareVersionOffset)
        assertEquals(0x0D, DeviceRegistry.LEAKSHIELD.protocol.firmwareVersionOffset)
        assertEquals(0x0D, DeviceRegistry.AQUASTREAM_ULTIMATE.protocol.firmwareVersionOffset)
        assertEquals(0x0B, DeviceRegistry.AQUAERO.protocol.firmwareVersionOffset)
        assertEquals(0x03, DeviceRegistry.HIGH_FLOW_USB.protocol.firmwareVersionOffset)
        assertEquals(0x21, DeviceRegistry.POWERADJUST3.protocol.firmwareVersionOffset)
        assertEquals(0x32, DeviceRegistry.AQUASTREAM_XT.protocol.firmwareVersionOffset)
    }

    @Test
    fun testDeviceRegistryFindByTypeDelegation() {
        val octoDefault = DeviceRegistry.findByType(DeviceType.OCTO)
        val octoWithFw = DeviceRegistry.findByType(DeviceType.OCTO, 1020)
        assertEquals(octoDefault, octoWithFw)
    }

    @Test
    fun testDetectedHardwareDeviceDescriptorResolution() {
        val device =
            DetectedHardwareDevice(
                id = DeviceId.from("test-octo"),
                alias = DeviceAlias.from("octo-1"),
                type = DeviceType.OCTO,
                name = "Aquacomputer OCTO",
                firmwareVersion = 1020,
            )

        val descriptor = device.descriptor()
        assertEquals(DeviceType.OCTO, descriptor.type)
        assertEquals(0xF011, descriptor.productId)
    }

    @Test
    fun testDeviceCapabilitiesArchitecture() {
        val octo = DeviceRegistry.OCTO
        assertTrue(octo.hasCapability<DeviceCapability.Fans>())
        assertTrue(octo.hasCapability<DeviceCapability.TemperatureSensors>())
        assertTrue(octo.hasCapability<DeviceCapability.VirtualSensors>())
        assertTrue(octo.hasCapability<DeviceCapability.FlowSensor>())
        assertTrue(octo.hasCapability<DeviceCapability.Lighting.Rgbpx>())
        assertFalse(octo.hasCapability<DeviceCapability.Display>())
        assertFalse(octo.hasCapability<DeviceCapability.Lighting.ClassicAnalogRgb>())

        val fansCap = octo.requireCapability<DeviceCapability.Fans>()
        assertEquals(8, fansCap.channels.size)

        val virtualSensorsCap = octo.requireCapability<DeviceCapability.VirtualSensors>()
        assertEquals(16, virtualSensorsCap.count)
        assertEquals(0x33, virtualSensorsCap.reportSize)

        assertFailsWith<IllegalStateException> {
            octo.requireCapability<DeviceCapability.Display>()
        }
    }

    @Test
    fun testDevicesMissingFansCapability() {
        val devicesWithoutFans =
            listOf(
                DeviceRegistry.FARBWERK360,
                DeviceRegistry.FARBWERK,
                DeviceRegistry.HIGH_FLOW_NEXT,
                DeviceRegistry.LEAKSHIELD,
                DeviceRegistry.HIGH_FLOW_USB,
            )

        for (desc in devicesWithoutFans) {
            assertFalse(desc.hasCapability<DeviceCapability.Fans>(), "${desc.name} should not have Fans capability")
            assertNull(desc.findCapability<DeviceCapability.Fans>(), "${desc.name} Fans capability should be null")
        }
    }

    @Test
    fun testDeviceSpecificCapabilities() {
        val farbwerk = DeviceRegistry.FARBWERK
        assertTrue(farbwerk.hasCapability<DeviceCapability.Lighting.ClassicAnalogRgb>())
        assertFalse(farbwerk.hasCapability<DeviceCapability.Lighting.Rgbpx>())
        val analogRgb = farbwerk.requireCapability<DeviceCapability.Lighting.ClassicAnalogRgb>()
        assertEquals(4, analogRgb.channelCount)

        val leakshield = DeviceRegistry.LEAKSHIELD
        assertTrue(leakshield.hasCapability<DeviceCapability.Pressure>())
        assertTrue(leakshield.hasCapability<DeviceCapability.Display>())
        assertEquals(DisplayKind.OLED, leakshield.requireCapability<DeviceCapability.Display>().kind)

        val highFlowNext = DeviceRegistry.HIGH_FLOW_NEXT
        assertTrue(highFlowNext.hasCapability<DeviceCapability.CoolantQuality>())
        assertTrue(highFlowNext.hasCapability<DeviceCapability.Display>())
        assertEquals(DisplayKind.OLED, highFlowNext.requireCapability<DeviceCapability.Display>().kind)

        val aquaero = DeviceRegistry.AQUAERO
        assertTrue(aquaero.hasCapability<DeviceCapability.Display>())
        assertEquals(DisplayKind.LCD, aquaero.requireCapability<DeviceCapability.Display>().kind)
        val aquaeroFlow = aquaero.requireCapability<DeviceCapability.FlowSensor>()
        assertEquals(2, aquaeroFlow.portCount)
    }
    @Test
    fun testCommitStrategies() {
        assertEquals(CommitStrategy.SecondaryReport(), DeviceRegistry.OCTO.protocol.commitStrategy)
        assertEquals(CommitStrategy.SecondaryReport(), DeviceRegistry.QUADRO.protocol.commitStrategy)
        assertEquals(CommitStrategy.SecondaryReport(), DeviceRegistry.D5_NEXT.protocol.commitStrategy)
        assertEquals(CommitStrategy.SecondaryReport(), DeviceRegistry.FARBWERK360.protocol.commitStrategy)
        assertEquals(CommitStrategy.SecondaryReport(), DeviceRegistry.FARBWERK.protocol.commitStrategy)
        assertEquals(CommitStrategy.SecondaryReport(), DeviceRegistry.HIGH_FLOW_NEXT.protocol.commitStrategy)
        assertEquals(CommitStrategy.SecondaryReport(), DeviceRegistry.LEAKSHIELD.protocol.commitStrategy)
        assertEquals(CommitStrategy.SecondaryReport(), DeviceRegistry.AQUASTREAM_ULTIMATE.protocol.commitStrategy)
        assertEquals(CommitStrategy.Direct, DeviceRegistry.AQUAERO.protocol.commitStrategy)
        assertEquals(CommitStrategy.Direct, DeviceRegistry.POWERADJUST3.protocol.commitStrategy)
        assertEquals(CommitStrategy.Direct, DeviceRegistry.AQUASTREAM_XT.protocol.commitStrategy)
        assertEquals(CommitStrategy.None, DeviceRegistry.HIGH_FLOW_USB.protocol.commitStrategy)
    }
}
