package com.example.vidyasarthi.core.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class AudioDecoder {

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

    fun startListening() {
        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        try {
             // Note: Permission must be granted before calling this
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_CALL, // Fallback to MIC if VOICE_CALL fails on some devices
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

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording", e)
        }
    }

    private fun processAudioStream(bufferSize: Int) {
        val buffer = ShortArray(bufferSize)
        val bitBuffer = ShortArray(SAMPLES_PER_BIT)
        var bitBufferIndex = 0
        
        var currentByte = 0
        var bitCount = 0
        
        while (isRecording) {
            val readCount = audioRecord?.read(buffer, 0, bufferSize) ?: 0
            if (readCount > 0) {
                for (i in 0 until readCount) {
                    bitBuffer[bitBufferIndex++] = buffer[i]
                    if (bitBufferIndex >= SAMPLES_PER_BIT) {
                        val bit = decodeBit(bitBuffer)
                        
                        // Shift bit into byte (assuming MSB first or LSB first, let's do MSB first)
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
                        
                        bitBufferIndex = 0
                    }
                }
            }
        }
    }

    private fun decodeBit(samples: ShortArray): Int {
        val powerLow = goertzel(samples, FREQ_LOW)
        val powerHigh = goertzel(samples, FREQ_HIGH)
        
        return if (powerHigh > powerLow) 1 else 0
    }

    private fun goertzel(samples: ShortArray, freq: Double): Double {
        val k = (0.5 + ((samples.size * freq) / SAMPLE_RATE)).toInt()
        val omega = (2.0 * PI * k) / samples.size
        val sine = sin(omega)
        val cosine = cos(omega)
        val coeff = 2.0 * cosine
        
        var q0 = 0.0
        var q1 = 0.0
        var q2 = 0.0
        
        for (sample in samples) {
            q0 = coeff * q1 - q2 + sample
            q2 = q1
            q1 = q0
        }
        
        return q1 * q1 + q2 * q2 - q1 * q2 * coeff
    }

    fun stopListening() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio record", e)
        }
        audioRecord = null
    }
}