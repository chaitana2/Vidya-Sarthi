package com.example.vidyasarthi

import com.example.vidyasarthi.core.data.ErrorCorrection
import com.example.vidyasarthi.core.security.EncryptionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataTransmissionTest {

    @Test
    fun testEncryptionDecryption() {
        val manager = EncryptionManager()
        val originalData = "Test Message".toByteArray()
        val pin = "123456"

        val encryptedPair = manager.encrypt(originalData, pin)
        assertNotNull("Encryption should succeed", encryptedPair)

        val (encrypted, iv) = encryptedPair!!
        val decrypted = manager.decrypt(encrypted, iv, pin)

        assertNotNull("Decryption should succeed", decrypted)
        assertEquals("Decrypted data should match original", String(originalData), String(decrypted!!))
    }

    @Test
    fun testCRC() {
        val data = "Test Data".toByteArray()
        val crc1 = ErrorCorrection.calculateCRC(data)
        val crc2 = ErrorCorrection.calculateCRC(data)

        assertEquals("CRC should be deterministic", crc1, crc2)

        val corruptedData = "Test Data".toByteArray()
        corruptedData[0] = 'X'.code.toByte()
        val crc3 = ErrorCorrection.calculateCRC(corruptedData)

        assertTrue("CRC should change for different data", crc1 != crc3)
    }
}