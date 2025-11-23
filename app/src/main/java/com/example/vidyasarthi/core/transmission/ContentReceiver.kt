package com.example.vidyasarthi.core.transmission

import com.example.vidyasarthi.core.audio.AudioDecoder
import com.example.vidyasarthi.core.data.OfflineCache
import com.example.vidyasarthi.core.diagnostics.LogManager
import com.example.vidyasarthi.core.ui.VoiceUiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

private const val TAG = "ContentReceiver"
private const val RECEIVE_BUFFER_SIZE = 100

class ContentReceiver(
    private val audioDecoder: AudioDecoder,
    private val offlineCache: OfflineCache,
    private val logManager: LogManager,
    private val voiceUiManager: VoiceUiManager,
    private val receivedContent: MutableSharedFlow<ByteArray>,
    private val transmissionState: MutableStateFlow<DataTransmissionManager.TransmissionState>
) {
    private val receiveBuffer = ArrayList<Byte>()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun start() {
        scope.launch {
            audioDecoder.decodedData.collect { byte ->
                if (transmissionState.value != DataTransmissionManager.TransmissionState.RECEIVING) {
                    transmissionState.value = DataTransmissionManager.TransmissionState.RECEIVING
                    voiceUiManager.speak("Receiving content")
                }
                processIncomingByte(byte)
            }
        }
    }

    private fun processIncomingByte(byte: Byte) {
        receiveBuffer.add(byte)
        if (receiveBuffer.size > RECEIVE_BUFFER_SIZE) {
            transmissionState.value = DataTransmissionManager.TransmissionState.COMPLETED
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
                receivedContent.emit(payload)
            }
        } catch (e: IOException) {
            logManager.logError(TAG, "Error processing payload", e)
            transmissionState.value = DataTransmissionManager.TransmissionState.ERROR
        }
    }
}