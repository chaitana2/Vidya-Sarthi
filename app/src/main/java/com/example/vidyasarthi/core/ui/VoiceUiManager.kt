package com.example.vidyasarthi.core.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class VoiceUiManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val supportedLanguages = listOf("en", "hi", "bn", "ta", "te", "mr", "gu")

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setLanguage(Locale.getDefault().language)
        } else {
            Log.e("VoiceUiManager", "Initialization failed")
        }
    }

    fun setLanguage(language: String) {
        if (!isInitialized) return

        val locale = Locale.forLanguageTag(language)
        val result = tts?.setLanguage(locale)

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("VoiceUiManager", "Language not supported: $language")
            // Fallback to default language
            tts?.language = Locale.getDefault()
        } else {
            tts?.language = locale
        }
    }

    fun speak(text: String) {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
