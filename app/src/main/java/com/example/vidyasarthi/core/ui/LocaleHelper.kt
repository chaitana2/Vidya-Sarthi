package com.example.vidyasarthi.core.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {

    fun setLocale(languageCode: String) {
        val appLocale = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun getSupportedLocales(): List<Locale> {
        return listOf(
            Locale.forLanguageTag("en"),
            Locale.forLanguageTag("hi"),
            Locale.forLanguageTag("bn"),
            Locale.forLanguageTag("ta"),
            Locale.forLanguageTag("te"),
            Locale.forLanguageTag("mr"),
            Locale.forLanguageTag("gu")
        )
    }
}
