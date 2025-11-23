package com.example.vidyasarthi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.vidyasarthi.core.call.CallManager
import com.example.vidyasarthi.core.data.VidyaSarthiRepository
import com.example.vidyasarthi.core.sms.SmsHandler
import com.example.vidyasarthi.core.transmission.DataTransmissionManager
import com.example.vidyasarthi.core.ui.VoiceUiManager

class ViewModelFactory(
    private val repository: VidyaSarthiRepository,
    private val smsHandler: SmsHandler,
    private val dataTransmissionManager: DataTransmissionManager,
    private val voiceUiManager: VoiceUiManager,
    private val callManager: CallManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(
                repository,
                smsHandler,
                dataTransmissionManager,
                voiceUiManager,
                callManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}