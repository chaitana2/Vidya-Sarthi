package com.example.vidyasarthi.core.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.example.vidyasarthi.core.data.VidyaSarthiRepository

class SmsHandler(
    private val context: Context,
    private val repository: VidyaSarthiRepository
) {

    companion object {
        private const val TAG = "SmsHandler"
        private const val CONNECTION_REQUEST_PREFIX = "VS_CONNECT:"
        private const val CONNECTION_ACCEPT_PREFIX = "VS_ACCEPT:"
        private const val CONNECTION_REJECT_PREFIX = "VS_REJECT:"
    }

    fun handleIncomingSms(sender: String, messageBody: String) {
        Log.d(TAG, "Received SMS from $sender: $messageBody")
        repository.addLog("Received SMS from $sender")

        if (messageBody.startsWith(CONNECTION_REQUEST_PREFIX)) {
            handleConnectionRequest(sender, messageBody)
        } else if (messageBody.startsWith(CONNECTION_ACCEPT_PREFIX)) {
            handleConnectionAcceptance(sender, messageBody)
        } else if (messageBody.startsWith(CONNECTION_REJECT_PREFIX)) {
            repository.updateStatus("Connection Rejected")
            repository.addLog("Connection rejected by host")
        }
    }

    fun sendConnectionRequest(hostPhone: String, clientId: String, contentType: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val message = "$CONNECTION_REQUEST_PREFIX$clientId:$contentType"
            sendSms(hostPhone, message)
            repository.addLog("Sent connection request to $hostPhone for $contentType")
        }
    }

    private fun handleConnectionRequest(sender: String, message: String) {
        // Requirement 2: Validate client identifier
        val parts = message.removePrefix(CONNECTION_REQUEST_PREFIX).split(":")
        val clientId = parts.getOrNull(0)?.trim() ?: ""
        val contentType = parts.getOrNull(1)?.trim() ?: ""

        // Mock validation logic
        val isValid = validateClient(clientId)

        if (isValid) {
            val sessionToken = java.util.UUID.randomUUID().toString().substring(0, 8)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                sendSms(sender, "$CONNECTION_ACCEPT_PREFIX$sessionToken")
            }
            repository.addLog("Accepted connection from $sender for $contentType")
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                sendSms(sender, "${CONNECTION_REJECT_PREFIX}INVALID_ID")
            }
            repository.addLog("Rejected connection from $sender")
        }
    }

    private fun handleConnectionAcceptance(sender: String, message: String) {
        val sessionToken = message.removePrefix(CONNECTION_ACCEPT_PREFIX).trim()
        Log.d(TAG, "Connection accepted. Token: $sessionToken")
        repository.updateStatus("Connected")
        repository.addLog("Connection accepted. Token: $sessionToken")
        // Ideally trigger voice call here
    }

    private fun validateClient(clientId: String): Boolean {
        return clientId.isNotEmpty()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun sendSms(phoneNumber: String, message: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "SEND_SMS permission not granted")
            repository.addLog("Failed to send SMS: Permission not granted.")
            return
        }
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d(TAG, "Sent SMS to $phoneNumber: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
            repository.addLog("Failed to send SMS: ${e.message}")
        }
    }
}