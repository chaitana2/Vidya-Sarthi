package com.example.vidyasarthi.core.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.vidyasarthi.VidyaSarthiApplication

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            // Get repository from Application class
            val app = context.applicationContext as VidyaSarthiApplication
            val repository = app.repository
            val smsHandler = SmsHandler(context, repository)
            
            messages.forEach { smsMessage ->
                val sender = smsMessage.displayOriginatingAddress
                val messageBody = smsMessage.messageBody
                
                if (sender != null && messageBody != null) {
                    smsHandler.handleIncomingSms(sender, messageBody)
                }
            }
        }
    }
}