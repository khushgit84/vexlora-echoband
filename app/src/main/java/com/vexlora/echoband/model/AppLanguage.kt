package com.vexlora.echoband.model

import java.util.Locale

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val ttsLocale: Locale,
    val speechTag: String
) {
    HINGLISH("hinglish", "Indian Hinglish 🇮🇳", Locale("en", "IN"), "en-IN"),
    HINDI("hi-IN", "Hindi (हिंदी) 🇮🇳", Locale("hi", "IN"), "hi-IN"),
    ENGLISH_IN("en-IN", "Indian English 🇮🇳", Locale("en", "IN"), "en-IN"),
    ENGLISH_US("en-US", "English (Global) 🌐", Locale.US, "en-US"),
    TAMIL("ta-IN", "Tamil (தமிழ்) 🇮🇳", Locale("ta", "IN"), "ta-IN"),
    TELUGU("te-IN", "Telugu (తెలుగు) 🇮🇳", Locale("te", "IN"), "te-IN"),
    SPANISH("es-ES", "Spanish (Español) 🇪🇸", Locale("es", "ES"), "es-ES"),
    FRENCH("fr-FR", "French (Français) 🇫🇷", Locale.FRANCE, "fr-FR"),
    GERMAN("de-DE", "German (Deutsch) 🇩🇪", Locale.GERMANY, "de-DE");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: HINGLISH
        }
    }
}
