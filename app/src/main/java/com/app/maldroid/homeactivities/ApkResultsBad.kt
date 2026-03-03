package com.app.maldroid.homeactivities

import android.annotation.SuppressLint
import android.app.RecoverableSecurityException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.app.maldroid.MainActivity
import com.app.maldroid.R
import com.app.maldroid.data.model.ScanResult
import com.app.maldroid.data.model.ThreatType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ApkResultsBad : AppCompatActivity() {

    private lateinit var maliciousAlertText: TextView
    private lateinit var scanDurationText: TextView
    private lateinit var apkFileNameText: TextView
    private lateinit var permissionsText: TextView
    private lateinit var apiCallsText: TextView
    private lateinit var codePatternsText: TextView
    private lateinit var networkLinksText: TextView
    private lateinit var backButton: ImageButton
    private lateinit var deleteAppBtn: Button

    private lateinit var intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_apk_results_bad)

        initializeViews()
        initIntentSenderLauncher()
        setupClickListeners()
        displayScanResults()
        displayScanDuration()
    }

    private fun initializeViews() {
        maliciousAlertText = findViewById(R.id.maliciousAlertText)
        scanDurationText = findViewById(R.id.scanDurationText)
        apkFileNameText = findViewById(R.id.apkFileNameText)
        permissionsText = findViewById(R.id.permissionsText)
        apiCallsText = findViewById(R.id.apiCallsText)
        codePatternsText = findViewById(R.id.codePatternsText)
        networkLinksText = findViewById(R.id.networkLinksText)
        backButton = findViewById(R.id.backButton)
        deleteAppBtn = findViewById(R.id.deleteAppBtn)
    }

    private fun initIntentSenderLauncher() {
        intentSenderLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                showSuccessDialog()
            } else {
                Toast.makeText(this, "Deletion declined", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        val scanAgainBtn = findViewById<Button>(R.id.scanAgainBtn)

        scanAgainBtn.setOnClickListener {
            val intent = Intent(this, FileSelection::class.java)
            startActivity(intent)
            finish()
        }

        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        deleteAppBtn.setOnClickListener {
            val uriString = intent.getStringExtra("ORIGINAL_URI")
            if (!uriString.isNullOrEmpty()) {
                lifecycleScope.launch { deleteApkFromExternalStorage(Uri.parse(uriString)) }
                return@setOnClickListener
            }

            val filePath = intent.getStringExtra("FILE_PATH")
            if (!filePath.isNullOrEmpty()) {
                val file = File(filePath)
                if (file.exists() && file.delete()) {
                    showSuccessDialog()
                } else {
                    Toast.makeText(this, "Could not delete file (Permission or Not Found)", Toast.LENGTH_SHORT).show()
                }
                return@setOnClickListener
            }

            Toast.makeText(this, "File location unknown", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun displayScanDuration() {
        val durationMs = intent.getLongExtra("SCAN_DURATION", 0)

        val durationText = if (durationMs >= 1000) {
            String.format("%.2f seconds", durationMs / 1000.0)
        } else if (durationMs > 0) {
            "$durationMs ms"
        } else {
            "--"
        }

        scanDurationText.text = "Scan Duration: $durationText"
    }

    private suspend fun deleteApkFromExternalStorage(apkUri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                contentResolver.delete(apkUri, null, null)
                withContext(Dispatchers.Main) { showSuccessDialog() }
            } catch (e: SecurityException) {
                val intentSender = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                        MediaStore.createDeleteRequest(contentResolver, listOf(apkUri)).intentSender
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                        (e as? RecoverableSecurityException)?.userAction?.actionIntent?.intentSender
                    else -> null
                }
                intentSender?.let { sender ->
                    withContext(Dispatchers.Main) {
                        intentSenderLauncher.launch(IntentSenderRequest.Builder(sender).build())
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ApkResultsBad, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Threat Removed")
            .setMessage("The file has been successfully deleted.")
            .setPositiveButton("OK") { _, _ ->
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .setCancelable(false)
            .show()
    }


    @SuppressLint("SetTextI18n")
    private fun displayScanResults() {
        val fileName = intent.getStringExtra("FILE_NAME") ?: "Unknown File"
        apkFileNameText.text = fileName

        val serializableExtra = intent.getSerializableExtra("SCAN_RESULT")
        when (serializableExtra) {
            is ScanResult.Malicious -> {
                maliciousAlertText.text = "MALICIOUS THREAT DETECTED"
                maliciousAlertText.setTextColor(ContextCompat.getColor(this, R.color.red))

                fun formatThreats(type: ThreatType): String {
                    val items = serializableExtra.detectedThreats
                        .filter { it.type == type }
                        .map { "• ${it.description}" }
                    return if (items.isNotEmpty()) items.joinToString("\n") else "None Detected"
                }

                permissionsText.text = formatThreats(ThreatType.DANGEROUS_PERMISSIONS)
                apiCallsText.text = formatThreats(ThreatType.SUSPICIOUS_BEHAVIOR)
                codePatternsText.text = formatThreats(ThreatType.KNOWN_MALWARE)
                networkLinksText.text = formatThreats(ThreatType.PRIVACY_RISK)
            }

            is ScanResult.Suspicious -> {
                maliciousAlertText.text = "SUSPICIOUS APP"
                maliciousAlertText.setTextColor(ContextCompat.getColor(this, R.color.orange))
                val warnings = serializableExtra.warnings.joinToString("\n") { "• ${it.description}" }

                permissionsText.text = warnings
                apiCallsText.text = "• Inconclusive patterns"
                codePatternsText.text = "• Verify source manually"
                networkLinksText.text = "• Review permissions"
            }

            is ScanResult.Error -> {
                maliciousAlertText.text = "SCAN ERROR"
                maliciousAlertText.setTextColor(ContextCompat.getColor(this, R.color.red))

                permissionsText.text = "Error Message:\n${serializableExtra.message}"
                codePatternsText.text = "Verify ProGuard rules"
                networkLinksText.text = "Check Backend Logs"
            }

            is ScanResult.Clean -> {
                maliciousAlertText.text = "APP IS CLEAN"
                maliciousAlertText.setTextColor(ContextCompat.getColor(this, R.color.green))
                permissionsText.text = "No threats detected."
            }

            else -> {
                maliciousAlertText.text = "UNKNOWN RESULT"
                permissionsText.text = "An unexpected error occurred."
            }
        }
    }
}