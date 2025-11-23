package com.example.vidyasarthi.core.call

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.vidyasarthi.R
import com.example.vidyasarthi.VidyaSarthiApplication

class CallService : Service() {

    companion object {
        private const val CHANNEL_ID = "CallServiceChannel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
    }

    private lateinit var callManager: CallManager
    private lateinit var telephonyManager: TelephonyManager
    private var lastState: Int = TelephonyManager.CALL_STATE_IDLE

    @RequiresApi(Build.VERSION_CODES.S)
    private var telephonyCallback: CallStateCallback? = null

    @Suppress("DEPRECATION")
    private var phoneStateListener: CallStateListener? = null


    override fun onCreate() {
        super.onCreate()
        val application = applicationContext as VidyaSarthiApplication
        callManager = application.callManager
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback = CallStateCallback()
        } else {
            @Suppress("DEPRECATION")
            phoneStateListener = CallStateListener()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                val notification = createNotification()
                startForeground(NOTIFICATION_ID, notification)
                registerCallStateListener()
            }
            ACTION_STOP_SERVICE -> {
                unregisterCallStateListener()
                @Suppress("DEPRECATION")
                stopForeground(true)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun registerCallStateListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let {
                telephonyManager.registerTelephonyCallback(ContextCompat.getMainExecutor(this), it)
            }
        } else {
            @Suppress("DEPRECATION")
            phoneStateListener?.let {
                telephonyManager.listen(it, PhoneStateListener.LISTEN_CALL_STATE)
            }
        }
    }

    private fun unregisterCallStateListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let {
                telephonyManager.unregisterTelephonyCallback(it)
            }
        } else {
            @Suppress("DEPRECATION")
            phoneStateListener?.let {
                telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
            }
        }
    }
    
    private fun handleCallStateChange(state: Int) {
        if (lastState == state) return

        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                callManager.updateCallState(CallManager.CallState.Ringing)
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                callManager.updateCallState(CallManager.CallState.Active)
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (lastState == TelephonyManager.CALL_STATE_OFFHOOK || lastState == TelephonyManager.CALL_STATE_RINGING) {
                    callManager.updateCallState(CallManager.CallState.Ended)
                } else {
                    callManager.updateCallState(CallManager.CallState.Idle)
                }
            }
        }
        lastState = state
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private inner class CallStateCallback : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            handleCallStateChange(state)
        }
    }

    @Suppress("DEPRECATION")
    private inner class CallStateListener : PhoneStateListener() {
        @Deprecated("Use TelephonyCallback instead")
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            handleCallStateChange(state)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Vidya Sarthi Voice Data")
            .setContentText("Service is running to handle voice data transmission.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Voice Data Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        unregisterCallStateListener()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
