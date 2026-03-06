package com.app.maldroid.homeactivities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.maldroid.R
import com.app.maldroid.data.model.ScanResultItem
import com.app.maldroid.data.model.ScanResult
import com.app.maldroid.data.model.DetectedThreat
import com.app.maldroid.data.model.ThreatType
import com.app.maldroid.data.model.ThreatLevel

class ScanAllApkResultActivity : AppCompatActivity() {

    private lateinit var resultsRecycler: RecyclerView
    private lateinit var doneButton: Button
    private lateinit var resultSummary: TextView
    private lateinit var adapter: ScanResultsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_all_apk_result)
        val scannedItems: ArrayList<ScanResultItem> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("SCAN_RESULTS", ScanResultItem::class.java) ?: arrayListOf()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra("SCAN_RESULTS") ?: arrayListOf()
        }

        initViews()
        setupRecyclerView(scannedItems)
        updateSummary(scannedItems)
    }

    private fun initViews() {
        resultsRecycler = findViewById(R.id.resultsRecycler)
        doneButton = findViewById(R.id.doneButton)
        resultSummary = findViewById(R.id.resultSummary)

        doneButton.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView(items: List<ScanResultItem>) {
        resultsRecycler.layoutManager = LinearLayoutManager(this)

        adapter = ScanResultsAdapter(items) { clickedItem ->

            val targetActivity = if (clickedItem.isSafe) {
                ApkResultsAllGood::class.java
            } else {
                ApkResultsBad::class.java
            }

            val intent = Intent(this, targetActivity)
            intent.putExtra("FILE_NAME", clickedItem.appName)
            val passedResult = clickedItem.fullScanResult

            val finalResult = if (passedResult != null) {
                passedResult
            } else if (clickedItem.isSafe) {
                ScanResult.Clean(
                    confidence = 0.95f,
                    notes = emptyList(),
                    triggers = clickedItem.threatDetails ?: emptyList()
                )
            } else {
                val reconstructedThreats = (clickedItem.threatDetails ?: emptyList()).map { threatString ->
                    DetectedThreat(
                        type = ThreatType.SUSPICIOUS_BEHAVIOR,
                        severity = ThreatLevel.HIGH,
                        description = threatString,
                        details = "Detected during batch scan"
                    )
                }
                ScanResult.Malicious(
                    threatLevel = ThreatLevel.HIGH,
                    confidence = 0.85f,
                    detectedThreats = reconstructedThreats,
                    recommendations = listOf("🚫 Uninstall Immediately", "⚠️ Check app permissions")
                )
            }

            intent.putExtra("SCAN_RESULT", finalResult)
            intent.putExtra("PACKAGE_NAME", clickedItem.packageName)
            intent.putExtra("FILE_PATH", clickedItem.filePath)
            intent.putExtra("SCAN_DURATION", clickedItem.scanDuration)

            startActivity(intent)
        }

        resultsRecycler.adapter = adapter
    }

    private fun updateSummary(items: List<ScanResultItem>) {
        val threats = items.count { !it.isSafe }
        resultSummary.text = "Found $threats threats in ${items.size} files"

        if (threats > 0) {
            resultSummary.setTextColor(resources.getColor(android.R.color.holo_red_dark, theme))
        }
    }
}