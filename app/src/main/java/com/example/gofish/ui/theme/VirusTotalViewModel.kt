package com.example.gofish.ui.theme

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gofish.BuildConfig
import com.example.gofish.network.AppRetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class VirusTotalViewModel : ViewModel() {

    private val _scanResult = MutableStateFlow<String?>(null)
    val scanResult: StateFlow<String?> = _scanResult

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _safetyPercentage = MutableStateFlow<Int?>(null)
    val safetyPercentage: StateFlow<Int?> = _safetyPercentage

    private val _detailedReport = MutableStateFlow<String?>(null)
    val detailedReport: StateFlow<String?> = _detailedReport

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
            _safetyPercentage.value = null
            _detailedReport.value = null

            try {
                Log.d("VirusTotalViewModel", "Submitting URL for scanning: $urlToScan")
                val submissionResponse = AppRetrofitInstance.virusTotalApi.submitUrl(apiKey, urlToScan)

                if (submissionResponse.isSuccessful) {
                    val analysisId = submissionResponse.body()?.data?.id
                    Log.d("VirusTotalViewModel", "URL submitted. Analysis ID: $analysisId")

                    if (analysisId != null) {
                        delay(15000) // Wait for analysis
                        Log.d("VirusTotalViewModel", "Fetching analysis report for ID: $analysisId")
                        val reportResponse = AppRetrofitInstance.virusTotalApi.getAnalysisReport(apiKey, analysisId)

                        if (reportResponse.isSuccessful) {
                            val reportAttributes = reportResponse.body()?.data?.attributes
                            val stats = reportAttributes?.stats
                            Log.i("VirusTotalViewModel", "Report fetched: ${reportAttributes?.status}, Stats: $stats")

                            if (reportAttributes?.status == "completed") {
                                val maliciousCount = stats?.malicious ?: 0
                                val suspiciousCount = stats?.suspicious ?: 0
                                val harmlessCount = stats?.harmless ?: 0
                                val undetectedCount = stats?.undetected ?: 0
                                val totalEngines = maliciousCount + suspiciousCount + harmlessCount + undetectedCount

                                _detailedReport.value = "Scan Details: Malicious: $maliciousCount, Suspicious: $suspiciousCount, Harmless: $harmlessCount, Undetected: $undetectedCount"

                                if (totalEngines > 0) {
                                    _safetyPercentage.value = ((harmlessCount + undetectedCount) * 100) / totalEngines
                                } else {
                                    _safetyPercentage.value = null // Or 0, or handle as error/unknown
                                }

                                if (maliciousCount > 0) {
                                    _scanResult.value = "High Risk"
                                } else if (suspiciousCount > 0) {
                                    _scanResult.value = "Suspicious"
                                } else {
                                    _scanResult.value = "Safe"
                                }
                                
                                val threats = reportAttributes.threatNames?.joinToString(", ")
                                val currentScanResult = _scanResult.value ?: ""
                                if (!threats.isNullOrEmpty()){
                                     _scanResult.value = "$currentScanResult - Detected threats: $threats"
                                } else if (maliciousCount == 0 && suspiciousCount == 0){
                                    _scanResult.value = "$currentScanResult - No specific threats detected by scanned engines."
                                } else {
                                    _scanResult.value = "$currentScanResult - Review scan details for threat information."
                                }

                            } else {
                                _scanResult.value = "Analysis pending or status: ${reportAttributes?.status}. Try again shortly."
                                _safetyPercentage.value = null
                                _detailedReport.value = "Report not yet complete. Status: ${reportAttributes?.status}"
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

    private fun isValidUrl(url: String): Boolean {
        return try {
            java.net.URL(url)
            url.startsWith("http://") || url.startsWith("https://")
        } catch (_: java.net.MalformedURLException) {
            false
        }
    }
}
