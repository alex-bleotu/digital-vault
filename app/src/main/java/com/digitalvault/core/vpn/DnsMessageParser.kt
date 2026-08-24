package com.digitalvault.core.vpn

object DnsMessageParser {

    private const val TYPE_A = 1
    private const val TYPE_AAAA = 28
    private const val CLASS_IN = 1

    fun parseQueriedDomain(message: ByteArray, length: Int): String? {
        if (length < 12) {
            return null
        }
        val questionCount = ((message[4].toInt() and 0xFF) shl 8) or (message[5].toInt() and 0xFF)
        if (questionCount < 1) {
            return null
        }
        val labels = StringBuilder()
        var offset = 12
        while (offset < length) {
            val labelLength = message[offset].toInt() and 0xFF
            if (labelLength == 0) {
                offset += 1

                break
            }
            if (offset + 1 + labelLength > length) {
                return null
            }
            if (labels.isNotEmpty()) {
                labels.append('.')
            }
            labels.append(String(message, offset + 1, labelLength, Charsets.US_ASCII))
            offset += 1 + labelLength
        }

        return labels.toString().takeIf { it.isNotEmpty() }
    }

    fun buildBlockedResponse(query: ByteArray, queryLength: Int): ByteArray? {
        if (queryLength < 12) {
            return null
        }
        val questionCount = ((query[4].toInt() and 0xFF) shl 8) or (query[5].toInt() and 0xFF)
        if (questionCount < 1) {
            return null
        }
        var offset = 12
        while (offset < queryLength) {
            val labelLength = query[offset].toInt() and 0xFF
            if (labelLength == 0) {
                offset += 1

                break
            }
            offset += 1 + labelLength
        }
        if (offset + 4 > queryLength) {
            return null
        }
        val questionType = ((query[offset].toInt() and 0xFF) shl 8) or (query[offset + 1].toInt() and 0xFF)
        val questionEnd = offset + 4
        val isAaaa = questionType == TYPE_AAAA

        val header = ByteArray(12)
        header[0] = query[0]
        header[1] = query[1]
        header[2] = (0x81).toByte()
        header[3] = (0x80).toByte()
        header[4] = 0
        header[5] = 1
        header[6] = 0
        header[7] = 1
        header[8] = 0
        header[9] = 0
        header[10] = 0
        header[11] = 0

        val question = query.copyOfRange(12, questionEnd)
        val answer = buildAnswerRecord(isAaaa)

        return header + question + answer
    }

    private fun buildAnswerRecord(isAaaa: Boolean): ByteArray {
        val name = byteArrayOf(0xC0.toByte(), 0x0C)
        val type = if (isAaaa) TYPE_AAAA else TYPE_A
        val recordType = byteArrayOf((type shr 8).toByte(), (type and 0xFF).toByte())
        val recordClass = byteArrayOf(0x00, CLASS_IN.toByte())
        val ttl = byteArrayOf(0x00, 0x00, 0x00, 0x3C)
        val address = if (isAaaa) ByteArray(16) else byteArrayOf(0, 0, 0, 0)
        val dataLength = byteArrayOf((address.size shr 8).toByte(), (address.size and 0xFF).toByte())

        return name + recordType + recordClass + ttl + dataLength + address
    }
}
