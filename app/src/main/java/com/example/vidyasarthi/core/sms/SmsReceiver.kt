package com.example.vidyasarthi.core.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import androidx.annotation.RequiresApi
import com.example.vidyasarthi.VidyaSarthiApplication

class SmsReceiver : BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return
        }

        if (intent.action != "android.provider.Telephony.SMS_RECEIVED") return

        val application = context.applicationContext as VidyaSarthiApplication
        val smsHandler = SmsHandler(application, application.repository)

        val pdus = (intent.extras?.get("pdus") as Array<*>).filterIsInstance<ByteArray>()
        val format = intent.getStringExtra("format")

        for (pdu in pdus) {
            val message = SmsMessage.createFromPdu(pdu, format)
            val sender = message.originatingAddress
            val messageBody = message.messageBody

            if (sender != null) {
                smsHandler.handleIncomingSms(sender, messageBody)
            }
        }
    }
}