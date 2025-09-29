package com.example.gofish.util

data class PatternAnalysisResult(
    val isSuspicious: Boolean,
    val reasons: List<String>,
    val riskScore: Float, // 0.0 (low) to 1.0 (high)
    val riskLevelText: String
)

object PatternLinkDetector {

    private const val MAX_SCORE_THRESHOLD = 10 // Points at which risk is considered 100%

    fun analyzeUrl(url: String): PatternAnalysisResult {
        val reasons = mutableListOf<String>()
        var suspiciousPoints = 0

        if (url.isBlank()) {
            return PatternAnalysisResult(false, listOf("URL is empty."), 0f, "NO RISK")
        }

        // 1. Check for HTTPS (simple check)
        if (!url.startsWith("https://", ignoreCase = true)) {
            reasons.add("URL does not use HTTPS.")
            suspiciousPoints += 2
        }

        // 2. Check for presence of '@' in domain part (before path)
        val domainPart = url.substringAfter("://").substringBefore("/")
        if (domainPart.contains("@")) {
            reasons.add("URL contains '@' symbol in domain name part.")
            suspiciousPoints += 3
        }

        // 3. Check for IP address as domain
        val ipRegex = Regex("""^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$""")
        if (ipRegex.matches(domainPart)) {
            reasons.add("URL uses an IP address instead of a domain name.")
            suspiciousPoints += 3
        }

        // 4. Check for excessive hyphens in domain
        val hyphenCount = domainPart.count { it == '-' }
        if (hyphenCount > 2) {
            reasons.add("Excessive hyphens in domain name ($hyphenCount).")
            suspiciousPoints += (hyphenCount - 2) // More hyphens, more points
        }

        // 5. Check for unusually long URL
        if (url.length > 100) {
            reasons.add("Unusually long URL (${url.length} characters).")
            suspiciousPoints += 2
        } else if (url.length > 75) {
            reasons.add("Long URL (${url.length} characters).")
            suspiciousPoints += 1
        }
        
        // 6. Basic check for common phishing keywords (use with caution)
        // val phishingKeywords = listOf("login", "verify", "account", "update", "secure", "bank", "password")
        // phishingKeywords.forEach { keyword ->
        //     if (url.contains(keyword, ignoreCase = true)) {
        //         reasons.add("URL contains potentially suspicious keyword: '$keyword'.")
        //         suspiciousPoints += 1 
        //     }
        // }

        // 7. Check for multiple subdomains (e.g. more than 3 dots in domain part for a typical .com/.org)
        val dotCountInDomain = domainPart.count { it == '.' }
        if (dotCountInDomain > 3) { // e.g., a.b.c.d.com (4 dots is suspicious)
            reasons.add("URL has multiple subdomains ($dotCountInDomain dots in domain part).")
            suspiciousPoints += (dotCountInDomain - 3)
        }
        
        // 8. No domain part (e.g. "http://", "https://")
        if (domainPart.isBlank() && url.startsWith("http")) {
            reasons.add("URL has no domain part specified.")
            suspiciousPoints += 3
        }

        val riskScore = kotlin.math.min(suspiciousPoints.toFloat() / MAX_SCORE_THRESHOLD, 1.0f)
        val isSuspicious = riskScore > 0.3 // If score is over 30%, consider suspicious

        val riskLevelText = when {
            riskScore >= 0.7 -> "HIGH RISK"
            riskScore >= 0.3 -> "MEDIUM RISK"
            riskScore > 0   -> "LOW RISK"
            else            -> "NO APPARENT RISK"
        }
        if (isSuspicious && reasons.isEmpty()){
             reasons.add("General heuristics suggest potential risk.")
        }
        if (!isSuspicious && reasons.isEmpty() && url.isNotBlank()){
            reasons.add("URL appears to be safe based on basic patterns.")
        }


        return PatternAnalysisResult(isSuspicious, reasons.distinct(), riskScore, riskLevelText)
    }
}
