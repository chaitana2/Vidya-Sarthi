package com.example.vidyasarthi

import com.example.vidyasarthi.core.audio.AudioUtils
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AudioEncoderTest {

    @Test
    fun testToneGeneration() {
        val freq = 1000.0
        val durationMs = 10
        val sampleRate = 8000
        
        val tone = AudioUtils.generateTone(freq, durationMs, sampleRate)
        
        // Expected samples: sampleRate * duration / 1000 = 8000 * 10 / 1000 = 80
        assertTrue(tone.size == 80)
        
        // Check if max amplitude is reasonable (using Short.MAX_VALUE)
        val maxAmp = tone.maxOf { abs(it.toInt()) }
        assertTrue(maxAmp > 0)
    }

    @Test
    fun testGoertzelAlgorithm() {
        val targetFreq = 1200.0
        val sampleRate = 8000
        val durationMs = 10
        val tone = AudioUtils.generateTone(targetFreq, durationMs, sampleRate)

        val power = AudioUtils.goertzel(tone, targetFreq, sampleRate)
        val powerNoise = AudioUtils.goertzel(tone, 2200.0, sampleRate)

        assertTrue("Signal power should be higher than noise power", power > powerNoise)
    }
}