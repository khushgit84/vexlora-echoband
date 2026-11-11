package com.vexlora.echoband.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class SpeechRecognitionState {
    data object Idle : SpeechRecognitionState()
    data object Listening : SpeechRecognitionState()
    data class Result(val text: String) : SpeechRecognitionState()
    data class PartialResult(val text: String) : SpeechRecognitionState()
    data object SilenceTimeout : SpeechRecognitionState()
    data class Error(val message: String) : SpeechRecognitionState()
}

class SpeechRecognitionService(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow<SpeechRecognitionState>(SpeechRecognitionState.Idle)
    val state: StateFlow<SpeechRecognitionState> = _state.asStateFlow()

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(languageTag: String = "en-IN") {
        if (!isAvailable) {
            _state.value = SpeechRecognitionState.Error(
                "Speech recognition is not available on this device"
            )
            return
        }

        destroy()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createListener())
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "hi-IN", "en-US"))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        _state.value = SpeechRecognitionState.Listening
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun destroy() {
        speechRecognizer?.apply {
            cancel()
            destroy()
        }
        speechRecognizer = null
    }

    fun resetState() {
        _state.value = SpeechRecognitionState.Idle
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = SpeechRecognitionState.Listening
        }

        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                _state.value = SpeechRecognitionState.SilenceTimeout
                return
            }

            val msg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                else -> "Recognition error ($error)"
            }
            _state.value = SpeechRecognitionState.Error(msg)
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull().orEmpty()
            _state.value = if (text.isNotBlank()) {
                SpeechRecognitionState.Result(text)
            } else {
                SpeechRecognitionState.Error("No speech recognized")
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull().orEmpty()
            if (text.isNotBlank()) {
                _state.value = SpeechRecognitionState.PartialResult(text)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
