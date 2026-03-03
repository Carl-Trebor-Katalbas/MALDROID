package com.app.maldroid

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.maldroid.data.ScanDataManager
import com.app.maldroid.data.model.*
import com.app.maldroid.homeactivities.ApkResultsAllGood
import com.app.maldroid.homeactivities.ApkResultsBad
import com.app.maldroid.homeactivities.ScanResultsAdapter

class RecentScansFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ScanResultsAdapter
    private lateinit var emptyStateText: TextView
    private lateinit var cardView: CardView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recent_scans, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
    }

    override fun onResume() {
        super.onResume()
        loadScanHistory()
    }

    private fun setupViews(view: View) {
        recyclerView = view.findViewById(R.id.scanHistoryContainer)
        cardView = view.findViewById(R.id.cardView)
        emptyStateText = TextView(requireContext()).apply {
            text = "NO RECENT SCAN HISTORY"
            setTextColor(android.graphics.Color.BLACK)
            textSize = 16f
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            visibility = View.GONE
        }
        cardView.addView(emptyStateText)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadScanHistory() {
        val allResults = mutableListOf<ScanResultItem>()
        val appScans = ScanDataManager.getScannedApps()
        for (app in appScans) {
            allResults.add(
                ScanResultItem(
                    appName = app.name,
                    scanTimestamp = app.scanTimestamp,
                    scanResult = if (app.isMalicious) "Threat Detected" else "Safe",
                    isSafe = !app.isMalicious,
                    packageName = app.packageName,
                    scanDuration = 0
                )
            )
        }
        val apkScans = ScanDataManager.getApkScanResults()
        for (apk in apkScans) {
            val historyTriggers = mutableListOf<String>()
            if (!apk.permissions.isNullOrEmpty()) {
                historyTriggers.addAll(apk.permissions!!)
            }
            if (!apk.detectedThreats.isNullOrEmpty()) {
                historyTriggers.addAll(apk.detectedThreats!!)
            }
            val finalTriggers = historyTriggers.distinct()
            allResults.add(
                ScanResultItem(
                    appName = apk.fileName ?: "Unknown APK",
                    scanTimestamp = apk.scanTimestamp,
                    scanResult = apk.scanResult ?: if (apk.isSafe) "Safe" else "Malicious",
                    isSafe = apk.isSafe,
                    filePath = apk.filePath,
                    scanDuration = apk.scanDuration ?: 0L,
                    threatDetails = finalTriggers
                )
            )
        }
        allResults.sortByDescending { it.scanTimestamp }
        updateUI(allResults)
    }

    private fun updateUI(scanResults: List<ScanResultItem>) {
        if (scanResults.isEmpty()) {
            emptyStateText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyStateText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE

            adapter = ScanResultsAdapter(scanResults) { clickedItem ->
                val cleanedTriggers = clickedItem.threatDetails.map { cleanTriggerString(it) }

                if (clickedItem.isSafe) {
                    val intent = Intent(requireContext(), ApkResultsAllGood::class.java)
                    intent.putExtra("FILE_NAME", clickedItem.appName)
                    intent.putExtra("FILE_PATH", clickedItem.filePath)
                    intent.putExtra("SCAN_DURATION", clickedItem.scanDuration)

                    val cleanResult = ScanResult.Clean(
                        confidence = 0.95f,
                        notes = listOf(SecurityNote("Clean", "Verified from Scan History")),
                        triggers = cleanedTriggers
                    )
                    intent.putExtra("SCAN_RESULT", cleanResult)
                    startActivity(intent)

                } else {
                    val intent = Intent(requireContext(), ApkResultsBad::class.java)

                    intent.putExtra("FILE_NAME", clickedItem.appName)
                    intent.putExtra("FILE_PATH", clickedItem.filePath)
                    intent.putExtra("SCAN_DURATION", clickedItem.scanDuration)
                    intent.putExtra("PACKAGE_NAME", clickedItem.packageName)

                    val isSuspicious = clickedItem.scanResult.contains("Suspicious", ignoreCase = true)

                    val resultObject: ScanResult = if (isSuspicious) {
                        val warnings = cleanedTriggers.map { cleanWarning ->
                            SecurityWarning(
                                title = "Suspicious Feature",
                                description = cleanWarning,
                                severity = ThreatLevel.MEDIUM
                            )
                        }
                        ScanResult.Suspicious(
                            threatLevel = ThreatLevel.MEDIUM,
                            confidence = 0.5f,
                            warnings = warnings
                        )
                    } else {
                        val restoredThreats = cleanedTriggers.map { cleanName ->
                            val type = when {
                                cleanName.contains("Permission", true) -> ThreatType.DANGEROUS_PERMISSIONS
                                cleanName.contains("http", true) -> ThreatType.PRIVACY_RISK
                                else -> ThreatType.SUSPICIOUS_BEHAVIOR
                            }
                            DetectedThreat(
                                type = type,
                                severity = ThreatLevel.HIGH,
                                description = cleanName,
                                details = "Detected in history"
                            )
                        }

                        ScanResult.Malicious(
                            threatLevel = ThreatLevel.HIGH,
                            confidence = 1.0f,
                            detectedThreats = restoredThreats,
                            recommendations = listOf("Uninstall Immediately", "Scan Device")
                        )
                    }

                    intent.putExtra("SCAN_RESULT", resultObject)
                    startActivity(intent)
                }
            }
            recyclerView.adapter = adapter
        }
    }
    private fun cleanTriggerString(raw: String): String {
        return raw.replace(Regex("\\(AI Score:.*\\)", RegexOption.IGNORE_CASE), "")
            .replace("android.permission.", "")
            .replace("android.hardware.", "")
            .trim()
    }
}