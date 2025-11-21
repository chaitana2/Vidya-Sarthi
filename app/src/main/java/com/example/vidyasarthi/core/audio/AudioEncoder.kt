package com.example.vidyasarthi.core.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlin.math.sin

class AudioEncoder {

    companion object {
        private const val TAG = "AudioEncoder"
        private const val SAMPLE_RATE = 8000 // Standard for voice calls
        private const val FREQ_LOW = 1200.0 // Representing '0'
        private const val FREQ_HIGH = 2200.0 // Representing '1'
        private const val BIT_DURATION_MS = 10 // 10ms per bit = 100bps (Req 7.5)
    }

    private var audioTrack: AudioTrack? = null

    init {
        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack(
            AudioManager.STREAM_VOICE_CALL,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
            AudioTrack.MODE_STREAM
        )
    }

    fun sendData(data: ByteArray) {
        Log.d(TAG, "Encoding and sending ${data.size} bytes")
        audioTrack?.play()
        
        for (byte in data) {
            for (i in 7 downTo 0) {
                val bit = (byte.toInt() shr i) and 1
                val tone = generateTone(if (bit == 1) FREQ_HIGH else FREQ_LOW, BIT_DURATION_MS)
                audioTrack?.write(tone, 0, tone.size)
            }
        }
        
        // Add silence or stop
        // audioTrack?.stop() // Don't stop immediately if streaming
    }
    
    private fun generateTone(freq: Double, durationMs: Int): ShortArray {
        val numSamples = (SAMPLE_RATE * durationMs / 1000)
        val sample = ShortArray(numSamples)
        val phaseIncrement = 2 * Math.PI * freq / SAMPLE_RATE
        var phase = 0.0
        
        for (i in 0 until numSamples) {
            sample[i] = (sin(phase) * Short.MAX_VALUE).toInt().toShort()
            phase += phaseIncrement
        }
        return sample
    }

    fun release() {
        audioTrack?.release()
    }
}