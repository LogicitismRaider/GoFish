// In your RetrofitInstance.kt
package com.example.gofish.network

import com.example.gofish.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object AppRetrofitInstance {

    private const val API_BASE_URL = "https://www.virustotal.com/api/v3/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    // You might want to add an interceptor here to automatically add the VirusTotal API key
    // to every request if you prefer, instead of adding it as a @Header in the service interface.
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS) // Increased timeout for potentially longer scans
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val virusTotalApi: VirusTotalApiService by lazy {
        Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VirusTotalApiService::class.java)
    }
}
