// In a file like OpenAiApiService.kt
package com.example.gofish.network // Or your appropriate package

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAiApiService {
    @POST("v1/chat/completions") // OpenAI Chat Completions endpoint
    suspend fun getChatCompletions(
        @Header("Authorization") apiKey: String,
        @Body requestBody: OpenAiChatRequest
    ): Response<OpenAiChatResponse>
}