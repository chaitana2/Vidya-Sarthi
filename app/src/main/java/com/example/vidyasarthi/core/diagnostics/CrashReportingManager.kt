package com.example.vidyasarthi.core.diagnostics

import android.util.Log

/**
 * Abstraction for Crash Reporting services like Firebase Crashlytics.
 * Since we cannot include google-services.json in this environment,
 * this serves as a production-ready wrapper structure.
 */
object CrashReportingManager {

    private const val TAG = "CrashReportingManager"

    fun log(message: String) {
        // In production: FirebaseCrashlytics.getInstance().log(message)
        Log.i(TAG, message)
    }

    fun logException(e: Throwable) {
        // In production: FirebaseCrashlytics.getInstance().recordException(e)
        Log.e(TAG, "Exception captured", e)
    }
    
    fun setCustomKey(key: String, value: String) {
        // In production: FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        Log.d(TAG, "Key set: $key = $value")
    }
}