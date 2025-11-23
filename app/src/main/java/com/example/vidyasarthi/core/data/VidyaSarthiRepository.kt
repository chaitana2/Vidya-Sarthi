package com.example.vidyasarthi.core.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

private const val MIN_PIN = 100000
private const val MAX_PIN = 999999
private const val MAX_LOGS = 100

class VidyaSarthiRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("vidya_prefs", Context.MODE_PRIVATE)

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()
    
    private val _userPin = MutableStateFlow(getOrGenerateUserPin())
    val userPin: StateFlow<String> = _userPin.asStateFlow()

    companion object {
        private const val KEY_HOST_PHONE = "host_phone"
        private const val KEY_USER_PIN = "user_pin"
    }

    fun getHostPhone(): String {
        return prefs.getString(KEY_HOST_PHONE, "") ?: ""
    }

    fun setHostPhone(phone: String) {
        prefs.edit().putString(KEY_HOST_PHONE, phone).apply()
    }
    
    private fun getOrGenerateUserPin(): String {
        var pin = prefs.getString(KEY_USER_PIN, null)
        if (pin == null) {
            pin = generatePin()
            prefs.edit().putString(KEY_USER_PIN, pin).apply()
        }
        return pin
    }
    
    private fun generatePin(): String {
        return Random.nextInt(MIN_PIN, MAX_PIN).toString()
    }
    
    fun resetPin() {
        val newPin = generatePin()
        prefs.edit().putString(KEY_USER_PIN, newPin).apply()
        _userPin.value = newPin
    }
    
    fun getUserPin(): String {
        return _userPin.value
    }
    
    fun updateStatus(status: String) {
        _connectionStatus.value = status
        addLog("Status: $status")
    }

    fun addLog(message: String) {
        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(0, "${System.currentTimeMillis()}: $message")
        if (currentLogs.size > MAX_LOGS) {
            currentLogs.removeAt(currentLogs.lastIndex)
        }
        _logs.value = currentLogs
    }
}