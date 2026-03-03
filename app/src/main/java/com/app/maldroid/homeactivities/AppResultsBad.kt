//package com.app.maldroid.homeactivities
//
//import android.annotation.SuppressLint
//import android.content.Intent
//import android.net.Uri
//import android.os.Bundle
//import android.widget.Button
//import android.widget.ImageButton
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.ContextCompat
//import com.app.maldroid.R
//import com.app.maldroid.data.model.ScanResult
//import com.app.maldroid.data.ScanDataManager
//
//class AppResultsBad : AppCompatActivity() {
//
//    private lateinit var maliciousAlertText: TextView
//    private lateinit var scanDurationText: TextView
//    private lateinit var appNameText: TextView
//    private lateinit var threatDescriptionText: TextView
//    private lateinit var permissionsText: TextView
//    private lateinit var behaviorPatternsText: TextView
//    private lateinit var codeAnalysisText: TextView
//    private lateinit var networkActivityText: TextView
//    private lateinit var backButton: ImageButton
//
//    private var isWaitingForUninstall = false
//    private var currentPackageName: String? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_app_results_bad)
//
//        currentPackageName = intent.getStringExtra("PACKAGE_NAME")
//
//        if (currentPackageName == null) {
//            currentPackageName = ScanDataManager.getScannedApps().lastOrNull()?.packageName
//        }
//
//        initializeViews()
//        setupClickListeners()
//        displayScanResults()
//    }
//
//    override fun onResume() {
//        super.onResume()
//        if (isWaitingForUninstall && currentPackageName != null) {
//            if (!isAppInstalled(currentPackageName!!)) {
//                showSuccessDialog()
//                isWaitingForUninstall = false
//            }
//        }
//    }
//
//    private fun initializeViews() {
//        maliciousAlertText = findViewById(R.id.maliciousAlertText)
//        scanDurationText = findViewById(R.id.scanDurationText)
//        appNameText = findViewById(R.id.appNameText)
//        threatDescriptionText = findViewById(R.id.threatDescriptionText)
//        permissionsText = findViewById(R.id.permissionsText)
//        behaviorPatternsText = findViewById(R.id.behaviorPatternsText)
//        codeAnalysisText = findViewById(R.id.codeAnalysisText)
//        networkActivityText = findViewById(R.id.networkActivityText)
//        backButton = findViewById(R.id.backButton)
//    }
//
//    private fun setupClickListeners() {
//        val scanAgainBtn = findViewById<Button>(R.id.scanAgainBtn)
//        val uninstallAppBtn = findViewById<Button>(R.id.uninstallAppBtn)
//
//        scanAgainBtn.setOnClickListener {
//            val intent = Intent(this, AppSelection::class.java)
//            startActivity(intent)
//            finish()
//        }
//
//        backButton.setOnClickListener {
//            val intent = Intent(this, AppSelection::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
//            startActivity(intent)
//            finish()
//        }
//
//        uninstallAppBtn.setOnClickListener {
//            if (currentPackageName != null) {
//                isWaitingForUninstall = true
//                val intent = Intent(Intent.ACTION_DELETE)
//                intent.data = Uri.parse("package:$currentPackageName")
//                startActivity(intent)
//            } else {
//                Toast.makeText(this, "Could not identify app package", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    private fun showSuccessDialog() {
//        AlertDialog.Builder(this)
//            .setTitle("Uninstall Successful")
//            .setMessage("The threat has been removed from your device.")
//            .setPositiveButton("OK") { _, _ ->
//                val intent = Intent(this, AppSelection::class.java)
//                startActivity(intent)
//                finish()
//            }
//            .setCancelable(false)
//            .show()
//    }
//
//    private fun isAppInstalled(packageName: String): Boolean {
//        return try {
//            packageManager.getPackageInfo(packageName, 0)
//            true
//        } catch (e: Exception) {
//            false
//        }
//    }
//
//    @SuppressLint("SetTextI18n")
//    private fun displayScanResults() {
//        val appResults = ScanDataManager.getScannedApps()
//        val latestApp = appResults.find { it.packageName == currentPackageName } ?: appResults.lastOrNull()
//        val scanResult = intent.getSerializableExtra("SCAN_RESULT") as? ScanResult
//
//        if (latestApp != null && scanResult != null) {
//            appNameText.text = latestApp.name
//            threatDescriptionText.text = "Threats found in ${latestApp.name}"
//            scanDurationText.text = "Scan Duration: 12 seconds"
//
//            // --- FIX: REMOVED SAVE LOGIC HERE ---
//            // saveAppToScanHistory(latestApp, scanResult)  <-- THIS WAS THE CAUSE OF DUPLICATES
//
//            when (scanResult) {
//                is ScanResult.Malicious -> {
//                    maliciousAlertText.text = "MALICIOUS DETECTED"
//                    maliciousAlertText.setTextColor(ContextCompat.getColor(this, R.color.red))
//                    displayMaliciousThreats(scanResult, latestApp.name)
//                }
//                is ScanResult.Suspicious -> {
//                    maliciousAlertText.text = "SUSPICIOUS ACTIVITY DETECTED"
//                    maliciousAlertText.setTextColor(ContextCompat.getColor(this, R.color.orange))
//                    displaySuspiciousThreats(scanResult, latestApp.name)
//                }
//                is ScanResult.Error -> {
//                    maliciousAlertText.text = "SCAN ERROR"
//                    maliciousAlertText.setTextColor(ContextCompat.getColor(this, R.color.red))
//                    displayError(scanResult)
//                }
//                else -> {
//                    maliciousAlertText.text = "POTENTIAL THREAT DETECTED"
//                    maliciousAlertText.setTextColor(ContextCompat.getColor(this, R.color.orange))
//                    displayGenericThreats(latestApp.name)
//                }
//            }
//        } else {
//            displayDemoData()
//        }
//    }
//
//    // --- REMOVED THE saveAppToScanHistory FUNCTION ENTIRELY ---
//
//    private fun displayMaliciousThreats(scanResult: ScanResult.Malicious, appName: String) {
//        permissionsText.text = if (scanResult.detectedThreats.any { it.type.name.contains("PERMISSION", ignoreCase = true) }) {
//            scanResult.detectedThreats
//                .filter { it.type.name.contains("PERMISSION", ignoreCase = true) }
//                .joinToString("\n") { it.description }
//        } else {
//            "Dangerous permissions detected\nREAD_SMS\nACCESS_FINE_LOCATION\nCAMERA"
//        }
//        behaviorPatternsText.text = "Suspicious behavior patterns\nBackground data collection\nExcessive resource usage"
//        codeAnalysisText.text = "Malicious code patterns\nCode obfuscation\nDynamic class loading"
//        networkActivityText.text = "Suspicious network activity\nExternal server communication"
//    }
//
//    private fun displaySuspiciousThreats(scanResult: ScanResult.Suspicious, appName: String) {
//        permissionsText.text = scanResult.warnings.filter { it.title.contains("Permission", ignoreCase = true) }.joinToString("\n") { it.description }.ifEmpty { "Excessive permissions requested" }
//        behaviorPatternsText.text = "Unusual app behavior\nPotential privacy risks"
//        codeAnalysisText.text = "Suspicious code structure"
//        networkActivityText.text = "Unusual network behavior"
//    }
//
//    @SuppressLint("SetTextI18n")
//    private fun displayError(scanResult: ScanResult.Error) {
//        permissionsText.text = "Scan failed"
//        behaviorPatternsText.text = "Unable to analyze"
//        codeAnalysisText.text = "ML processing error"
//        networkActivityText.text = scanResult.message
//    }
//
//    @SuppressLint("SetTextI18n")
//    private fun displayGenericThreats(appName: String) {
//        permissionsText.text = "Potential permission abuse"
//        behaviorPatternsText.text = "Suspicious app behavior"
//        codeAnalysisText.text = "Questionable code patterns"
//        networkActivityText.text = "Unverified network endpoints"
//    }
//@SuppressLint("SetTextI18n")
//    private fun displayDemoData() {
//        appNameText.text = "Unknown App"
//        maliciousAlertText.text = "MALICIOUS DETECTED"
//        permissionsText.text = "READ_SMS\nACCESS_FINE_LOCATION"
//    }
//}