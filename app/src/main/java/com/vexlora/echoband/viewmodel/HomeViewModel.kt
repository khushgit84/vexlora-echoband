package com.vexlora.echoband.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vexlora.echoband.model.AppLanguage
import com.vexlora.echoband.model.AssistantState
import com.vexlora.echoband.model.BluetoothState
import com.vexlora.echoband.model.ConversationMessage
import com.vexlora.echoband.service.AiServiceManager
import com.vexlora.echoband.service.BluetoothService
import com.vexlora.echoband.service.DeviceControlService
import com.vexlora.echoband.service.EchoBandForegroundService
import com.vexlora.echoband.service.MemoryManager
import com.vexlora.echoband.service.RememberedFact
import com.vexlora.echoband.service.SpeechRecognitionService
import com.vexlora.echoband.service.SpeechRecognitionState
import com.vexlora.echoband.service.TextToSpeechService
import com.vexlora.echoband.service.TtsState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val speechService = SpeechRecognitionService(application)
    private val ttsService = TextToSpeechService(application)
    private val bluetoothService = BluetoothService(application)
    private val aiService = AiServiceManager(application)
    private val deviceControl = DeviceControlService(application)
    private val memoryManager = MemoryManager(application)

    private val langPrefs = application.getSharedPreferences("echoband_lang_prefs", Context.MODE_PRIVATE)

    // --- Active Language (Defaults to Indian Hinglish 🇮🇳) ---
    private val _selectedLanguage = MutableStateFlow(
        AppLanguage.fromCode(langPrefs.getString("selected_lang", AppLanguage.HINGLISH.code))
    )
    val selectedLanguage: StateFlow<AppLanguage> = _selectedLanguage.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        _selectedLanguage.value = language
        langPrefs.edit().putString("selected_lang", language.code).apply()
        ttsService.setLanguage(language)
    }

    // --- Continuous Auto-Listen Mode ---
    private val _isAutoListen = MutableStateFlow(true)
    val isAutoListen: StateFlow<Boolean> = _isAutoListen.asStateFlow()

    // --- Background Service Mode ---
    private val _isBackgroundMode = MutableStateFlow(false)
    val isBackgroundMode: StateFlow<Boolean> = _isBackgroundMode.asStateFlow()

    // --- Device Controls State ---
    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _volumePercent = MutableStateFlow(deviceControl.getVolumePercent())
    val volumePercent: StateFlow<Int> = _volumePercent.asStateFlow()

    // --- Remembered Facts & Notes ---
    private val _rememberedFacts = MutableStateFlow<List<RememberedFact>>(emptyList())
    val rememberedFacts: StateFlow<List<RememberedFact>> = _rememberedFacts.asStateFlow()

    // --- API Key Management (pasted directly into app) ---
    private val _apiKey = MutableStateFlow(aiService.apiKey)
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    // --- Reactive states ---
    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _aiResponse = MutableStateFlow("")
    val aiResponse: StateFlow<String> = _aiResponse.asStateFlow()

    // Persistent conversation history across app launches
    private val _conversationHistory = MutableStateFlow<List<ConversationMessage>>(emptyList())
    val conversationHistory: StateFlow<List<ConversationMessage>> = _conversationHistory.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val bluetoothState: StateFlow<BluetoothState> = bluetoothService.connectionState
    val bluetoothDeviceName: StateFlow<String?> = bluetoothService.deviceName

    private var autoListenJob: Job? = null

    init {
        bluetoothService.register()
        observeSpeech()
        observeTts()

        // Apply active language to TTS engine
        ttsService.setLanguage(_selectedLanguage.value)

        // Load remembered conversation history from device memory
        val savedHistory = aiService.loadMemory()
        if (savedHistory.isNotEmpty()) {
            _conversationHistory.value = savedHistory
            _aiResponse.value = savedHistory.lastOrNull { !it.isUser }?.text.orEmpty()
        }

        // Load remembered facts
        _rememberedFacts.value = memoryManager.getFacts()
    }

    private fun observeSpeech() {
        viewModelScope.launch {
            speechService.state.collect { state ->
                when (state) {
                    is SpeechRecognitionState.Idle -> {
                        if (_assistantState.value == AssistantState.LISTENING) {
                            _assistantState.value = AssistantState.IDLE
                        }
                    }
                    is SpeechRecognitionState.Listening -> {
                        _assistantState.value = AssistantState.LISTENING
                        _errorMessage.value = null
                    }
                    is SpeechRecognitionState.PartialResult -> {
                        _recognizedText.value = state.text
                    }
                    is SpeechRecognitionState.Result -> {
                        _recognizedText.value = state.text
                        processUserInput(state.text)
                    }
                    is SpeechRecognitionState.SilenceTimeout -> {
                        if (_isAutoListen.value && _assistantState.value != AssistantState.SPEAKING) {
                            scheduleAutoListen(delayMs = 350)
                        } else {
                            _assistantState.value = AssistantState.IDLE
                        }
                    }
                    is SpeechRecognitionState.Error -> {
                        _assistantState.value = AssistantState.ERROR
                        _errorMessage.value = state.message

                        if (_isAutoListen.value && _assistantState.value != AssistantState.SPEAKING) {
                            scheduleAutoListen(delayMs = 1200)
                        }
                    }
                }
            }
        }
    }

    private fun observeTts() {
        viewModelScope.launch {
            ttsService.state.collect { state ->
                when (state) {
                    is TtsState.Speaking -> {
                        _assistantState.value = AssistantState.SPEAKING
                    }
                    is TtsState.Done -> {
                        _assistantState.value = AssistantState.IDLE
                        ttsService.resetState()

                        if (_isAutoListen.value) {
                            scheduleAutoListen(delayMs = 600)
                        }
                    }
                    is TtsState.Error -> {
                        _assistantState.value = AssistantState.ERROR
                        _errorMessage.value = state.message
                    }
                    is TtsState.Idle -> { /* idle */ }
                }
            }
        }
    }

    private fun scheduleAutoListen(delayMs: Long) {
        autoListenJob?.cancel()
        autoListenJob = viewModelScope.launch {
            delay(delayMs)
            if (_assistantState.value != AssistantState.SPEAKING && _isAutoListen.value) {
                speechService.startListening(_selectedLanguage.value.speechTag)
            }
        }
    }

    // --- Public controls ---

    fun toggleAutoListen() {
        val next = !_isAutoListen.value
        _isAutoListen.value = next
        if (!next) {
            autoListenJob?.cancel()
            if (_assistantState.value == AssistantState.LISTENING) {
                speechService.stopListening()
            }
        } else {
            startListening()
        }
    }

    fun toggleBackgroundService() {
        val next = !_isBackgroundMode.value
        _isBackgroundMode.value = next
        if (next) {
            EchoBandForegroundService.start(getApplication())
        } else {
            EchoBandForegroundService.stop(getApplication())
        }
    }

    fun toggleTorch() {
        val next = !_isTorchOn.value
        val ok = deviceControl.setTorch(next)
        if (ok) _isTorchOn.value = next
    }

    fun increaseVolume() {
        val pct = deviceControl.increaseVolume()
        _volumePercent.value = pct
    }

    fun decreaseVolume() {
        val pct = deviceControl.decreaseVolume()
        _volumePercent.value = pct
    }

    fun deleteFact(id: String) {
        memoryManager.removeFact(id)
        _rememberedFacts.value = memoryManager.getFacts()
    }

    fun updateApiKey(key: String) {
        aiService.apiKey = key
        _apiKey.value = aiService.apiKey
    }

    fun startListening() {
        autoListenJob?.cancel()
        if (_assistantState.value == AssistantState.SPEAKING) {
            ttsService.stop()
        }
        _recognizedText.value = ""
        _aiResponse.value = ""
        _errorMessage.value = null
        speechService.startListening(_selectedLanguage.value.speechTag)
    }

    fun stopListening() {
        autoListenJob?.cancel()
        speechService.stopListening()
    }

    fun dismissError() {
        _errorMessage.value = null
        if (_assistantState.value == AssistantState.ERROR) {
            _assistantState.value = AssistantState.IDLE
        }
    }

    fun clearConversation() {
        autoListenJob?.cancel()
        aiService.clearMemory()
        _conversationHistory.value = emptyList()
        _recognizedText.value = ""
        _aiResponse.value = ""
        _errorMessage.value = null
        if (_assistantState.value == AssistantState.SPEAKING) {
            ttsService.stop()
        }
        _assistantState.value = AssistantState.IDLE
    }

    fun refreshBluetoothStatus() {
        bluetoothService.checkBluetoothAudio()
        _volumePercent.value = deviceControl.getVolumePercent()
    }

    private fun processUserInput(text: String) {
        val userMsg = ConversationMessage(text = text, isUser = true)
        val currentHistory = _conversationHistory.value
        _conversationHistory.value = currentHistory + userMsg

        aiService.saveMemory(_conversationHistory.value)

        // 1. Check if input is a direct phone hardware command, language shift, or memory action
        val commandResult = deviceControl.processCommand(text, memoryManager, _selectedLanguage.value)
        if (commandResult != null && commandResult.handled) {
            if (commandResult.languageShift != null) {
                setLanguage(commandResult.languageShift)
            }

            _isTorchOn.value = deviceControl.isTorchActive
            _volumePercent.value = deviceControl.getVolumePercent()
            _rememberedFacts.value = memoryManager.getFacts()

            val reply = commandResult.spokenResponse
            _aiResponse.value = reply
            _conversationHistory.value = _conversationHistory.value + ConversationMessage(
                text = reply,
                isUser = false
            )
            aiService.saveMemory(_conversationHistory.value)
            ttsService.speak(reply)
            return
        }

        // 2. Otherwise, route through Gemini with continuous conversation history, facts, and active language
        _assistantState.value = AssistantState.THINKING

        viewModelScope.launch {
            val factsSummary = memoryManager.getFactsSummary()
            val result = aiService.sendMessageWithMemory(
                userMessage = text,
                history = currentHistory,
                factsSummary = factsSummary,
                language = _selectedLanguage.value
            )
            result.fold(
                onSuccess = { replyText ->
                    _aiResponse.value = replyText
                    _conversationHistory.value = _conversationHistory.value + ConversationMessage(
                        text = replyText,
                        isUser = false
                    )
                    aiService.saveMemory(_conversationHistory.value)
                    ttsService.speak(replyText)
                },
                onFailure = { error ->
                    val failureMsg = if (_selectedLanguage.value == AppLanguage.HINGLISH) {
                        "Main Gemini se connect nahi kar paya. Kripya Settings mein API key check karein."
                    } else {
                        "I couldn't reach Gemini. Please check your API key in Settings."
                    }
                    _errorMessage.value = error.localizedMessage ?: "Connection failed"
                    _aiResponse.value = failureMsg
                    _conversationHistory.value = _conversationHistory.value + ConversationMessage(
                        text = failureMsg,
                        isUser = false
                    )
                    aiService.saveMemory(_conversationHistory.value)
                    ttsService.speak(failureMsg)
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoListenJob?.cancel()
        speechService.destroy()
        ttsService.destroy()
        bluetoothService.unregister()
    }
}
