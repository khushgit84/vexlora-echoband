package com.vexlora.echoband.service

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vexlora.echoband.model.ConversationMessage
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Manages direct Gemini API communication with persistent multi-turn memory
 * and automatic model fallback for unlimited, continuous conversation.
 */
class AiServiceManager(context: Context? = null) {

    private val prefs: SharedPreferences? = context?.getSharedPreferences(
        "echoband_config",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    companion object {
        private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"
        private const val PREF_KEY_API_KEY = "gemini_api_key"
        private const val PREF_KEY_MEMORY = "conversation_memory"

        // Default empty placeholder — enter your key via in-app Settings or .env backend
        const val INITIAL_API_KEY = ""

        // Supported models in priority order for continuous unlimited operation
        private val MODELS = listOf(
            "gemini-3.5-flash",
            "gemini-3-flash-preview"
        )

        private const val SYSTEM_PROMPT = (
            "You are EchoBand, the intelligent AI voice assistant by VEXLORA, streaming directly " +
            "into the user's Sony WH-CH510 Bluetooth headphones. " +
            "You have continuous memory: remember everything the user tells you across this conversation, " +
            "including their name, context, preferences, and previous statements. " +
            "Audio playback rules: " +
            "1. Deliver responses that sound natural, sleek, and clear when read aloud by Text-To-Speech. " +
            "2. Never use markdown formatting (no asterisks, bullet points, headers, or code blocks). " +
            "3. Keep answers concise (1 to 3 spoken sentences) unless the user asks for deep detail. " +
            "4. Maintain a futuristic, friendly, and reliable persona."
        )
    }

    var apiKey: String = prefs?.getString(PREF_KEY_API_KEY, INITIAL_API_KEY) ?: INITIAL_API_KEY
        set(value) {
            val trimmed = value.trim()
            field = trimmed
            prefs?.edit()?.putString(PREF_KEY_API_KEY, trimmed)?.apply()
        }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()
    }

    private val geminiApi: GeminiDirectApiService by lazy {
        Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiDirectApiService::class.java)
    }

    // ── Persistent Memory Storage ──

    fun saveMemory(history: List<ConversationMessage>) {
        try {
            // Keep the last 40 messages to maintain long-term memory while staying within token limits
            val windowed = if (history.size > 40) history.takeLast(40) else history
            val json = gson.toJson(windowed)
            prefs?.edit()?.putString(PREF_KEY_MEMORY, json)?.apply()
        } catch (_: Exception) {
            // Ignore persistence errors
        }
    }

    fun loadMemory(): List<ConversationMessage> {
        return try {
            val json = prefs?.getString(PREF_KEY_MEMORY, null) ?: return emptyList()
            val type = object : TypeToken<List<ConversationMessage>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clearMemory() {
        prefs?.edit()?.remove(PREF_KEY_MEMORY)?.apply()
    }

    private fun buildSystemPrompt(language: com.vexlora.echoband.model.AppLanguage, factsSummary: String): String {
        val langInstruction = when (language) {
            com.vexlora.echoband.model.AppLanguage.HINGLISH -> (
                "LANGUAGE: Talk in natural, warm, everyday Indian Hinglish (a friendly, conversational blend of Hindi and English written in Latin script, exactly like real people talk in India, e.g. 'Haan bilkul, main abhi karta hoon!', 'Aapka timer set ho gaya hai', 'Bolo bhai, kya help chahiye?'). Be like a friendly Indian buddy."
            )
            com.vexlora.echoband.model.AppLanguage.HINDI -> (
                "LANGUAGE: Talk in polite, natural Hindi (हिंदी). Be warm and helpful."
            )
            com.vexlora.echoband.model.AppLanguage.ENGLISH_IN -> (
                "LANGUAGE: Talk in natural Indian English with warm Indian conversational phrasing."
            )
            com.vexlora.echoband.model.AppLanguage.TAMIL -> "LANGUAGE: Talk in natural, polite Tamil."
            com.vexlora.echoband.model.AppLanguage.TELUGU -> "LANGUAGE: Talk in natural, polite Telugu."
            com.vexlora.echoband.model.AppLanguage.SPANISH -> "LANGUAGE: Talk in natural Spanish."
            com.vexlora.echoband.model.AppLanguage.FRENCH -> "LANGUAGE: Talk in natural French."
            com.vexlora.echoband.model.AppLanguage.GERMAN -> "LANGUAGE: Talk in natural German."
            com.vexlora.echoband.model.AppLanguage.ENGLISH_US -> "LANGUAGE: Talk in natural English."
        }

        return buildString {
            append("You are EchoBand, the personal AI voice assistant by VEXLORA for Sony WH-CH510 Bluetooth headphones.\n")
            append(langInstruction)
            append("\nVoice synthesis rules:\n")
            append("1. Keep answers concise (1 to 3 spoken sentences) unless the user specifically asks for detail.\n")
            append("2. Never use markdown formatting (no asterisks, bullet points, headers, or code blocks).\n")
            append("3. Sound like a real, enthusiastic, human speaker.\n")
            if (factsSummary.isNotBlank()) {
                append("\n$factsSummary\n")
            }
        }
    }

    // ── Multi-turn Direct Chat with Memory & Unlimited Fallback ──

    suspend fun sendMessageWithMemory(
        userMessage: String,
        history: List<ConversationMessage>,
        factsSummary: String = "",
        language: com.vexlora.echoband.model.AppLanguage = com.vexlora.echoband.model.AppLanguage.HINGLISH
    ): Result<String> {
        val key = apiKey.trim()
        if (key.isBlank()) {
            return Result.failure(Exception("Please enter your Gemini API key in Settings."))
        }

        // Build multi-turn conversational contents for memory
        val contents = mutableListOf<GeminiContent>()

        // Take up to the last 20 past turns for contextual memory continuity
        val recentHistory = if (history.size > 20) history.takeLast(20) else history
        for (msg in recentHistory) {
            val role = if (msg.isUser) "user" else "model"
            if (msg.text.isNotBlank()) {
                contents.add(GeminiContent(role = role, parts = listOf(GeminiPart(msg.text))))
            }
        }

        // Append the current user prompt
        contents.add(GeminiContent(role = "user", parts = listOf(GeminiPart(userMessage))))

        val activePrompt = buildSystemPrompt(language, factsSummary)

        val request = GeminiGenerateContentRequest(
            systemInstruction = GeminiSystemInstruction(
                parts = listOf(GeminiPart(activePrompt))
            ),
            contents = contents,
            generationConfig = GeminiGenerationConfig(
                temperature = 0.7f,
                maxOutputTokens = 300
            )
        )

        var lastError: Exception? = null

        // Try primary model, with automatic fallback to secondary model if rate limited or unavailable
        for ((index, model) in MODELS.withIndex()) {
            try {
                if (index > 0) delay(400) // Brief backoff before fallback

                val response = geminiApi.generateContent(
                    model = model,
                    apiKey = key,
                    request = request
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    val candidate = body?.candidates?.firstOrNull()
                    val reply = candidate?.content?.parts?.firstOrNull()?.text?.trim()

                    if (!reply.isNullOrEmpty()) {
                        return Result.success(reply)
                    }
                } else {
                    val code = response.code()
                    val errBody = response.errorBody()?.string() ?: response.message()
                    lastError = Exception("Model $model returned error ($code): $errBody")
                }
            } catch (e: Exception) {
                lastError = e
            }
        }

        return Result.failure(
            lastError ?: Exception("Unable to get response from Gemini. Check your network or API key.")
        )
    }
}
