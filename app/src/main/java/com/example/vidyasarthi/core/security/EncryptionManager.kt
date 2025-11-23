package com.example.vidyasarthi.core.security

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class EncryptionManager {

    companion object {
        private const val TAG = "EncryptionManager"
        private const val TRANSFORMATION = "AES/CTR/NoPadding"
        private const val ALGORITHM = "AES"
        private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val ITERATION_COUNT = 1000
        private const val KEY_LENGTH = 128 // 128-bit AES
    }

    private fun deriveKey(pin: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
        val secret = factory.generateSecret(spec)
        return SecretKeySpec(secret.encoded, ALGORITHM)
    }

    fun encrypt(data: ByteArray, pin: String): Pair<ByteArray, ByteArray>? {
        return try {
            // Use a random salt/IV for each encryption for security
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)
            val key = deriveKey(pin, iv)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
            val encryptedData = cipher.doFinal(data)
            Pair(encryptedData, iv)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            null
        }
    }

    fun decrypt(encryptedData: ByteArray, iv: ByteArray, pin: String): ByteArray? {
        return try {
            val key = deriveKey(pin, iv)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
            cipher.doFinal(encryptedData)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            null
        }
    }
}