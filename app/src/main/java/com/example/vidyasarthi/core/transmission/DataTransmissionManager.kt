package com.example.vidyasarthi.core.transmission

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.vidyasarthi.core.audio.AudioDecoder
import com.example.vidyasarthi.core.audio.AudioEncoder
import com.example.vidyasarthi.core.data.OfflineCache
import com.example.vidyasarthi.core.diagnostics.LogManager
import com.example.vidyasarthi.core.security.EncryptionManager
import com.example.vidyasarthi.core.sms.SmsHandler
import com.example.vidyasarthi.core.ui.VoiceUiManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class DataTransmissionManager(
    private val context: Context,
    private val offlineCache: OfflineCache,
    private val logManager: LogManager,
    private val smsHandler: SmsHandler,
    private val voiceUiManager: VoiceUiManager
) {

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

    private val _receivedContent = MutableSharedFlow<ByteArray>()
    val receivedContent = _receivedContent.asSharedFlow()

    private val _receivedAcks = MutableSharedFlow<Byte>()

    private val _transmissionState = MutableStateFlow(TransmissionState.IDLE)
    val transmissionState: StateFlow<TransmissionState> = _transmissionState.asStateFlow()

    private val contentSender = ContentSender(audioEncoder, encryptionManager, logManager, smsHandler, voiceUiManager, _receivedAcks)
    private val contentReceiver = ContentReceiver(audioDecoder, offlineCache, logManager, voiceUiManager, _receivedContent, _transmissionState)

    init {
        contentReceiver.start()
    }

    fun simulateReceiveContent(content: String) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        // This is a simulation, so we just emit the bytes directly
    }

    suspend fun onAckReceived(seq: Byte) {
        _receivedAcks.emit(seq)
    }

    @SuppressLint("MissingPermission")
    fun startTransmission() {
        if (hasRecordAudioPermission()) {
            audioDecoder.startListening()
        } else {
            logManager.logError("DataTransmissionManager", "Missing RECORD_AUDIO permission")
            _transmissionState.value = TransmissionState.ERROR
        }
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun stopTransmission() {
        audioDecoder.stopListening()
        audioEncoder.release()
        _transmissionState.value = TransmissionState.IDLE
    }

    suspend fun sendContent(content: String, hostPhone: String, pin: String): Boolean {
        _transmissionState.value = TransmissionState.SENDING
        val result = contentSender.sendContent(content, hostPhone, pin)
        _transmissionState.value = if (result) TransmissionState.COMPLETED else TransmissionState.ERROR
        return result
    }
}