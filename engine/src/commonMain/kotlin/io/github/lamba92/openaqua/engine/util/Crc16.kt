package io.github.lamba92.openaqua.engine.util

/**
 * Calculates the USB CRC-16 checksum (poly 0x8005 reversed 0xA001, init 0x0000, xorout 0xFFFF).
 */
fun crc16Usb(
    data: UByteArray,
    offset: Int = 0,
    length: Int = data.size,
): UShort {
    var crc = 0xFFFF
    val end = offset + length
    for (i in offset until end) {
        crc = crc xor (data[i].toInt() and 0xFF)
        for (j in 0 until 8) {
            crc =
                if ((crc and 1) != 0) {
                    (crc ushr 1) xor 0xA001
                } else {
                    crc ushr 1
                }
        }
    }
    return (crc xor 0xFFFF).toUShort()
}
