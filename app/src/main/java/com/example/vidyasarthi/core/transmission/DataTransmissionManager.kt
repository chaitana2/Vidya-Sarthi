package com.example.vidyasarthi.core.transmission

import android.util.Log
import com.example.vidyasarthi.core.audio.AudioDecoder
import com.example.vidyasarthi.core.audio.AudioEncoder
import com.example.vidyasarthi.core.data.CompressionUtils
import com.example.vidyasarthi.core.data.ErrorCorrection
import com.example.vidyasarthi.core.data.OfflineCache
import com.example.vidyasarthi.core.security.EncryptionManager
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

class DataTransmissionManager(private val offlineCache: OfflineCache) {

    companion object {
        private const val TAG = "DataTransmissionManager"
        private const val FRAME_SIZE = 256
        private const val MAX_RETRIES = 2
        private const val ACK_TIMEOUT_MS = 5000L

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
    private val audioDecoder = AudioDecoder()
    private val encryptionManager = EncryptionManager()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _receivedContent = MutableSharedFlow<ByteArray>()
    val receivedContent = _receivedContent.asSharedFlow()

    private val _receivedAcks = MutableSharedFlow<Byte>()

    private val _transmissionState = MutableStateFlow(TransmissionState.IDLE)
    val transmissionState: StateFlow<TransmissionState> = _transmissionState.asStateFlow()

    init {
        startReceiving()
    }

    /**
     * Simulates receiving content. Added to fix 'Unresolved reference' error.
     */
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
            handleReceivedPayload(dummyContent)
            receiveBuffer.clear()
        }
    }

    private fun handleReceivedPayload(payload: ByteArray) {
        try {
            val fileName = "received_${System.currentTimeMillis()}.txt"
            offlineCache.saveContent(fileName, payload)
            Log.d(TAG, "Saved received content to $fileName")

            scope.launch {
                _receivedContent.emit(payload)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing payload", e)
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

    suspend fun sendContent(content: String): Boolean {
        _transmissionState.value = TransmissionState.SENDING

        val rawData = content.toByteArray(Charsets.UTF_8)
        val compressed = CompressionUtils.compress(rawData)
        val encryptedPair = encryptionManager.encrypt(compressed)

        if (encryptedPair == null) {
            _transmissionState.value = TransmissionState.ERROR
            return false
        }

        val (encryptedData, _) = encryptedPair

        val frames = encryptedData.toList().chunked(FRAME_SIZE)
        var seq: Byte = 0

        for (frameData in frames) {
            if (!sendFrameWithRetry(seq, frameData.toByteArray())) {
                Log.e(TAG, "Failed to send frame $seq")
                _transmissionState.value = TransmissionState.ERROR
                return false
            }
            seq++
        }

        _transmissionState.value = TransmissionState.COMPLETED
        return true
    }

    private suspend fun sendFrameWithRetry(seq: Byte, data: ByteArray): Boolean {
        val packet = buildPacket(TYPE_DATA, seq, data)

        repeat(MAX_RETRIES + 1) { attempt ->
            Log.d(TAG, "Sending frame $seq, attempt ${attempt + 1}")
            audioEncoder.sendData(packet)

            val ackReceived = withTimeoutOrNull(ACK_TIMEOUT_MS) {
                _receivedAcks.collect { ackSeq ->
                    if (ackSeq == seq) return@collect
                }
                true
            }

            if (ackReceived == true) {
                return true
            }
        }
        return false
    }

    private fun buildPacket(type: Byte, seq: Byte, data: ByteArray): ByteArray {
        val length = data.size
        val buffer = ByteBuffer.allocate(1 + 1 + 4 + length + 8)
        buffer.put(type)
        buffer.put(seq)
        buffer.putInt(length)
        buffer.put(data)

        val tempArray = buffer.array().copyOf(buffer.position())
        val crc = ErrorCorrection.calculateCRC(tempArray)

        val finalBuffer = ByteBuffer.allocate(tempArray.size + 8)
        finalBuffer.put(tempArray)
        finalBuffer.putLong(crc)

        return finalBuffer.array()
    }
}
