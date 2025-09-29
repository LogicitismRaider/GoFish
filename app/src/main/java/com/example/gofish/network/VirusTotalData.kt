package com.example.gofish.network

import com.google.gson.annotations.SerializedName

// --- Response for POST /api/v3/urls ---
data class VirusTotalSubmissionResponse(
    val data: VirusTotalSubmissionData?
)

data class VirusTotalSubmissionData(
    val type: String?,
    val id: String? // This is the Analysis ID
)

// --- Response for GET /api/v3/analyses/{id} ---
data class VirusTotalAnalysisReport(
    val data: VirusTotalAnalysisData?,
    val meta: VirusTotalMetaInfo? // Added for potential future use (like getting original URL)
)

data class VirusTotalAnalysisData(
    val attributes: VirusTotalAttributes?,
    val type: String?,
    val id: String?
)

data class VirusTotalAttributes(
    val date: Long?,
    val status: String?, // e.g., "completed", "queued"
    val stats: VirusTotalStats?,
    @SerializedName("results") // Contains detailed results from each AV engine
    val engineResults: Map<String, VirusTotalEngineResult>?,
    @SerializedName("threat_names")
    val threatNames: List<String>?,
    val url: String? // The URL that was analyzed
)

data class VirusTotalStats(
    val harmless: Int? = 0,
    val malicious: Int? = 0,
    val suspicious: Int? = 0,
    val undetected: Int? = 0,
    val timeout: Int? = 0
)

data class VirusTotalEngineResult(
    val category: String?, // e.g., "harmless", "malicious", "suspicious"
    @SerializedName("engine_name")
    val engineName: String?,
    @SerializedName("method")
    val methodName: String?,
    val result: String? // e.g., null if clean, or the threat name if detected
)

data class VirusTotalMetaInfo(
    @SerializedName("url_info")
    val urlInfo: UrlInfo?
)

data class UrlInfo(
    val url: String?,
    val id: String?
)

// --- Error Response ---
data class VirusTotalApiErrorResponse(
    val error: VirusTotalApiError?
)

data class VirusTotalApiError(
    val code: String?,
    val message: String?
)
