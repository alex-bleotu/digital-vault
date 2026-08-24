package com.digitalvault.core.vpn

object ChecksumUtil {

    fun ipv4HeaderChecksum(packet: ByteArray, headerOffset: Int, headerLength: Int): Int {
        var sum = 0
        var index = headerOffset
        val end = headerOffset + headerLength
        while (index < end) {
            if (index == headerOffset + 10) {
                index += 2

                continue
            }
            sum += ((packet[index].toInt() and 0xFF) shl 8) or (packet[index + 1].toInt() and 0xFF)
            index += 2
        }
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }

        return sum.inv() and 0xFFFF
    }

    fun udpChecksum(
        sourceAddress: ByteArray,
        destAddress: ByteArray,
        udpSegment: ByteArray,
        udpLength: Int,
    ): Int {
        var sum = 0
        sum += pairSum(sourceAddress, 0)
        sum += pairSum(sourceAddress, 2)
        sum += pairSum(destAddress, 0)
        sum += pairSum(destAddress, 2)
        sum += 17
        sum += udpLength

        var index = 0
        while (index + 1 < udpLength) {
            sum += ((udpSegment[index].toInt() and 0xFF) shl 8) or (udpSegment[index + 1].toInt() and 0xFF)
            index += 2
        }
        if (index < udpLength) {
            sum += (udpSegment[index].toInt() and 0xFF) shl 8
        }
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        val result = sum.inv() and 0xFFFF

        return if (result == 0) 0xFFFF else result
    }

    private fun pairSum(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
}
