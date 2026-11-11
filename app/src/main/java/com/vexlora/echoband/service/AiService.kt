package com.vexlora.echoband.service

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class GeminiPart(
    @SerializedName("text") val text: String
)

data class GeminiContent(
    @SerializedName("role") val role: String,
    @SerializedName("parts") val parts: List<GeminiPart>
)

data class GeminiSystemInstruction(
    @SerializedName("parts") val parts: List<GeminiPart>
)

data class GeminiGenerationConfig(
    @SerializedName("temperature") val temperature: Float = 0.7f,
    @SerializedName("maxOutputTokens") val maxOutputTokens: Int = 300
)

data class GeminiGenerateContentRequest(
    @SerializedName("system_instruction") val systemInstruction: GeminiSystemInstruction? = null,
    @SerializedName("contents") val contents: List<GeminiContent>,
    @SerializedName("generationConfig") val generationConfig: GeminiGenerationConfig = GeminiGenerationConfig()
)

data class GeminiCandidate(
    @SerializedName("content") val content: GeminiContent?
)

data class GeminiError(
    @SerializedName("code") val code: Int?,
    @SerializedName("message") val message: String?,
    @SerializedName("status") val status: String?
)

data class GeminiGenerateContentResponse(
    @SerializedName("candidates") val candidates: List<GeminiCandidate>?,
    @SerializedName("error") val error: GeminiError?
)

/**
 * Direct Gemini REST API service.
 * Enables VEXLORA EchoBand to talk directly to Gemini anywhere with multi-turn memory.
 */
interface GeminiDirectApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateContentRequest
    ): Response<GeminiGenerateContentResponse>
}
