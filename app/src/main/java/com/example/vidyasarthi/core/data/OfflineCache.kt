package com.example.vidyasarthi.core.data

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class OfflineCache(private val context: Context) {

    companion object {
        private const val TAG = "OfflineCache"
        private const val CACHE_DIR_NAME = "offline_content"
        private const val MAX_CACHE_SIZE_MB = 50
        private const val RETENTION_DAYS = 7
    }

    init {
        createCacheDir()
    }

    private fun createCacheDir() {
        val cacheDir = File(context.filesDir, CACHE_DIR_NAME)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    fun saveContent(fileName: String, content: ByteArray) {
        try {
            val cacheDir = File(context.filesDir, CACHE_DIR_NAME)
            val file = File(cacheDir, fileName)
            FileOutputStream(file).use { it.write(content) }
            Log.d(TAG, "Content saved: $fileName")
            
            manageCacheSize()
        } catch (e: IOException) {
            Log.e(TAG, "Error saving content", e)
        }
    }

    fun getContent(fileName: String): ByteArray? {
        val cacheDir = File(context.filesDir, CACHE_DIR_NAME)
        val file = File(cacheDir, fileName)
        return if (file.exists()) {
            file.readBytes()
        } else {
            null
        }
    }

    private fun manageCacheSize() {
        val cacheDir = File(context.filesDir, CACHE_DIR_NAME)
        val files = cacheDir.listFiles() ?: return

        var totalSize = files.sumOf { it.length() }
        val maxSizeBytes = MAX_CACHE_SIZE_MB * 1024 * 1024

        if (totalSize > maxSizeBytes) {
            // Delete oldest files
            val sortedFiles = files.sortedBy { it.lastModified() }
            for (file in sortedFiles) {
                totalSize -= file.length()
                file.delete()
                if (totalSize <= maxSizeBytes) break
            }
        }
        
        // Cleanup old files
        val retentionTime = System.currentTimeMillis() - (RETENTION_DAYS * 24 * 60 * 60 * 1000L)
        files.forEach { 
            if (it.lastModified() < retentionTime) {
                it.delete()
            }
        }
    }
}