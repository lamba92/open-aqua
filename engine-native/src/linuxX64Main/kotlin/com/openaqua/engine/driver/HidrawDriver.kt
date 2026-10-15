package com.openaqua.engine.driver

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.set
import platform.posix.O_RDWR
import platform.posix.close
import platform.posix.ioctl
import platform.posix.open
import platform.posix.usleep

@OptIn(ExperimentalForeignApi::class)
object HidrawDriver {
    private fun hidiocgfeature(len: Int): ULong = 0xC0000000UL or ((len.toULong() and 0x3FFFUL) shl 16) or 0x4807UL

    private fun hidiocsfeature(len: Int): ULong = 0xC0000000UL or ((len.toULong() and 0x3FFFUL) shl 16) or 0x4806UL

    /**
     * Reads a HID feature report with the given ID and payload length.
     * The returned array has size (reportPayloadLength + 1), with index 0 containing reportId.
     */
    fun readFeatureReport(
        hidrawPath: String,
        reportId: UByte,
        reportPayloadLength: Int,
    ): UByteArray? {
        val totalLen = reportPayloadLength + 1
        val fd = open(hidrawPath, O_RDWR)
        if (fd < 0) return null

        try {
            memScoped {
                val buf = allocArray<UByteVar>(totalLen)
                buf[0] = reportId

                val cmd = hidiocgfeature(totalLen)
                val ret = ioctl(fd, cmd, buf)
                if (ret < 0) return null

                val result = UByteArray(totalLen)
                for (i in 0 until totalLen) {
                    result[i] = buf[i]
                }
                return result
            }
        } catch (_: Exception) {
            return null
        } finally {
            close(fd)
        }
    }

    /**
     * Sends a HID feature report. reportData[0] must contain the report ID.
     */
    fun writeFeatureReport(
        hidrawPath: String,
        reportData: UByteArray,
    ): Boolean {
        val totalLen = reportData.size
        val fd = open(hidrawPath, O_RDWR)
        if (fd < 0) return false

        try {
            memScoped {
                val buf = allocArray<UByteVar>(totalLen)
                for (i in 0 until totalLen) {
                    buf[i] = reportData[i]
                }

                // Slow down briefly to allow device to accept reports reliably
                usleep(200_000u)

                val cmd = hidiocsfeature(totalLen)
                val ret = ioctl(fd, cmd, buf)
                return ret >= 0
            }
        } catch (_: Exception) {
            return false
        } finally {
            close(fd)
        }
    }
}
