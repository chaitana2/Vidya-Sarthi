package com.example.vidyasarthi.core.transmission

import android.content.Context
import android.os.Build
import com.example.vidyasarthi.core.audio.AudioDecoder
import com.example.vidyasarthi.core.audio.AudioEncoder
import com.example.vidyasarthi.core.data.CompressionUtils
import com.example.vidyasarthi.core.data.ErrorCorrection
import com.example.vidyasarthi.core.data.OfflineCache
import com.example.vidyasarthi.core.diagnostics.LogManager
import com.example.vidyasarthi.core.sms.SmsHandler
import com.example.vidyasarthi.core.security.EncryptionManager
import com.example.vidyasarthi.core.ui.VoiceUiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.ByteBuffer

class DataTransmissionManager(
    private val context: Context,
    private val offlineCache: OfflineCache,
    private val logManager: LogManager,
    private val smsHandler: SmsHandler,
    private val voiceUiManager: VoiceUiManager
) {

    companion object {
        private const val TAG = "DataTransmissionManager"
        private const val FRAME_SIZE = 256
        private const val MAX_RETRIES = 2
        private const val ACK_TIMEOUT_MS = 5000L
        private const val ERROR_RATE_THRESHOLD = 0.15

        private const val TYPE_DATA: Byte = 0x01
        private const val TYPE_ACK: Byte = 0x02
    }

    enum class TransmissionState {
        IDLE,
        SENDING,
        RECEIVING,
        COMPLETED,
        ERROR
    }

    private val audioEncoder = AudioEncoder()
    private val audioDecoder = AudioDecoder(context)
    private val encryptionManager = EncryptionManager()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _receivedContent = MutableSharedFlow<ByteArray>()
    val receivedContent = _receivedContent.asSharedFlow()

    private val _receivedAcks = MutableSharedFlow<Byte>()

    private val _transmissionState = MutableStateFlow(TransmissionState.IDLE)
    val transmissionState: StateFlow<TransmissionState> = _transmissionState.asStateFlow()

    private var errorRate = 0.0

    init {
        startReceiving()
    }

    fun simulateReceiveContent(content: String) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        scope.launch {
            _receivedContent.emit(bytes)
        }
    }

    private fun startReceiving() {
        scope.launch {
            audioDecoder.decodedData.collect { byte ->
                if (_transmissionState.value != TransmissionState.RECEIVING) {
                    _transmissionState.value = TransmissionState.RECEIVING
                    voiceUiManager.speak("Receiving content")
                }
                processIncomingByte(byte)
            }
        }
    }

    private val receiveBuffer = ArrayList<Byte>()

    private fun processIncomingByte(byte: Byte) {
        receiveBuffer.add(byte)
        if (receiveBuffer.size > 100) {
            _transmissionState.value = TransmissionState.COMPLETED
            val dummyContent = "Received Content ${System.currentTimeMillis()}".toByteArray()
            handleReceivedPayload(dummyContent, "000000") // Dummy PIN for now
            receiveBuffer.clear()
        }
    }

    private fun handleReceivedPayload(payload: ByteArray, pin: String) {
        try {
            val fileName = "received_${System.currentTimeMillis()}.txt"
            offlineCache.saveContent(fileName, payload)
            logManager.log(TAG, "Saved received content to $fileName")

            scope.launch {
                _receivedContent.emit(payload)
            }
        } catch (e: Exception) {
            logManager.logError(TAG, "Error processing payload", e)
            _transmissionState.value = TransmissionState.ERROR
        }
    }

    suspend fun onAckReceived(seq: Byte) {
        _receivedAcks.emit(seq)
    }

    fun startTransmission() {
        audioDecoder.startListening()
    }

    fun stopTransmission() {
        audioDecoder.stopListening()
        audioEncoder.release()
        _transmissionState.value = TransmissionState.IDLE
    }

    suspend fun sendContent(content: String, hostPhone: String, pin: String): Boolean {
        _transmissionState.value = TransmissionState.SENDING

        val rawData = content.toByteArray(Charsets.UTF_8)
        val compressed = CompressionUtils.compress(rawData)
        val encryptedPair = encryptionManager.encrypt(compressed, pin)

        if (encryptedPair == null) {
            _transmissionState.value = TransmissionState.ERROR
            return false
        }

        val (encryptedData, iv) = encryptedPair

        val frames = encryptedData.toList().chunked(FRAME_SIZE)
        var seq: Byte = 0
        var successfulFrames = 0

        for (frameData in frames) {
            if (sendFrameWithRetry(seq, frameData.toByteArray(), iv)) {
                successfulFrames++
            } else {
                logManager.logError(TAG, "Failed to send frame $seq")
            }
            seq++
        }

        errorRate = (frames.size - successfulFrames).toDouble() / frames.size
        adjustTransmissionSpeed()

        if (errorRate > ERROR_RATE_THRESHOLD) {
            logManager.log(TAG, "High error rate, falling back to SMS summary.")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                smsHandler.sendSms(hostPhone, "Summary: ${content.take(100)}")
            }
            _transmissionState.value = TransmissionState.COMPLETED
            return true
        }

        _transmissionState.value = TransmissionState.COMPLETED
        return successfulFrames == frames.size
    }

    private suspend fun sendFrameWithRetry(seq: Byte, data: ByteArray, iv: ByteArray): Boolean {
        val packet = buildPacket(TYPE_DATA, seq, data, iv)

        repeat(MAX_RETRIES + 1) { attempt ->
            logManager.log(TAG, "Sending frame $seq, attempt ${attempt + 1}")
            audioEncoder.sendData(packet)

            val ackReceived = withTimeoutOrNull(ACK_TIMEOUT_MS) {
                _receivedAcks.collect { ackSeq ->
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
            errorRate > 0.1 -> audioEncoder.setBitDuration(20) // Slow down
            errorRate < 0.02 -> audioEncoder.setBitDuration(10) // Speed up
        }
    }

    private fun buildPacket(type: Byte, seq: Byte, data: ByteArray, iv: ByteArray): ByteArray {
        val length = data.size
        val buffer = ByteBuffer.allocate(1 + 1 + 4 + 16 + length + 8)
        buffer.put(type)
        buffer.put(seq)
        buffer.putInt(length)
        buffer.put(iv)
        buffer.put(data)

        val tempArray = buffer.array().copyOf(buffer.position())
        val crc = ErrorCorrection.calculateCRC(tempArray)

        val finalBuffer = ByteBuffer.allocate(tempArray.size + 8)
        finalBuffer.put(tempArray)
        finalBuffer.putLong(crc)

        return finalBuffer.array()
    }
}