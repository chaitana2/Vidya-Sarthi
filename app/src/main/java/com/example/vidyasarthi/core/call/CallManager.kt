package com.example.vidyasarthi.core.call

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.content.ContextCompat

class CallManager(private val context: Context) {

    companion object {
        private const val TAG = "CallManager"
    }

    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun startCall(phoneNumber: String) {
        try {
            if (context.checkSelfPermission(android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                val uri = Uri.fromParts("tel", phoneNumber, null)
                val intent = Intent(Intent.ACTION_CALL, uri)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.d(TAG, "Starting call to $phoneNumber")
                
                // Start the service to manage transmission
                startCallService()
            } else {
                Log.e(TAG, "Missing CALL_PHONE permission")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting call", e)
        }
    }

    fun answerCall() {
        try {
            if (context.checkSelfPermission(android.Manifest.permission.ANSWER_PHONE_CALLS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    telecomManager.acceptRingingCall()
                    Log.d(TAG, "Attempted to answer call")
                    setupAudioForDataTransmission()
                    startCallService()
                }
            } else {
                Log.e(TAG, "Missing ANSWER_PHONE_CALLS permission")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error answering call", e)
        }
    }

    fun endCall() {
        try {
            if (context.checkSelfPermission(android.Manifest.permission.ANSWER_PHONE_CALLS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                 if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    telecomManager.endCall()
                    Log.d(TAG, "Attempted to end call")
                }
            }
            stopCallService()
        } catch (e: Exception) {
            Log.e(TAG, "Error ending call", e)
        }
    }

    private fun setupAudioForDataTransmission() {
        try {
            audioManager.mode = AudioManager.MODE_IN_CALL
            audioManager.isSpeakerphoneOn = false
            audioManager.isMicrophoneMute = true // We interact via AudioRecord/AudioTrack
            Log.d(TAG, "Audio configured for data transmission")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up audio", e)
        }
    }

    fun setMicrophoneMute(mute: Boolean) {
        audioManager.isMicrophoneMute = mute
    }

    private fun startCallService() {
        val serviceIntent = Intent(context, CallService::class.java)
        serviceIntent.action = CallService.ACTION_START_SERVICE
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    private fun stopCallService() {
        val serviceIntent = Intent(context, CallService::class.java)
        serviceIntent.action = CallService.ACTION_STOP_SERVICE
        context.startService(serviceIntent)
    }
}