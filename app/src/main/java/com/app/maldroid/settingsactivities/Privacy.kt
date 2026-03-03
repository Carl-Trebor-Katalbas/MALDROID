package com.app.maldroid.settingsactivities

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.app.maldroid.R
import com.app.maldroid.data.ScanDataManager

class Privacy : AppCompatActivity() {

    private val PREFS_NAME = "MaldroidSettings"
    private val KEY_KEEP_HISTORY = "keep_scan_history"

    private lateinit var historyRadioGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_privacy)

        val backArrow = findViewById<ImageView>(R.id.backArrow2)
        val clearScanButton = findViewById<Button>(R.id.clearResultScanButton)
        val resetSettingsButton = findViewById<Button>(R.id.resetSettingsButton)
        historyRadioGroup = findViewById(R.id.historyRadioGroup)

        loadSettings()

        backArrow.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        clearScanButton.setOnClickListener {
            showClearDataConfirmation()
        }

        resetSettingsButton.setOnClickListener {
            showResetSettingsConfirmation()
        }

        historyRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val shouldKeepHistory = (checkedId == R.id.radioYes)
            saveKeepHistoryPreference(shouldKeepHistory)
        }
    }
    private fun loadSettings() {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val keepHistory = sharedPrefs.getBoolean(KEY_KEEP_HISTORY, true)

        if (keepHistory) {
            historyRadioGroup.check(R.id.radioYes)
        } else {
            historyRadioGroup.check(R.id.radioNo)
        }
    }

    private fun saveKeepHistoryPreference(keepHistory: Boolean) {
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putBoolean(KEY_KEEP_HISTORY, keepHistory)
            apply()
        }
    }


    private fun showClearDataConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Clear Scan Results")
            .setMessage("Are you sure you want to delete all scan history and cache? This cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                performClearData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performClearData() {
        try {
            ScanDataManager.clearAll(this)

            cacheDir.deleteRecursively()

            Toast.makeText(this, "Scan history and cache cleared", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error clearing data", Toast.LENGTH_SHORT).show()
        }
    }
    private fun showResetSettingsConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Reset Settings")
            .setMessage("Restore all privacy settings to default?")
            .setPositiveButton("Reset") { _, _ ->
                performResetSettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performResetSettings() {
        historyRadioGroup.check(R.id.radioYes)
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putBoolean(KEY_KEEP_HISTORY, true)
            apply()
        }

        Toast.makeText(this, "Settings reset to default", Toast.LENGTH_SHORT).show()
    }
}