package com.example.vidyasarthi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.vidyasarthi.core.call.CallManager
import com.example.vidyasarthi.core.transmission.DataTransmissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VidyaSarthiApplication
    private val repository = app.repository
    private val smsHandler = app.smsHandler
    private val settingsManager = app.settingsManager
    private val callManager = app.callManager
    private val voiceUiManager = app.voiceUiManager
    private val dataTransmissionManager = app.dataTransmissionManager

    val connectionStatus: StateFlow<String> = repository.connectionStatus
    val logs: StateFlow<List<String>> = repository.logs
    val userPin: StateFlow<String> = repository.userPin
    val transmissionState: StateFlow<DataTransmissionManager.TransmissionState> = dataTransmissionManager.transmissionState

    private val _receivedContentText = MutableStateFlow("")
    val receivedContentText: StateFlow<String> = _receivedContentText.asStateFlow()

    private val _dataRate = MutableStateFlow(settingsManager.getDataRate())
    val dataRate: StateFlow<String> = _dataRate.asStateFlow()

    private val _cacheSize = MutableStateFlow(settingsManager.getCacheSize())
    val cacheSize: StateFlow<Int> = _cacheSize.asStateFlow()

    private val _autoRetry = MutableStateFlow(settingsManager.getAutoRetry())
    val autoRetry: StateFlow<Int> = _autoRetry.asStateFlow()

    private val _muteAudio = MutableStateFlow(settingsManager.getMuteAudio())
    val muteAudio: StateFlow<Boolean> = _muteAudio.asStateFlow()

    init {
        viewModelScope.launch {
            dataTransmissionManager.receivedContent.collectLatest { bytes ->
                val text = String(bytes, Charsets.UTF_8)
                _receivedContentText.value = text
                repository.addLog("Content received: ${text.take(20)}...")
                voiceUiManager.speak("Content received successfully")
            }
        }

        viewModelScope.launch {
            callManager.callState.collectLatest { state ->
                val status = when (state) {
                    CallManager.CallState.Idle -> "Call Idle"
                    CallManager.CallState.Ringing -> "Call Ringing"
                    CallManager.CallState.Active -> "Call Active"
                    CallManager.CallState.Ended -> "Call Ended"
                }
                repository.updateStatus(status)
            }
        }
    }

    fun connectToHost(hostPhone: String, contentType: String) {
        viewModelScope.launch {
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
    }

    fun startVoiceCall(hostPhone: String) {
        if (hostPhone.isNotBlank()) {
            callManager.startCall(hostPhone)
            repository.addLog("Starting voice call to $hostPhone")
        } else {
            repository.addLog("No host phone provided")
        }
    }

    fun startDataTransmission() {
        repository.addLog("Starting Data Listening")
        dataTransmissionManager.startTransmission()
        voiceUiManager.speak("Listening for data")
    }

    fun sendData(content: String, hostPhone: String) {
        viewModelScope.launch {
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
    }

    fun resetUserPin() {
        repository.resetPin()
        repository.addLog("User PIN reset")
        voiceUiManager.speak("PIN reset successfully")
    }

    fun setDataRate(rate: String) {
        settingsManager.saveDataRate(rate)
        _dataRate.value = rate
    }

    fun setCacheSize(size: Int) {
        settingsManager.saveCacheSize(size)
        _cacheSize.value = size
    }

    fun setAutoRetry(retries: Int) {
        settingsManager.saveAutoRetry(retries)
        _autoRetry.value = retries
    }

    fun setMuteAudio(mute: Boolean) {
        settingsManager.saveMuteAudio(mute)
        _muteAudio.value = mute
    }
}