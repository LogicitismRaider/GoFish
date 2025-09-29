// In a new file, e.g., OpenAiData.kt
package com.example.gofish.network // Or your appropriate package

import com.google.gson.annotations.SerializedName

// --- Request Data Classes ---
data class OpenAiChatRequest(
    val model: String = "gpt-3.5-turbo", // Or your desired model, e.g., "gpt-4"
    val messages: List<ChatMessage>,
    val temperature: Float = 0.7f, // Controls randomness, adjust as needed
    val stream: Boolean? = null // Set to true for streaming
    // You can add other parameters like max_tokens, top_p, etc.
)

data class ChatMessage(
    val role: String, // "system", "user", or "assistant"
    val content: String
)

// --- Non-Streaming Response Data Classes ---
data class OpenAiChatResponse(
    val id: String?,
    @SerializedName("object")
    val objectType: String?,
    val created: Long?,
    val model: String?,
    val choices: List<Choice>?,
    val usage: Usage?,
    val error: OpenAiError? // For capturing API errors
)

data class Choice(
    val index: Int?,
    val message: ChatMessage?,
    @SerializedName("finish_reason")
    val finishReason: String?
)

// --- Streaming Response Chunk Data Classes ---
// Represents a single chunk received during streaming
data class OpenAiStreamChunk(
    val id: String?,
    @SerializedName("object")
    val objectType: String?,
    val created: Long?,
    val model: String?,
    val choices: List<StreamChoice>?,
    val error: OpenAiError? // For capturing API errors in a stream event
)

data class StreamChoice(
    val index: Int?,
    val delta: DeltaContent?,
    @SerializedName("finish_reason")
    val finishReason: String?
)

// Contains the actual new content in the stream
data class DeltaContent(
    val role: String? = null, // Role might appear in the first chunk
    val content: String? = null // The new piece of text
)

// --- Common Data Classes (Usage, Error) ---
data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int?,
    @SerializedName("completion_tokens")
    val completionTokens: Int?,
    @SerializedName("total_tokens")
    val totalTokens: Int?
)

data class OpenAiError(
    val message: String?,
    val type: String?,
    val param: String?,
    val code: String?
)
