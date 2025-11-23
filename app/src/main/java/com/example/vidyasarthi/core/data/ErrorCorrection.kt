package com.example.vidyasarthi.core.data

import java.util.zip.CRC32

class ErrorCorrection {

    companion object {
        private const val REPETITION_FACTOR = 3

        // Simple CRC32 implementation for error detection (Requirement 8.1)
        fun calculateCRC(data: ByteArray): Long {
            val crc = CRC32()
            crc.update(data)
            return crc.value
        }

        fun validateCRC(data: ByteArray, expectedCRC: Long): Boolean {
            val calculated = calculateCRC(data)
            return calculated == expectedCRC
        }

        // Simple repetition-based Forward Error Correction
        fun encodeWithFEC(data: ByteArray): ByteArray {
            val encoded = ByteArray(data.size * REPETITION_FACTOR)
            for (i in data.indices) {
                for (j in 0 until REPETITION_FACTOR) {
                    encoded[i * REPETITION_FACTOR + j] = data[i]
                }
            }
            return encoded
        }

        fun decodeWithFEC(data: ByteArray): ByteArray {
            val decoded = ByteArray(data.size / REPETITION_FACTOR)
            for (i in decoded.indices) {
                val byteCounts = mutableMapOf<Byte, Int>()
                for (j in 0 until REPETITION_FACTOR) {
                    val byte = data[i * REPETITION_FACTOR + j]
                    byteCounts[byte] = (byteCounts[byte] ?: 0) + 1
                }
                decoded[i] = byteCounts.maxByOrNull { it.value }?.key ?: 0
            }
            return decoded
        }
    }
}