package com.example.vidyasarthi.core.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DATA_RATE = "data_rate"
        private const val KEY_CACHE_SIZE = "cache_size"
        private const val KEY_AUTO_RETRY = "auto_retry"
        private const val KEY_MUTE_AUDIO = "mute_audio"
    }

    fun saveDataRate(rate: String) {
        prefs.edit().putString(KEY_DATA_RATE, rate).apply()
    }

    fun getDataRate(): String {
        return prefs.getString(KEY_DATA_RATE, "Medium") ?: "Medium"
    }

    fun saveCacheSize(size: Int) {
        prefs.edit().putInt(KEY_CACHE_SIZE, size).apply()
    }

    fun getCacheSize(): Int {
        return prefs.getInt(KEY_CACHE_SIZE, 20)
    }

    fun saveAutoRetry(retries: Int) {
        prefs.edit().putInt(KEY_AUTO_RETRY, retries).apply()
    }

    fun getAutoRetry(): Int {
        return prefs.getInt(KEY_AUTO_RETRY, 2)
    }

    fun saveMuteAudio(mute: Boolean) {
        prefs.edit().putBoolean(KEY_MUTE_AUDIO, mute).apply()
    }

    fun getMuteAudio(): Boolean {
        return prefs.getBoolean(KEY_MUTE_AUDIO, true)
    }
}