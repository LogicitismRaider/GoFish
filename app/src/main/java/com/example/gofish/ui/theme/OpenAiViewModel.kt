// In a file like OpenAiViewModel.kt
package com.example.gofish.ui.theme // Corrected package declaration

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gofish.BuildConfig
import com.example.gofish.network.ChatMessage
import com.example.gofish.network.OpenAiChatRequest
import com.example.gofish.network.OpenAiChatResponse
import com.example.gofish.network.OpenAiRetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OpenAiViewModel : ViewModel() {

    private val _verificationResult = MutableStateFlow<String?>(null)
    val verificationResult: StateFlow<String?> = _verificationResult

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // This is where you retrieve your API key from BuildConfig
    // Ensure buildFeatures { buildConfig = true } is in your app/build.gradle.kts
    private val apiKey = "Bearer ${BuildConfig.OPENAI_API_KEY}"


    fun processTextWithOpenAi(inputText: String, systemPrompt: String = "You are a helpful assistant.") {
        if (apiKey.isBlank() || apiKey == "Bearer YOUR_OPENAI_API_KEY_HERE" || apiKey == "Bearer \"\"") {
            _error.value = "OpenAI API Key not found. Please set it in gradle.properties."
            Log.e("OpenAiViewModel", "API Key is missing or placeholder.")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _verificationResult.value = null

            val messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = inputText)
            )
            val request = OpenAiChatRequest(messages = messages)

            try {
                Log.d("OpenAiViewModel", "Sending request to OpenAI: $request")
                Log.d("OpenAiViewModel", "Using API Key (first 15 chars): ${apiKey.take(15)}...")

                val response = OpenAiRetrofitInstance.api.getChatCompletions(apiKey, request)

                if (response.isSuccessful) {
                    val chatResponse = response.body()
                    if (chatResponse?.choices?.isNotEmpty() == true) {
                        _verificationResult.value = chatResponse.choices[0].message?.content
                        Log.i("OpenAiViewModel", "OpenAI Success: ${chatResponse.choices[0].message?.content}")
                    } else if (chatResponse?.error != null) {
                        _error.value = "OpenAI API Error: ${chatResponse.error.message}"
                        Log.e("OpenAiViewModel", "OpenAI API Error: ${chatResponse.error.message}")
                    }
                    else {
                        _error.value = "OpenAI: Empty response or no choices."
                        Log.w("OpenAiViewModel", "OpenAI: Empty response or no choices. Full response: $chatResponse")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    _error.value = "OpenAI API Error: ${response.code()} - ${response.message()}. Body: $errorBody"
                    Log.e("OpenAiViewModel", "OpenAI API Error: ${response.code()} - ${response.message()}. Body: $errorBody")
                }
            } catch (e: Exception) {
                _error.value = "Network/OpenAI Error: ${e.message}"
                Log.e("OpenAiViewModel", "Network/OpenAI Exception", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
