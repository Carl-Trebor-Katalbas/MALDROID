//package com.app.maldroid.homeactivities
//
//import android.annotation.SuppressLint
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.os.Build
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//import android.widget.Button
//import android.widget.ProgressBar
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.ContextCompat
//import com.app.maldroid.R
//import com.app.maldroid.data.model.ScanResult
//
//class AppScanningProgress : AppCompatActivity() {
//
//    // UI Components matching your XML
//    private lateinit var circularProgressBar: ProgressBar
//    private lateinit var progressPercentageText: TextView
//    private lateinit var currentStepText: TextView
//    private lateinit var appNameText: TextView
//    private lateinit var cancelButton: Button
//
//    // Receiver to listen for service updates
//    private val scanReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent?) {
//            when (intent?.action) {
//                AppScannerService.ACTION_SCAN_PROGRESS -> {
//                    val progress = intent.getIntExtra(AppScannerService.EXTRA_PROGRESS, 0)
//                    val step = intent.getStringExtra(AppScannerService.EXTRA_CURRENT_STEP) ?: "Processing..."
//                    updateUI(progress, step)
//                }
//                AppScannerService.ACTION_SCAN_COMPLETE -> {
//                    val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                        intent.getSerializableExtra(AppScannerService.EXTRA_SCAN_RESULT, ScanResult::class.java)
//                    } else {
//                        @Suppress("DEPRECATION")
//                        intent.getSerializableExtra(AppScannerService.EXTRA_SCAN_RESULT) as? ScanResult
//                    }
//
//                    if (result != null) {
//                        handleScanResult(result)
//                    } else {
//                        currentStepText.text = "Error: Could not retrieve results."
//                    }
//                }
//            }
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_app_scanning_progress)
//
//        // 1. Bind Views to your XML IDs
//        circularProgressBar = findViewById(R.id.circularProgressBar)
//        progressPercentageText = findViewById(R.id.progressPercentageText)
//        currentStepText = findViewById(R.id.currentStepText)
//        appNameText = findViewById(R.id.appNameText)
//        cancelButton = findViewById(R.id.cancel_button)
//
//        // 2. Setup Data
//        val packageName = intent.getStringExtra("PACKAGE_NAME")
//        val appName = intent.getStringExtra("APP_NAME") ?: "Unknown App"
//
//        appNameText.text = "Scanning: $appName"
//
//        // 3. Setup Cancel Button
//        cancelButton.setOnClickListener {
//            // Stop the background service
//            stopService(Intent(this, AppScannerService::class.java))
//            Toast.makeText(this, "Scan Cancelled", Toast.LENGTH_SHORT).show()
//            finish() // Go back
//        }
//
//        // 4. Start the Service
//        if (packageName != null) {
//            startScanningService(packageName)
//        } else {
//            Toast.makeText(this, "Error: No app selected", Toast.LENGTH_SHORT).show()
//            finish()
//        }
//    }
//
//    private fun startScanningService(packageName: String) {
//        currentStepText.text = "Initializing Scanner..."
//        val serviceIntent = Intent(this, AppScannerService::class.java).apply {
//            action = AppScannerService.ACTION_SCAN_APP
//            putExtra(AppScannerService.EXTRA_PACKAGE_NAME, packageName)
//        }
//        startService(serviceIntent)
//    }
//
//    @SuppressLint("UnspecifiedRegisterReceiverFlag")
//    override fun onStart() {
//        super.onStart()
//        val filter = IntentFilter().apply {
//            addAction(AppScannerService.ACTION_SCAN_PROGRESS)
//            addAction(AppScannerService.ACTION_SCAN_COMPLETE)
//        }
//
//        // --- FIXED: Use RECEIVER_NOT_EXPORTED for Android 14+ ---
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            registerReceiver(scanReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
//        } else {
//            registerReceiver(scanReceiver, filter)
//        }
//    }
//
//    override fun onStop() {
//        super.onStop()
//        try {
//            unregisterReceiver(scanReceiver)
//        } catch (e: Exception) {
//            // Already unregistered
//        }
//    }
//
//    private fun updateUI(progress: Int, step: String) {
//        circularProgressBar.progress = progress
//        progressPercentageText.text = "$progress%"
//        currentStepText.text = step
//    }
//
//    private fun handleScanResult(result: ScanResult) {
//        circularProgressBar.progress = 100
//        progressPercentageText.text = "100%"
//        currentStepText.text = "Analysis Complete!"
//
//        Handler(Looper.getMainLooper()).postDelayed({
//            when (result) {
//                is ScanResult.Clean -> {
//                    val intent = Intent(this, AppResultsAllGood::class.java)
//                    intent.putExtra("SCAN_RESULT", result)
//                    startActivity(intent)
//                    finish()
//                }
//                is ScanResult.Malicious, is ScanResult.Suspicious -> {
//                    val intent = Intent(this, AppResultsBad::class.java)
//                    intent.putExtra("SCAN_RESULT", result)
//                    startActivity(intent)
//                    finish()
//                }
//                is ScanResult.Error -> {
//                    currentStepText.text = "Failed: ${result.message}"
//                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
//                }
//            }
//        }, 1000)
//    }
//}