package com.example.vidyasarthi

import android.app.Application
import com.example.vidyasarthi.core.data.OfflineCache
import com.example.vidyasarthi.core.data.VidyaSarthiRepository
import com.example.vidyasarthi.core.transmission.DataTransmissionManager
import com.example.vidyasarthi.core.ui.VoiceUiManager

class VidyaSarthiApplication : Application() {

    lateinit var repository: VidyaSarthiRepository
        private set
        
    lateinit var dataTransmissionManager: DataTransmissionManager
        private set
        
    lateinit var offlineCache: OfflineCache
        private set
        
    lateinit var voiceUiManager: VoiceUiManager
        private set

    override fun onCreate() {
        super.onCreate()
        repository = VidyaSarthiRepository(this)
        offlineCache = OfflineCache(this)
        voiceUiManager = VoiceUiManager(this)
        dataTransmissionManager = DataTransmissionManager(offlineCache)
    }
    
    override fun onTerminate() {
        voiceUiManager.shutdown()
        super.onTerminate()
    }
}