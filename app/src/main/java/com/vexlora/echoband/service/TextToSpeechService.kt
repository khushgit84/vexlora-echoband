package com.vexlora.echoband.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.vexlora.echoband.model.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

sealed class TtsState {
    data object Idle : TtsState()
    data object Speaking : TtsState()
    data object Done : TtsState()
    data class Error(val message: String) : TtsState()
}

class TextToSpeechService(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _state = MutableStateFlow<TtsState>(TtsState.Idle)
    val state: StateFlow<TtsState> = _state.asStateFlow()

    var activeLanguage: AppLanguage = AppLanguage.HINGLISH
        private set

    init {
        tts = TextToSpeech(context) { status ->
            isInitialized = status == TextToSpeech.SUCCESS
            if (isInitialized) {
                applyLanguageSettings(activeLanguage)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _state.value = TtsState.Speaking
                    }

                    override fun onDone(utteranceId: String?) {
                        _state.value = TtsState.Done
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _state.value = TtsState.Error("Speech playback failed")
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _state.value = TtsState.Error("Speech playback failed (code: $errorCode)")
                    }
                })
            } else {
                _state.value = TtsState.Error("Text-to-speech initialization failed")
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        activeLanguage = language
        if (isInitialized) {
            applyLanguageSettings(language)
        }
    }

    private fun applyLanguageSettings(language: AppLanguage) {
        val targetLocale = language.ttsLocale
        tts?.language = targetLocale
        tts?.setPitch(1.0f)
        tts?.setSpeechRate(1.02f)

        // Find the best real human / natural voice for the target locale
        try {
            val availableVoices = tts?.voices
            if (!availableVoices.isNullOrEmpty()) {
                val bestVoice = availableVoices
                    .filter { it.locale.language.equals(targetLocale.language, ignoreCase = true) }
                    .maxByOrNull { voiceScore(it, targetLocale) }

                if (bestVoice != null) {
                    tts?.voice = bestVoice
                }
            }
        } catch (_: Exception) {
            // Fallback to default voice
        }
    }

    private fun voiceScore(voice: Voice, targetLocale: Locale): Int {
        var score = 0
        if (voice.locale.country.equals(targetLocale.country, ignoreCase = true)) score += 10
        if (!voice.isNetworkConnectionRequired) score += 5
        if (voice.quality >= Voice.QUALITY_HIGH) score += 10
        if (voice.quality >= Voice.QUALITY_VERY_HIGH) score += 15
        val name = voice.name.lowercase(Locale.ROOT)
        if (name.contains("neural") || name.contains("natural") || name.contains("premium")) score += 20
        if (name.contains("en-in") || name.contains("hi-in")) score += 15
        return score
    }

    fun speak(text: String) {
        if (!isInitialized) {
            _state.value = TtsState.Error("Text-to-speech not initialized")
            return
        }

        // Detect Devanagari characters: switch to Hindi engine on the fly if needed
        val hasDevanagari = text.any { it in '\u0900'..'\u097F' }
        if (hasDevanagari && activeLanguage == AppLanguage.HINGLISH) {
            tts?.language = Locale("hi", "IN")
        } else {
            tts?.language = activeLanguage.ttsLocale
        }

        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _state.value = TtsState.Idle
    }

    fun destroy() {
        tts?.apply { stop(); shutdown() }
        tts = null
        isInitialized = false
    }

    fun resetState() {
        _state.value = TtsState.Idle
    }
}
