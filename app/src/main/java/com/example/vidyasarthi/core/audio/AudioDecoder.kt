package com.example.vidyasarthi.core.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

class AudioDecoder(private val context: Context) {

    companion object {
        private const val TAG = "AudioDecoder"
        private const val SAMPLE_RATE = 8000
        private const val FREQ_LOW = 1200.0 // '0'
        private const val FREQ_HIGH = 2200.0 // '1'
        private const val BIT_DURATION_MS = 10
        private const val SAMPLES_PER_BIT = (SAMPLE_RATE * BIT_DURATION_MS) / 1000
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val _decodedData = MutableSharedFlow<Byte>()
    val decodedData = _decodedData.asSharedFlow()
    private val scope = CoroutineScope(Dispatchers.Default)

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startListening() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO permission not granted")
            return
        }

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_CALL,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            Thread {
                processAudioStream(bufferSize)
            }.start()

            Log.d(TAG, "Started audio decoding")

        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to start audio recording due to security issue", e)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to start audio recording", e)
        }
    }

    private fun processAudioStream(bufferSize: Int) {
        val buffer = ShortArray(bufferSize)
        val bitBuffer = ShortArray(SAMPLES_PER_BIT)
        var bitBufferIndex = 0

        while (isRecording) {
            val readCount = audioRecord?.read(buffer, 0, bufferSize) ?: 0
            if (readCount > 0) {
                for (i in 0 until readCount) {
                    bitBuffer[bitBufferIndex++] = buffer[i]
                    if (bitBufferIndex >= SAMPLES_PER_BIT) {
                        processBit(bitBuffer)
                        bitBufferIndex = 0
                    }
                }
            }
        }
    }

    private fun processBit(bitBuffer: ShortArray) {
        var currentByte = 0
        var bitCount = 0
        val bit = decodeBit(bitBuffer)

        currentByte = (currentByte shl 1) or bit
        bitCount++

        if (bitCount == 8) {
            val finalByte = currentByte.toByte()
            scope.launch {
                _decodedData.emit(finalByte)
            }
            currentByte = 0
            bitCount = 0
        }
    }


    private fun decodeBit(samples: ShortArray): Int {
        val powerLow = AudioUtils.goertzel(samples, FREQ_LOW, SAMPLE_RATE)
        val powerHigh = AudioUtils.goertzel(samples, FREQ_HIGH, SAMPLE_RATE)

        return if (powerHigh > powerLow) 1 else 0
    }

    fun stopListening() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error stopping audio record", e)
        }
        audioRecord = null
    }
}