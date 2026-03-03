//package com.app.maldroid.homeactivities
//
//import android.annotation.SuppressLint
//import android.content.Intent
//import android.os.Bundle
//import android.widget.Button
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.ContextCompat
//import com.app.maldroid.R
//import com.app.maldroid.data.model.ScanResult
//
//class AppResultsAllGood : AppCompatActivity() {
//
//    private lateinit var scanSummaryText: TextView
//    private lateinit var threatStatusText: TextView
//    private lateinit var appDetailsText: TextView
//    private lateinit var backButton: ImageView
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_app_results_all_good)
//
//        // Retrieve data passed from AppScanningProgress
//        val appName = intent.getStringExtra("APP_NAME") ?: "Unknown App"
//        val packageName = intent.getStringExtra("PACKAGE_NAME") ?: "N/A"
//        val scanResult = intent.getSerializableExtra("SCAN_RESULT") as? ScanResult
//
//        initializeViews()
//        setupClickListeners()
//
//        displaySafeResults(appName, packageName, scanResult)
//    }
//
//    private fun initializeViews() {
//        backButton = findViewById(R.id.backButton)
//        scanSummaryText = findViewById(R.id.scanSummaryText)
//        threatStatusText = findViewById(R.id.threatStatusText)
//        appDetailsText = findViewById(R.id.apkDetailsText)
//    }
//
//    private fun setupClickListeners() {
//        findViewById<Button>(R.id.scanAgainBtn).setOnClickListener {
//            startActivity(Intent(this, AppSelection::class.java))
//            finish()
//        }
//
//        backButton.setOnClickListener {
//            startActivity(Intent(this, AppSelection::class.java).apply {
//                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
//            })
//            finish()
//        }
//    }
//
//    @SuppressLint("SetTextI18n")
//    private fun displaySafeResults(appName: String, packageName: String, result: ScanResult?) {
//        scanSummaryText.text = "App Scan Complete"
//
//        threatStatusText.text = "No risks found."
//        threatStatusText.setTextColor(ContextCompat.getColor(this, R.color.green))
//
//        val confidence = if (result is ScanResult.Clean) (result.confidence * 100).toInt() else 0
//
//        val details = """
//            App Name: $appName
//            Package: $packageName
//            Status: Safe
//            ML Confidence: $confidence%
//
//            Our machine learning engine analyzed this app and found no malicious patterns or dangerous code structures.
//        """.trimIndent()
//
//        appDetailsText.text = details
//
//
//    }
//}