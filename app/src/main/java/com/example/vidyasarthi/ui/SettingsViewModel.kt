package com.example.vidyasarthi.ui

import androidx.lifecycle.ViewModel
import com.example.vidyasarthi.core.data.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(private val settingsManager: SettingsManager) : ViewModel() {

    private val _dataRate = MutableStateFlow(settingsManager.getDataRate())
    val dataRate: StateFlow<String> = _dataRate.asStateFlow()

    private val _cacheSize = MutableStateFlow(settingsManager.getCacheSize())
    val cacheSize: StateFlow<Int> = _cacheSize.asStateFlow()

    private val _autoRetry = MutableStateFlow(settingsManager.getAutoRetry())
    val autoRetry: StateFlow<Int> = _autoRetry.asStateFlow()

    private val _muteAudio = MutableStateFlow(settingsManager.getMuteAudio())
    val muteAudio: StateFlow<Boolean> = _muteAudio.asStateFlow()

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