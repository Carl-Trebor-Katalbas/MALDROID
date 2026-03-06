package com.app.maldroid.homeactivities

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.app.maldroid.R
import com.app.maldroid.api.ApiClient
import com.app.maldroid.data.ScanDataManager
import com.app.maldroid.data.model.ApkScanResult
import com.app.maldroid.data.model.ScanResultItem
import com.app.maldroid.data.model.ScanResult
import com.app.maldroid.data.model.DetectedThreat
import com.app.maldroid.data.model.SecurityNote
import com.app.maldroid.data.model.SecurityWarning
import com.app.maldroid.data.model.ThreatLevel
import com.app.maldroid.data.model.ThreatType
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.ArrayList

class ScanAllApkActivity : AppCompatActivity() {

    private object ConfidenceThresholds {
        const val MALICIOUS_MEDIUM = 0.45f
        const val BENIGN_MEDIUM = 0.70f
    }

    private lateinit var scanningContainer: ConstraintLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var percentText: TextView
    private lateinit var statusTitle: TextView
    private lateinit var currentFileText: TextView
    private lateinit var cancelButton: Button
    private lateinit var pulseRing1: View
    private lateinit var pulseRing2: View
    private val pulseAnimators = mutableListOf<AnimatorSet>()

    private val scannedItems = ArrayList<ScanResultItem>()
    private var scanJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_all_apk)

        initViews()

        if (hasStoragePermission()) {
            startScan()
        } else {
            requestStoragePermission()
        }
    }

    private fun initViews() {
        scanningContainer = findViewById(R.id.scanningContainer)
        progressBar = findViewById(R.id.progressBar)
        percentText = findViewById(R.id.percentText)
        statusTitle = findViewById(R.id.statusTitle)
        currentFileText = findViewById(R.id.currentFileText)
        cancelButton = findViewById(R.id.cancelButton)

        pulseRing1 = findViewById(R.id.pulseRing1)
        pulseRing2 = findViewById(R.id.pulseRing2)

        cancelButton.setOnClickListener {
            handleCancellation()
        }
    }

    private fun startScan() {
        startPulseAnimation()
        scannedItems.clear()

        scanJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                updateUI(0, "Locating Files...", "Please wait")
                val apkFiles = findAllApks(Environment.getExternalStorageDirectory())

                if (apkFiles.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ScanAllApkActivity, "No APKs found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    return@launch
                }

                val total = apkFiles.size

                for ((index, file) in apkFiles.withIndex()) {
                    ensureActive()
                    val rawPercent = (index + 1) * 100 / total
                    val displayPercent = if (rawPercent >= 100) 95 else rawPercent

                    updateUI(displayPercent, "Analyzing ${index + 1}/$total", "Scanning: ${file.name}")

                    try {
                        val startTime = System.currentTimeMillis()
                        val scanResponse = analyzeOneApk(file)
                        val endTime = System.currentTimeMillis()
                        val duration = endTime - startTime

                        if (scanResponse != null) {
                            val prediction = scanResponse.prediction
                            val confidence = scanResponse.confidence.toFloat()
                            val label = scanResponse.label ?: "Benign"
                            val triggers = scanResponse.triggers ?: emptyList()
                            val fullScanResult = createScanResult(prediction, confidence, label, file.name, triggers)
                            val actuallySafe = fullScanResult is ScanResult.Clean
                            val uiItem = ScanResultItem(
                                appName = file.name,
                                scanTimestamp = System.currentTimeMillis(),
                                scanResult = label,
                                isSafe = actuallySafe,
                                packageName = "Unknown",
                                filePath = file.absolutePath,
                                scanDuration = duration,
                                threatDetails = triggers,
                                fullScanResult = fullScanResult
                            )
                            scannedItems.add(uiItem)

                            val historyItem = ApkScanResult(
                                fileName = file.name,
                                filePath = file.absolutePath,
                                fileSize = file.length(),
                                packageName = "Unknown",
                                versionName = "1.0",
                                versionCode = 1,
                                scanTimestamp = System.currentTimeMillis(),
                                isSafe = actuallySafe,
                                scanResult = label,
                                permissions = triggers,
                                scanDuration = duration
                            )
                            ScanDataManager.addApkScanResult(historyItem, this@ScanAllApkActivity)
                        } else {
                            val errorResult = ScanResult.Error(message = "API Error or Timeout")

                            val uiItem = ScanResultItem(
                                appName = file.name,
                                scanTimestamp = System.currentTimeMillis(),
                                scanResult = "Error",
                                isSafe = false,
                                packageName = "Unknown",
                                filePath = file.absolutePath,
                                scanDuration = duration,
                                threatDetails = emptyList(),
                                fullScanResult = errorResult
                            )
                            scannedItems.add(uiItem)
                        }
                    } catch (e: Exception) {
                        Log.e("ScanFileError", "Failed to scan ${file.name}: ${e.message}")
                    }
                }
                updateUI(100, "Scan Complete", "Finalizing...")
                delay(300)

            } catch (e: CancellationException) {
                Log.d("ScanAll", "Scan cancelled by user")
                return@launch
            } catch (e: Exception) {
                Log.e("ScanAll", "Global Error: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    goToResultsActivity()
                }
            }
        }
    }

    private fun goToResultsActivity() {
        stopPulseAnimation()

        val intent = Intent(this, ScanAllApkResultActivity::class.java)
        intent.putParcelableArrayListExtra("SCAN_RESULTS", ArrayList(scannedItems))
        startActivity(intent)
        finish()
    }

    private fun handleCancellation() {
        scanJob?.cancel()
        stopPulseAnimation()

        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            try { ApiClient.api.cancelScan() } catch (e: Exception) { /* Ignore */ }
        }

        Toast.makeText(this, "Scan Cancelled", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun startPulseAnimation() {
        stopPulseAnimation()
        animateRing(pulseRing1, 0)
        animateRing(pulseRing2, 900)
    }

    private fun animateRing(target: View, startDelay: Long) {
        val scaleX = ObjectAnimator.ofFloat(target, "scaleX", 1f, 1.45f)
        val scaleY = ObjectAnimator.ofFloat(target, "scaleY", 1f, 1.45f)
        val alpha = ObjectAnimator.ofFloat(target, "alpha", 0.5f, 0f)

        val animatorSet = AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 1500
            this.startDelay = startDelay
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!isFinishing) start()
                }
            })
            start()
        }
        pulseAnimators.add(animatorSet)
    }

    private fun stopPulseAnimation() {
        pulseAnimators.forEach { it.cancel() }
        pulseAnimators.clear()
        pulseRing1.alpha = 0f
        pulseRing2.alpha = 0f
    }

    private suspend fun updateUI(percent: Int, status: String, fileInfo: String) {
        withContext(Dispatchers.Main) {
            progressBar.progress = percent
            percentText.text = "$percent%"
            statusTitle.text = status
            currentFileText.text = fileInfo
        }
    }

    private suspend fun analyzeOneApk(file: File): com.app.maldroid.api.AnalyzeResponse? {
        return try {
            val requestFile = file.asRequestBody("application/vnd.android.package-archive".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("apkFile", file.name, requestFile)
            ApiClient.api.analyzeApk(body)
        } catch (e: Exception) {
            null
        }
    }

    private fun findAllApks(dir: File): List<File> {
        val apkList = mutableListOf<File>()
        try {
            val files = dir.listFiles() ?: return emptyList()
            for (file in files) {
                if (file.isDirectory && !file.name.startsWith(".") && file.name != "Android") {
                    apkList.addAll(findAllApks(file))
                } else if (file.name.endsWith(".apk", ignoreCase = true)) {
                    apkList.add(file)
                }
            }
        } catch (e: Exception) { }
        return apkList
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, 100)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivityForResult(intent, 100)
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 100)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPulseAnimation()
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
                    warnings = listOf(SecurityWarning("File Integrity", "APK may be corrupted", ThreatLevel.HIGH))
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
                    DetectedThreat(ThreatType.KNOWN_MALWARE, ThreatLevel.CRITICAL, "Critical-confidence malware detection", "Definitely malicious with ${"%.1f".format(confidence * 100)}% certainty"),
                    DetectedThreat(ThreatType.SUSPICIOUS_BEHAVIOR, ThreatLevel.HIGH, "Multiple advanced malicious behaviors", "Exhibits sophisticated attack patterns")
                ))
            }
            confidence >= 0.60f -> {
                threats.addAll(listOf(
                    DetectedThreat(ThreatType.KNOWN_MALWARE, ThreatLevel.HIGH, "High-confidence malware detection", "Strong malicious indicators with ${"%.1f".format(confidence * 100)}% confidence"),
                    DetectedThreat(ThreatType.DANGEROUS_PERMISSIONS, ThreatLevel.MEDIUM, "Suspicious permission combinations", "Requests dangerous system access")
                ))
            }
            confidence >= 0.50f -> {
                threats.addAll(listOf(
                    DetectedThreat(ThreatType.SUSPICIOUS_BEHAVIOR, ThreatLevel.HIGH, "Likely malicious behavior patterns", "Strong indicators of malicious intent"),
                    DetectedThreat(ThreatType.PRIVACY_RISK, ThreatLevel.MEDIUM, "Potential data exfiltration risk", "May access and transmit sensitive data")
                ))
            }
            else -> {
                threats.add(DetectedThreat(ThreatType.SUSPICIOUS_BEHAVIOR, ThreatLevel.MEDIUM, "Malicious patterns detected", "Exhibits suspicious characteristics"))
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
                cleanName = cleanName.replace("android.permission.", "").replace("android.hardware.", "").replace("_", " ").trim()
                SecurityWarning("Suspicious Feature", cleanName, ThreatLevel.MEDIUM)
            }
        }
        return listOf(SecurityWarning("Unknown Risk", "AI Classification is ambiguous", ThreatLevel.LOW))
    }
}