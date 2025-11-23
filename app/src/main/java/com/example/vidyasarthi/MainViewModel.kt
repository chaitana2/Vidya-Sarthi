package com.example.vidyasarthi

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vidyasarthi.core.call.CallManager
import com.example.vidyasarthi.core.data.VidyaSarthiRepository
import com.example.vidyasarthi.core.sms.SmsHandler
import com.example.vidyasarthi.core.transmission.DataTransmissionManager
import com.example.vidyasarthi.core.ui.VoiceUiManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val RECEIVED_CONTENT_LOG_LENGTH = 20

class MainViewModel(
    private val repository: VidyaSarthiRepository,
    private val smsHandler: SmsHandler,
    private val dataTransmissionManager: DataTransmissionManager,
    private val voiceUiManager: VoiceUiManager,
    private val callManager: CallManager,
) : ViewModel() {

    val connectionStatus: StateFlow<String> = repository.connectionStatus
    val logs: StateFlow<List<String>> = repository.logs
    val userPin: StateFlow<String> = repository.userPin
    val transmissionState: StateFlow<DataTransmissionManager.TransmissionState> = dataTransmissionManager.transmissionState

    private val _receivedContentText = MutableStateFlow("")
    val receivedContentText: StateFlow<String> = _receivedContentText.asStateFlow()

    init {
        viewModelScope.launch {
            dataTransmissionManager.receivedContent.collectLatest { bytes ->
                val text = String(bytes, Charsets.UTF_8)
                _receivedContentText.value = text
                repository.addLog("Content received: ${text.take(RECEIVED_CONTENT_LOG_LENGTH)}...")
                voiceUiManager.speak("Content received successfully")
            }
        }
    }

    fun connectToHost(hostPhone: String, contentType: String) = viewModelScope.launch {
        if (hostPhone.isBlank()) {
            voiceUiManager.speak("Please enter a host phone number")
            return@launch
        }

        repository.setHostPhone(hostPhone)
        repository.updateStatus("Initiating Connection...")
        voiceUiManager.speak("Initiating connection")

        val clientId = repository.getUserPin()
        smsHandler.sendConnectionRequest(hostPhone, clientId, contentType)
    }

    fun startVoiceCall(hostPhone: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (hostPhone.isBlank()) {
            repository.addLog("No host phone provided")
            return
        }
        callManager.startCall(hostPhone)
        repository.addLog("Starting voice call to $hostPhone")
    }

    fun answerCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            callManager.answerCall()
            repository.updateStatus("Call Answered")
        }
    }

    fun endCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            callManager.endCall()
            repository.updateStatus("Call Ended")
        }
    }

    fun startDataTransmission() {
        repository.addLog("Starting Data Listening")
        dataTransmissionManager.startTransmission()
        voiceUiManager.speak("Listening for data")
    }

    fun sendData(content: String, hostPhone: String) = viewModelScope.launch {
        repository.addLog("Sending data...")
        voiceUiManager.speak("Sending content")
        val pin = repository.getUserPin()
        val success = dataTransmissionManager.sendContent(content, hostPhone, pin)
        if (success) {
            repository.addLog("Data sent successfully")
            voiceUiManager.speak("Content sent successfully")
        } else {
            repository.addLog("Data send failed")
            voiceUiManager.speak("Error sending content")
        }
    }

    fun simulateReceive(content: String) {
        dataTransmissionManager.simulateReceiveContent(content)
    }

    fun resetUserPin() {
        repository.resetPin()
        repository.addLog("User PIN reset")
        voiceUiManager.speak("PIN reset successfully")
    }
}