package com.app.maldroid.homeactivities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.app.maldroid.MainActivity
import com.app.maldroid.R
import com.app.maldroid.data.ScanDataManager
import com.app.maldroid.data.model.ScanResult

class ApkResultsAllGood : AppCompatActivity() {

    private lateinit var scanSummaryText: TextView
    private lateinit var apkFileNameText: TextView
    private lateinit var threatStatusText: TextView
    private lateinit var scanDurationText: TextView
    private lateinit var permissionsText: TextView
    private lateinit var apiCallsText: TextView
    private lateinit var codePatternsText: TextView
    private lateinit var networkLinksText: TextView

    private lateinit var backButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_apk_results_all_good)
        backButton = findViewById(R.id.backButton)
        scanSummaryText = findViewById(R.id.scanSummaryText)
        apkFileNameText = findViewById(R.id.apkFileNameText)
        threatStatusText = findViewById(R.id.threatStatusText)
        scanDurationText = findViewById(R.id.scanDurationText)
        permissionsText = findViewById(R.id.permissionsText)
        apiCallsText = findViewById(R.id.apiCallsText)
        codePatternsText = findViewById(R.id.codePatternsText)
        networkLinksText = findViewById(R.id.networkLinksText)

        val scanAgainBtn = findViewById<Button>(R.id.scanAgainBtn)
        scanAgainBtn.setOnClickListener {
            val intent = Intent(this, FileSelection::class.java)
            startActivity(intent)
            finish()
        }

        backButton.setOnClickListener {
            finish()
        }
        displayCurrentScanResults()
        displayScanDuration()
    }

    @SuppressLint("SetTextI18n")
    private fun displayScanDuration() {
        var durationMs = intent.getLongExtra("SCAN_DURATION", 0L)
        if (durationMs == 0L) {
            durationMs = intent.getIntExtra("SCAN_DURATION", 0).toLong()
        }

        val durationText = if (durationMs >= 1000) {
            String.format("%.2f seconds", durationMs / 1000.0)
        } else if (durationMs > 0) {
            "$durationMs ms"
        } else {
            "--"
        }

        scanDurationText.text = "Scan Duration: $durationText"
    }

    @SuppressLint("SetTextI18n")
    private fun displayCurrentScanResults() {
        val fileName = intent.getStringExtra("FILE_NAME") ?: "Unknown File"
        apkFileNameText.text = fileName

        val scanResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("SCAN_RESULT", ScanResult::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("SCAN_RESULT") as? ScanResult
        }

        if (scanResult is ScanResult.Clean) {
            scanSummaryText.text = "APK IS CLEAN"
            threatStatusText.text = "No threats detected in this APK"
            categorizeAndDisplayTriggers(scanResult.triggers)

        } else {
            val stringExtra = intent.getStringExtra("SCAN_RESULT")
            if (!stringExtra.isNullOrEmpty()) {
                scanSummaryText.text = "APK IS CLEAN"
                threatStatusText.text = stringExtra

                permissionsText.text = "• View live scan for full list"
                apiCallsText.text = "• View live scan for full list"
                codePatternsText.text = "• View live scan for full list"
                networkLinksText.text = "• View live scan for full list"
            } else {
                displayFallbackResults()
            }
        }
    }

    private fun categorizeAndDisplayTriggers(triggers: List<String>) {
        val permissions = mutableListOf<String>()
        val apiCalls = mutableListOf<String>()
        val codePatterns = mutableListOf<String>()
        val privacy = mutableListOf<String>()

        triggers.forEach { triggerRaw ->
            val lowerTrigger = triggerRaw.lowercase()
            val cleanName = triggerRaw.replace("android.permission.", "")
                .replace("android.hardware.", "")
                .replace(Regex("\\(AI Score:.*\\)"), "")
                .trim()

            when {
                "permission" in lowerTrigger || "send_sms" in lowerTrigger || "receive_boot" in lowerTrigger -> permissions.add(cleanName)
                "camera" in lowerTrigger || "record" in lowerTrigger || "location" in lowerTrigger -> privacy.add(cleanName)
                "mount" in lowerTrigger || "su" in lowerTrigger || "chmod" in lowerTrigger || "dexclassloader" in lowerTrigger || "loadlibrary" in lowerTrigger -> codePatterns.add(cleanName)
                else -> apiCalls.add(cleanName)
            }
        }

        permissionsText.text = if (permissions.isNotEmpty()) permissions.joinToString("\n") { "• $it" } else "• None detected"
        apiCallsText.text = if (apiCalls.isNotEmpty()) apiCalls.joinToString("\n") { "• $it" } else "• None detected"
        codePatternsText.text = if (codePatterns.isNotEmpty()) codePatterns.joinToString("\n") { "• $it" } else "• None detected"
        networkLinksText.text = if (privacy.isNotEmpty()) privacy.joinToString("\n") { "• $it" } else "• None detected"
    }

    @SuppressLint("SetTextI18n")
    private fun displayFallbackResults() {
        val apkResults = ScanDataManager.getApkScanResults()
        val latestApk = apkResults.lastOrNull()

        if (latestApk != null) {
            apkFileNameText.text = latestApk.fileName
            scanSummaryText.text = "APK IS CLEAN"
            threatStatusText.text = "No threats found in recent scan"
        } else {
            scanSummaryText.text = "Scan Results"
            apkFileNameText.text = "Unknown"
            threatStatusText.text = "No scan data available"
        }

        permissionsText.text = "• Not available in history"
        apiCallsText.text = "• Not available in history"
        codePatternsText.text = "• Not available in history"
        networkLinksText.text = "• Not available in history"
    }
}