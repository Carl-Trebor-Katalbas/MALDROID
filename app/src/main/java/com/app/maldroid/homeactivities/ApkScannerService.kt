package com.app.maldroid.homeactivities

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.app.maldroid.data.model.ScanResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import android.util.Log
import com.app.maldroid.data.model.DetectedThreat
import com.app.maldroid.data.model.SecurityNote
import com.app.maldroid.data.model.SecurityWarning
import com.app.maldroid.data.model.ThreatLevel
import com.app.maldroid.data.model.ThreatType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ApkScannerService : Service() {

    companion object {
        const val ACTION_SCAN_APK = "ACTION_SCAN_APK"
        const val ACTION_SCAN_PROGRESS = "ACTION_SCAN_PROGRESS"
        const val ACTION_SCAN_COMPLETE = "ACTION_SCAN_COMPLETE"

        const val EXTRA_APK_PATH = "EXTRA_APK_PATH"
        const val EXTRA_PROGRESS = "EXTRA_PROGRESS"
        const val EXTRA_CURRENT_STEP = "EXTRA_CURRENT_STEP"
        const val EXTRA_SCAN_RESULT = "EXTRA_SCAN_RESULT"

        const val TAG = "ApkScannerService"
        private val BASE_URL = "http://192.168.1.170:5000"
    }

    private object ConfidenceThresholds {
        const val MALICIOUS_MEDIUM = 0.45f
        const val BENIGN_MEDIUM = 0.70f
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SCAN_APK -> {
                val apkPath = intent.getStringExtra(EXTRA_APK_PATH)
                apkPath?.let { scanApkFile(it) }
            }
        }
        return START_NOT_STICKY
    }

    @SuppressLint("SuspiciousIndentation")
    private fun scanApkFile(apkPath: String) {
        serviceScope.launch {
            try {
                updateProgress(10, "Connecting to AI Model...")
                Log.d(TAG, "Attempting to connect to backend at $BASE_URL")

                val apkFile = File(apkPath)
                if (!apkFile.exists()) {
                    sendScanResult(ScanResult.Error("APK file not found"))
                    return@launch
                }
                updateProgress(30, "Uploading APK...")

                val requestBody = apkFile.asRequestBody("application/vnd.android.package-archive".toMediaType())
                val apkPart = MultipartBody.Part.createFormData("apkFile", apkFile.name, requestBody)
                updateProgress(50, "Analyzing with Random Forest...")

                val response = com.app.maldroid.api.ApiClient.api.analyzeApk(apkPart)
                Log.i(TAG, "Connected to backend successfully.")
                Log.d(TAG, "Backend response: $response")

                val prediction = response.prediction
                val confidence = response.confidence.toFloat()
                val label = response.label
                val triggers = response.triggers ?: emptyList()

                Log.d(TAG, "Result: $label ($confidence), Triggers: $triggers")

                val scanResult = createScanResult(prediction, confidence, label, apkFile.name, triggers)
                sendScanResult(scanResult)

            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Failed to connect to backend: ${e.message}")
                e.printStackTrace()
                sendScanResult(ScanResult.Error("Network error: ${e.message}"))

            } finally {
                stopSelf()
            }
        }
    }

    private fun createScanResult(
        prediction: Int,
        confidence: Float,
        label: String,
        fileName: String,
        triggers: List<String>
    ): ScanResult {
        val isPossibleCorruption = confidence < 0.7f && (label.equals("suspicious", ignoreCase = true))

        return when {
            isPossibleCorruption -> {
                ScanResult.Suspicious(
                    threatLevel = ThreatLevel.HIGH,
                    confidence = confidence,
                    warnings = listOf(
                        SecurityWarning("File Integrity", "APK may be corrupted", ThreatLevel.HIGH)
                    )
                )
            }

            confidence >= ConfidenceThresholds.MALICIOUS_MEDIUM &&
                    (prediction == 1 || label.equals("malicious", ignoreCase = true)) -> {

                val threatLevel = when {
                    confidence >= 0.90f -> ThreatLevel.CRITICAL
                    confidence >= 0.65f -> ThreatLevel.HIGH
                    else -> ThreatLevel.MEDIUM
                }

                val aiThreats = if (triggers.isNotEmpty()) {
                    mapTriggersToThreats(triggers, confidence)
                } else {
                    generateThreatsBasedOnConfidence(confidence)
                }

                ScanResult.Malicious(
                    threatLevel = threatLevel,
                    confidence = confidence,
                    detectedThreats = aiThreats,
                    recommendations = getMaliciousRecommendations(confidence)
                )
            }

            confidence >= ConfidenceThresholds.BENIGN_MEDIUM &&
                    (prediction == 0 || label.equals("benign", ignoreCase = true)) -> {
                Log.d(TAG, "APK is Clean. Packing triggers to send to UI: $triggers")

                ScanResult.Clean(
                    confidence = confidence,
                    notes = getBenignNotes(confidence),
                    triggers = triggers
                )
            }

            else -> {
                val threatLevel = if (confidence < 0.45f) ThreatLevel.MEDIUM else ThreatLevel.HIGH
                ScanResult.Suspicious(
                    threatLevel = threatLevel,
                    confidence = confidence,
                    warnings = getSuspiciousWarnings(confidence, triggers)
                )
            }
        }
    }

    private fun mapTriggersToThreats(triggers: List<String>, confidence: Float): List<DetectedThreat> {
        return triggers.map { triggerRaw ->
            val lowerTrigger = triggerRaw.lowercase()

            val cleanName = triggerRaw.replace("android.permission.", "")
                .replace("android.hardware.", "")
                .replace(Regex("\\(AI Score:.*\\)"), "")
                .trim()

            val (type, desc) = when {
                "permission" in lowerTrigger || "send_sms" in lowerTrigger || "receive_boot" in lowerTrigger ->
                    Pair(ThreatType.DANGEROUS_PERMISSIONS, "Risky Permission")

                "camera" in lowerTrigger || "record" in lowerTrigger || "location" in lowerTrigger ->
                    Pair(ThreatType.PRIVACY_RISK, "Spyware Potential")

                "mount" in lowerTrigger || "su" in lowerTrigger || "chmod" in lowerTrigger ->
                    Pair(ThreatType.SUSPICIOUS_BEHAVIOR, "System Tampering")

                "dexclassloader" in lowerTrigger || "loadlibrary" in lowerTrigger ->
                    Pair(ThreatType.KNOWN_MALWARE, "Code Loading/Hiding")

                else -> Pair(ThreatType.SUSPICIOUS_BEHAVIOR, "Suspicious Feature")
            }

            DetectedThreat(
                type = type,
                severity = if (confidence > 0.8) ThreatLevel.HIGH else ThreatLevel.MEDIUM,
                description = cleanName,
                details = desc
            )
        }
    }

    private fun generateThreatsBasedOnConfidence(confidence: Float): List<DetectedThreat> {
        val threats = mutableListOf<DetectedThreat>()

        when {
            confidence >= 0.90f -> {
                threats.addAll(listOf(
                    DetectedThreat(
                        type = ThreatType.KNOWN_MALWARE,
                        severity = ThreatLevel.CRITICAL,
                        description = "Critical-confidence malware detection",
                        details = "Definitely malicious with ${"%.1f".format(confidence * 100)}% certainty"
                    ),
                    DetectedThreat(
                        type = ThreatType.SUSPICIOUS_BEHAVIOR,
                        severity = ThreatLevel.HIGH,
                        description = "Multiple advanced malicious behaviors",
                        details = "Exhibits sophisticated attack patterns"
                    )
                ))
            }
            confidence >= 0.60f -> {
                threats.addAll(listOf(
                    DetectedThreat(
                        type = ThreatType.KNOWN_MALWARE,
                        severity = ThreatLevel.HIGH,
                        description = "High-confidence malware detection",
                        details = "Strong malicious indicators with ${"%.1f".format(confidence * 100)}% confidence"
                    ),
                    DetectedThreat(
                        type = ThreatType.DANGEROUS_PERMISSIONS,
                        severity = ThreatLevel.MEDIUM,
                        description = "Suspicious permission combinations",
                        details = "Requests dangerous system access"
                    )
                ))
            }
            confidence >= 0.50f -> {
                threats.addAll(listOf(
                    DetectedThreat(
                        type = ThreatType.SUSPICIOUS_BEHAVIOR,
                        severity = ThreatLevel.HIGH,
                        description = "Likely malicious behavior patterns",
                        details = "Strong indicators of malicious intent"
                    ),
                    DetectedThreat(
                        type = ThreatType.PRIVACY_RISK,
                        severity = ThreatLevel.MEDIUM,
                        description = "Potential data exfiltration risk",
                        details = "May access and transmit sensitive data"
                    )
                ))
            }
            else -> {
                threats.add(
                    DetectedThreat(
                        type = ThreatType.SUSPICIOUS_BEHAVIOR,
                        severity = ThreatLevel.MEDIUM,
                        description = "Malicious patterns detected",
                        details = "Exhibits suspicious characteristics"
                    )
                )
            }
        }

        return threats
    }

    private fun getMaliciousRecommendations(confidence: Float): List<String> {
        return listOf("🚫 Uninstall Immediately", "🔍 Scan device for other threats", "⚠️ Check app permissions")
    }

    private fun getBenignNotes(confidence: Float): List<SecurityNote> {
        return listOf(SecurityNote("Clean", "No threats found."))
    }

    private fun getSuspiciousWarnings(confidence: Float, triggers: List<String>): List<SecurityWarning> {
        if (triggers.isNotEmpty()) {
            return triggers.map { triggerRaw ->
                var cleanName = triggerRaw.substringBefore(" (")

                cleanName = cleanName
                    .replace("android.permission.", "")
                    .replace("android.hardware.", "")
                    .replace("_", " ")
                    .trim()

                SecurityWarning(
                    "Suspicious Feature",
                    cleanName,
                    ThreatLevel.MEDIUM
                )
            }
        }
        return listOf(
            SecurityWarning("Unknown Risk", "AI Classification is ambiguous", ThreatLevel.LOW)
        )
    }

    private fun updateProgress(progress: Int, step: String) {
        val intent = Intent(ACTION_SCAN_PROGRESS).apply {
            putExtra(EXTRA_PROGRESS, progress)
            putExtra(EXTRA_CURRENT_STEP, step)
        }
        intent.setPackage(packageName)
        sendBroadcast(intent)
    }

    private fun sendScanResult(scanResult: ScanResult) {
        val intent = Intent(ACTION_SCAN_COMPLETE).apply {
            putExtra(EXTRA_SCAN_RESULT, scanResult)
        }
        intent.setPackage(packageName)
        sendBroadcast(intent)
    }
}