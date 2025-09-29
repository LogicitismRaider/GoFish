package com.example.gofish.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query // Or @Path, @Body depending on the API

// Define data classes for your API's request and response
data class VerificationRequest(val urlToCheck: String) // Example
data class VerificationResponse(val isTrusted: Boolean, val score: Float?, val details: String?) // Example

interface TrustApiService {
    @GET("verify") // Or the specific endpoint path from the API documentation
    suspend fun checkUrlTrustworthiness(
        @Query("url") url: String, // Or however the API expects the URL
        @Query("apiKey") apiKey: String // If the API key is passed as a query parameter
    ): Response<VerificationResponse> // Use Response for more control over the HTTP response

    // Add other API methods as needed
}
