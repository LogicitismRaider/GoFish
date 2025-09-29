package com.example.gofish.ui.theme

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gofish.BuildConfig
import com.example.gofish.network.AppRetrofitInstance
// Import VirusTotal data classes if/when needed for response parsing
// import com.example.gofish.network.VirusTotalSubmissionResponse
// import com.example.gofish.network.VirusTotalAnalysisReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay // For potential delays between API calls

class VirusTotalViewModel : ViewModel() {

    private val _scanResult = MutableStateFlow<String?>(null)
    val scanResult: StateFlow<String?> = _scanResult

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // API Key for VirusTotal from BuildConfig
    // IMPORTANT: Ensure VIRUSTOTAL_API_KEY is defined in gradle.properties & build.gradle.kts
    private val apiKey = BuildConfig.VIRUSTOTAL_API_KEY

    fun scanUrlWithVirusTotal(urlToScan: String) {
        if (apiKey.isBlank() || apiKey == "YOUR_VIRUSTOTAL_API_KEY") {
            _error.value = "VirusTotal API Key not found or is placeholder. Please set it in gradle.properties."
            Log.e("VirusTotalViewModel", "API Key is missing or placeholder.")
            return
        }
        if (!isValidUrl(urlToScan)) {
             _error.value = "Invalid URL format provided."
             Log.e("VirusTotalViewModel", "Invalid URL: $urlToScan")
             return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _scanResult.value = null

            try {
                Log.d("VirusTotalViewModel", "Submitting URL for scanning: $urlToScan")
                // Step 1: Submit the URL to VirusTotal
                val submissionResponse = AppRetrofitInstance.virusTotalApi.submitUrl(apiKey, urlToScan)

                if (submissionResponse.isSuccessful) {
                    val submissionData = submissionResponse.body()?.data
                    val analysisId = submissionData?.id
                    Log.d("VirusTotalViewModel", "URL submitted. Analysis ID: $analysisId")

                    if (analysisId != null) {
                        // Optional: Add a small delay before fetching the report,
                        // as analysis might not be instantaneous.
                        // For a quick first pass, we can fetch immediately, but VT might return a "queued" status.
                        // Consider a loop with delay if status is not "completed"
                        delay(15000) // Wait 15 seconds for analysis (adjust as needed, or implement polling)

                        Log.d("VirusTotalViewModel", "Fetching analysis report for ID: $analysisId")
                        // Step 2: Get the analysis report
                        val reportResponse = AppRetrofitInstance.virusTotalApi.getAnalysisReport(apiKey, analysisId)

                        if (reportResponse.isSuccessful) {
                            val reportData = reportResponse.body()?.data?.attributes
                            val stats = reportData?.stats
                            Log.i("VirusTotalViewModel", "Report fetched: ${reportData?.status}, Stats: $stats")

                            // Interpret the results
                            if (reportData?.status == "completed") {
                                val maliciousCount = stats?.malicious ?: 0
                                val suspiciousCount = stats?.suspicious ?: 0
                                //val harmlessCount = stats?.harmless ?: 0
                                //val undetectedCount = stats?.undetected ?: 0

                                if (maliciousCount > 0) {
                                    _scanResult.value = "High Risk: URL flagged as malicious by $maliciousCount engines."
                                } else if (suspiciousCount > 0) {
                                    _scanResult.value = "Suspicious: URL flagged as suspicious by $suspiciousCount engines."
                                } else {
                                    _scanResult.value = "Safe: URL appears to be safe according to VirusTotal."
                                }
                                val threats = reportData.threatNames?.joinToString(", ")
                                if (!threats.isNullOrEmpty()){
                                    _scanResult.value += " Detected threats: $threats"
                                }

                            } else {
                                _scanResult.value = "Analysis pending or status: ${reportData?.status}. Try again shortly."
                            }
                        } else {
                            val errorBody = reportResponse.errorBody()?.string()
                            _error.value = "VirusTotal (Report) API Error: ${reportResponse.code()} - ${reportResponse.message()}. Body: $errorBody"
                            Log.e("VirusTotalViewModel", "Report API Error: ${reportResponse.code()}. Body: $errorBody")
                        }
                    } else {
                        _error.value = "VirusTotal: Did not receive an Analysis ID."
                        Log.e("VirusTotalViewModel", "No Analysis ID in submission response.")
                    }
                } else {
                    val errorBody = submissionResponse.errorBody()?.string()
                    _error.value = "VirusTotal (Submission) API Error: ${submissionResponse.code()} - ${submissionResponse.message()}. Body: $errorBody"
                    Log.e("VirusTotalViewModel", "Submission API Error: ${submissionResponse.code()}. Body: $errorBody")
                }

            } catch (e: Exception) {
                _error.value = "Network/VirusTotal Error: ${e.message}"
                Log.e("VirusTotalViewModel", "Network/VirusTotal Exception", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Basic URL validation (can be improved)
    private fun isValidUrl(url: String): Boolean {
        return try {
            java.net.URL(url) // Check if it can be parsed as a URL
            // More sophisticated regex might be good here, e.g. Patterns.WEB_URL.matcher(url).matches()
            // For now, let's keep it simple or assume input is somewhat sanitized.
            // Consider common schemes.
            url.startsWith("http://") || url.startsWith("https://")
        } catch (e: java.net.MalformedURLException) {
            false
        }
    }
}
