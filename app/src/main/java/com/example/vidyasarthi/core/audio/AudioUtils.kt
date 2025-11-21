package com.example.vidyasarthi.core.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object AudioUtils {

    fun generateTone(freq: Double, durationMs: Int, sampleRate: Int): ShortArray {
        val numSamples = (sampleRate * durationMs / 1000)
        val sample = ShortArray(numSamples)
        val phaseIncrement = 2 * Math.PI * freq / sampleRate
        var phase = 0.0
        
        for (i in 0 until numSamples) {
            sample[i] = (sin(phase) * Short.MAX_VALUE).toInt().toShort()
            phase += phaseIncrement
        }
        return sample
    }

    fun goertzel(samples: ShortArray, freq: Double, sampleRate: Int): Double {
        val k = (0.5 + ((samples.size * freq) / sampleRate)).toInt()
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
}