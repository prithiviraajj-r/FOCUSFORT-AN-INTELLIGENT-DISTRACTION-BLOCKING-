package com.example.focusfortvs.data.network

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

@Serializable
data class ChatMessage(
    val role: String,
    val content: List<ContentItem>
)

@Serializable
data class ContentItem(
    val type: String, // "text" or "image_url"
    val text: String? = null,
    val image_url: ImageUrl? = null
)

@Serializable
data class ImageUrl(
    val url: String
)

@Serializable
data class ChatRequest(
    val messages: List<ChatMessage>,
    val max_tokens: Int = 800,
    val temperature: Double = 0.7
)

@Serializable
data class ChatResponse(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: ResponseMessage
)

@Serializable
data class ResponseMessage(
    val content: String
)

interface AzureOpenAIApi {
    @POST
    suspend fun generateQuiz(
        @Url url: String,
        @Header("api-key") apiKey: String,
        @Body request: ChatRequest
    ): ChatResponse
}
