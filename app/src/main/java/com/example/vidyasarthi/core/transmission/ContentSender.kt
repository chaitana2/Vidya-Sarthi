package com.example.vidyasarthi.core.transmission

import android.os.Build
import com.example.vidyasarthi.core.audio.AudioEncoder
import com.example.vidyasarthi.core.data.CompressionUtils
import com.example.vidyasarthi.core.data.ErrorCorrection
import com.example.vidyasarthi.core.diagnostics.LogManager
import com.example.vidyasarthi.core.security.EncryptionManager
import com.example.vidyasarthi.core.sms.SmsHandler
import com.example.vidyasarthi.core.ui.VoiceUiManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.ByteBuffer

private const val TAG = "ContentSender"
private const val FRAME_SIZE = 256
private const val MAX_RETRIES = 2
private const val ACK_TIMEOUT_MS = 5000L
private const val ERROR_RATE_THRESHOLD = 0.15
private const val TYPE_DATA: Byte = 0x01
private const val SMS_SUMMARY_LENGTH = 100
private const val HIGH_ERROR_RATE = 0.1
private const val LOW_ERROR_RATE = 0.02
private const val SLOW_BIT_DURATION = 20
private const val FAST_BIT_DURATION = 10
private const val PACKET_HEADER_SIZE = 22
private const val CRC_SIZE = 8

class ContentSender(
    private val audioEncoder: AudioEncoder,
    private val encryptionManager: EncryptionManager,
    private val logManager: LogManager,
    private val smsHandler: SmsHandler,
    private val voiceUiManager: VoiceUiManager,
    private val receivedAcks: MutableSharedFlow<Byte>
) {
    private var errorRate = 0.0

    suspend fun sendContent(content: String, hostPhone: String, pin: String): Boolean {
        val rawData = content.toByteArray(Charsets.UTF_8)
        val compressed = CompressionUtils.compress(rawData)

        val (encryptedData, iv) = encryptionManager.encrypt(compressed, pin) ?: return false

        val frames = encryptedData.toList().chunked(FRAME_SIZE)
        var successfulFrames = 0

        frames.forEachIndexed { index, frameData ->
            if (sendFrameWithRetry(index.toByte(), frameData.toByteArray(), iv)) {
                successfulFrames++
            } else {
                logManager.logError(TAG, "Failed to send frame $index")
            }
        }

        errorRate = (frames.size - successfulFrames).toDouble() / frames.size
        adjustTransmissionSpeed()

        if (errorRate > ERROR_RATE_THRESHOLD) {
            logManager.log(TAG, "High error rate, falling back to SMS summary.")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                smsHandler.sendSms(hostPhone, "Summary: ${content.take(SMS_SUMMARY_LENGTH)}")
            }
        }

        return successfulFrames == frames.size
    }

    private suspend fun sendFrameWithRetry(seq: Byte, data: ByteArray, iv: ByteArray): Boolean {
        val packet = buildPacket(TYPE_DATA, seq, data, iv)

        repeat(MAX_RETRIES + 1) { attempt ->
            logManager.log(TAG, "Sending frame $seq, attempt ${attempt + 1}")
            audioEncoder.sendData(packet)

            val ackReceived = withTimeoutOrNull(ACK_TIMEOUT_MS) {
                receivedAcks.collect { ackSeq ->
                    if (ackSeq == seq) return@collect
                }
                true
            }

            if (ackReceived == true) {
                return true
            } else if (attempt < MAX_RETRIES) {
                logManager.log(TAG, "Error sending frame $seq, retrying...")
                voiceUiManager.speak("Error occurred, retrying")
            }
        }
        return false
    }

    private fun adjustTransmissionSpeed() {
        when {
            errorRate > HIGH_ERROR_RATE -> audioEncoder.setBitDuration(SLOW_BIT_DURATION)
            errorRate < LOW_ERROR_RATE -> audioEncoder.setBitDuration(FAST_BIT_DURATION)
        }
    }

    private fun buildPacket(type: Byte, seq: Byte, data: ByteArray, iv: ByteArray): ByteArray {
        val length = data.size
        val buffer = ByteBuffer.allocate(PACKET_HEADER_SIZE + length)
        buffer.put(type)
        buffer.put(seq)
        buffer.putInt(length)
        buffer.put(iv)
        buffer.put(data)

        val tempArray = buffer.array().copyOf(buffer.position())
        val crc = ErrorCorrection.calculateCRC(tempArray)

        val finalBuffer = ByteBuffer.allocate(tempArray.size + CRC_SIZE)
        finalBuffer.put(tempArray)
        finalBuffer.putLong(crc)

        return finalBuffer.array()
    }
}