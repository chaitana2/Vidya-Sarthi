package com.example.vidyasarthi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VidyaSarthiApplication
    private val repository = app.repository
    private val smsHandler = SmsHandler(application, repository)
    private val callManager = CallManager(application)
    private val voiceUiManager = app.voiceUiManager
    private val dataTransmissionManager = app.dataTransmissionManager

    val connectionStatus: StateFlow<String> = repository.connectionStatus
    val logs: StateFlow<List<String>> = repository.logs
    val userPin: StateFlow<String> = repository.userPin
    val transmissionState: StateFlow<DataTransmissionManager.TransmissionState> = dataTransmissionManager.transmissionState
    
    private val _receivedContentText = MutableStateFlow<String>("")
    val receivedContentText: StateFlow<String> = _receivedContentText.asStateFlow()

    init {
        viewModelScope.launch {
            dataTransmissionManager.receivedContent.collectLatest { bytes ->
                val text = String(bytes, Charsets.UTF_8)
                _receivedContentText.value = text
                repository.addLog("Content received: ${text.take(20)}...")
                voiceUiManager.speak("Content received successfully")
            }
        }
    }

    fun connectToHost(hostPhone: String) {
        viewModelScope.launch {
            if (hostPhone.isBlank()) {
                voiceUiManager.speak("Please enter a host phone number")
                return@launch
            }
            
            repository.setHostPhone(hostPhone)
            repository.updateStatus("Initiating Connection...")
            voiceUiManager.speak("Initiating connection")
            
            val clientId = repository.getUserPin()
            smsHandler.sendConnectionRequest(hostPhone, clientId)
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
    
    fun answerCall() {
        callManager.answerCall()
        repository.updateStatus("Call Answered")
    }
    
    fun endCall() {
        callManager.endCall()
        repository.updateStatus("Call Ended")
    }
    
    fun startDataTransmission() {
        repository.addLog("Starting Data Listening")
        dataTransmissionManager.startTransmission()
        voiceUiManager.speak("Listening for data")
    }
    
    fun sendData(content: String) {
        viewModelScope.launch {
            repository.addLog("Sending data...")
            voiceUiManager.speak("Sending content")
            val success = dataTransmissionManager.sendContent(content)
            if (success) {
                repository.addLog("Data sent successfully")
                voiceUiManager.speak("Content sent successfully")
            } else {
                repository.addLog("Data send failed")
                voiceUiManager.speak("Error sending content")
            }
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

    override fun onCleared() {
        super.onCleared()
    }
}