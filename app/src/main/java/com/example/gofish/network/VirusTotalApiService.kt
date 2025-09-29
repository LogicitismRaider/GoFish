package com.example.gofish.network

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface VirusTotalApiService {

    @FormUrlEncoded
    @POST("urls") // Endpoint to submit a URL for scanning
    suspend fun submitUrl(
        @Header("x-apikey") apiKey: String,
        @Field("url") urlToScan: String
    ): Response<VirusTotalSubmissionResponse> // Expects the submission response with an analysis ID

    @GET("analyses/{analysis_id}") // Endpoint to get the analysis report
    suspend fun getAnalysisReport(
        @Header("x-apikey") apiKey: String,
        @Path("analysis_id") analysisId: String
    ): Response<VirusTotalAnalysisReport> // Expects the full analysis report

}
