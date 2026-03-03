package com.app.maldroid.data.model

import java.io.Serializable

sealed class ScanResult : Serializable {
    data class Clean(
        val confidence: Float,
        val notes: List<SecurityNote> = emptyList(),
        val permissions: List<String> = emptyList(),
        val triggers: List<String> = emptyList()
    ) : ScanResult()

    data class Suspicious(
        val threatLevel: ThreatLevel,
        val confidence: Float,
        val warnings: List<SecurityWarning> = emptyList()
    ) : ScanResult()

    data class Malicious(
        val threatLevel: ThreatLevel,
        val confidence: Float,
        val detectedThreats: List<DetectedThreat> = emptyList(),
        val recommendations: List<String> = emptyList()
    ) : ScanResult()



    data class Error(val message: String) : ScanResult()
}

enum class ThreatLevel { LOW, MEDIUM, HIGH, CRITICAL }

data class DetectedThreat(
    val type: ThreatType,
    val severity: ThreatLevel,
    val description: String,
    val details: String? = null
) : Serializable

enum class ThreatType {
    DANGEROUS_PERMISSIONS,
    SUSPICIOUS_BEHAVIOR,
    KNOWN_MALWARE,
    PRIVACY_RISK,
    SYSTEM_MODIFICATION
}

data class SecurityNote(
    val title: String,
    val description: String
) : Serializable

data class SecurityWarning(
    val title: String,
    val description: String,
    val severity: ThreatLevel
) : Serializable