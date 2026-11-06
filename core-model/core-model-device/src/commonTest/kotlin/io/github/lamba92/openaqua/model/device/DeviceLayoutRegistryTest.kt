package io.github.lamba92.openaqua.model.device

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
}
