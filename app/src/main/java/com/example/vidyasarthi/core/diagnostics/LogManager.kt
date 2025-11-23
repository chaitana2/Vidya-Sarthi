package com.example.vidyasarthi.core.diagnostics

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogManager(private val context: Context) {

    companion object {
        private const val LOG_DIR = "logs"
        private const val MAX_LOG_AGE_DAYS = 30
    }

    fun log(tag: String, message: String) {
        Log.d(tag, message)
        writeToLogFile(tag, message)
    }

    fun logError(tag: String, message: String, e: Throwable? = null) {
        Log.e(tag, message, e)
        writeToLogFile(tag, "ERROR: $message\n${Log.getStackTraceString(e)}")
    }

    private fun writeToLogFile(tag: String, message: String) {
        val logDir = File(context.filesDir, LOG_DIR)
        if (!logDir.exists()) {
            logDir.mkdirs()
        }

        val fileName = "${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.log"
        val logFile = File(logDir, fileName)

        FileOutputStream(logFile, true).use { fos ->
            val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
            fos.write("$timestamp $tag: $message\n".toByteArray())
        }
    }

    fun cleanUpOldLogs() {
        val logDir = File(context.filesDir, LOG_DIR)
        if (!logDir.exists()) {
            return
        }

        val files = logDir.listFiles() ?: return
        val retentionTime = System.currentTimeMillis() - (MAX_LOG_AGE_DAYS * 24 * 60 * 60 * 1000L)

        for (file in files) {
            if (file.lastModified() < retentionTime) {
                file.delete()
            }
        }
    }
}