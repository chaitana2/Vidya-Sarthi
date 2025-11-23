package com.example.vidyasarthi

import android.app.Application
import com.example.vidyasarthi.core.data.OfflineCache
import com.example.vidyasarthi.core.data.SettingsManager
import com.example.vidyasarthi.core.data.VidyaSarthiRepository
import com.example.vidyasarthi.core.diagnostics.LogManager
import com.example.vidyasarthi.core.sms.SmsHandler
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

    lateinit var settingsManager: SettingsManager
        private set

    lateinit var logManager: LogManager
        private set

    lateinit var smsHandler: SmsHandler
        private set

    override fun onCreate() {
        super.onCreate()
        repository = VidyaSarthiRepository(this)
        offlineCache = OfflineCache(this)
        voiceUiManager = VoiceUiManager(this)
        settingsManager = SettingsManager(this)
        logManager = LogManager(this)
        smsHandler = SmsHandler(this, repository)
        dataTransmissionManager = DataTransmissionManager(this, offlineCache, logManager, smsHandler, voiceUiManager)
        logManager.cleanUpOldLogs()
    }

    override fun onTerminate() {
        voiceUiManager.shutdown()
        super.onTerminate()
    }
}