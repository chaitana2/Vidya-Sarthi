package com.example.vidyasarthi.core.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log

class AudioEncoder {

    companion object {
        private const val TAG = "AudioEncoder"
        private const val SAMPLE_RATE = 8000 // Standard for voice calls
        private const val FREQ_LOW = 1200.0 // Representing '0'
        private const val FREQ_HIGH = 2200.0 // Representing '1'
    }

    private var audioTrack: AudioTrack? = null
    private var bitDurationMs = 10 // 10ms per bit = 100bps

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

        val audioData = generateAudioData(data)
        audioTrack?.write(audioData, 0, audioData.size)
    }

    private fun generateAudioData(data: ByteArray): ShortArray {
        val samplesPerBit = SAMPLE_RATE * bitDurationMs / 1000
        val totalSamples = data.size * 8 * samplesPerBit
        val audioData = ShortArray(totalSamples)
        var currentSample = 0

        for (byte in data) {
            for (i in 7 downTo 0) {
                val bit = (byte.toInt() shr i) and 1
                val freq = if (bit == 1) FREQ_HIGH else FREQ_LOW
                val tone = AudioUtils.generateTone(freq, bitDurationMs, SAMPLE_RATE)
                System.arraycopy(tone, 0, audioData, currentSample, tone.size)
                currentSample += tone.size
            }
        }

        return audioData
    }

    fun setBitDuration(durationMs: Int) {
        bitDurationMs = durationMs
    }

    fun release() {
        audioTrack?.release()
    }
}