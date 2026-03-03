package com.app.maldroid.homeactivities

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.app.maldroid.MainActivity
import com.app.maldroid.R
import com.app.maldroid.data.model.ApkScanResult
import java.io.File
import java.io.FileOutputStream

class FileSelection : AppCompatActivity() {

    private lateinit var selectedFileText: TextView
    private lateinit var scanResultText: TextView
    private lateinit var selectApkButton: Button
    private lateinit var scanApkButton: Button
    private lateinit var backButton: ImageView

    private var selectedApkUri: Uri? = null

    @SuppressLint("SetTextI18n")
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedApkUri = it
            val fileName = getFileNameFromUri(it)
            selectedFileText.text = "Selected: $fileName"
            enableScanButton()
            scanResultText.text = "Ready to scan: $fileName"
        } ?: run {
            selectedFileText.text = "No file selected"
            disableScanButton()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_selection)

        initializeViews()
        setupClickListeners()
        disableScanButton()
    }

    private fun initializeViews() {
        selectedFileText = findViewById(R.id.selectedFileText)
        scanResultText = findViewById(R.id.scanResultText)
        selectApkButton = findViewById(R.id.selectApkButton)
        scanApkButton = findViewById(R.id.scanApkButton)
        backButton = findViewById(R.id.backButton)
    }

    private fun setupClickListeners() {
        selectApkButton.setOnClickListener {
            openFilePicker()
        }
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        scanApkButton.setOnClickListener {
            selectedApkUri?.let { uri ->
                scanApkFile(uri)
            } ?: run {
                Toast.makeText(this, "Please select an APK file first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openFilePicker() {
        filePickerLauncher.launch("application/vnd.android.package-archive")
    }

    private fun getFileNameFromUri(uri: Uri): String {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            } ?: uri.lastPathSegment ?: "Unknown file"
        } catch (e: Exception) {
            uri.lastPathSegment ?: "Unknown file"
        }
    }

    @SuppressLint("SetTextI18n")
    private fun scanApkFile(uri: Uri) {
        try {
            val fileName = getFileNameFromUri(uri)
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File(cacheDir, fileName)
            inputStream?.use { it.copyTo(FileOutputStream(tempFile)) }

            val intent = Intent(this, ScanningProgress::class.java).apply {
                putExtra("APK_PATH", tempFile.absolutePath)
                putExtra("FILE_NAME", fileName)
                putExtra("FILE_PATH", tempFile.absolutePath)
                putExtra("ORIGINAL_URI", uri.toString())
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "File error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enableScanButton() {
        scanApkButton.isEnabled = true
        scanApkButton.setBackgroundResource(R.drawable.custom_button_green)
        scanApkButton.setTextColor(ContextCompat.getColor(this, android.R.color.white))

    }

    private fun disableScanButton() {
        scanApkButton.isEnabled = false
        scanApkButton.setBackgroundResource(R.drawable.custom_button2)
        scanApkButton.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
    }

    private fun extractApkInfo(uri: Uri): ApkScanResult {
        val fileName = getFileNameFromUri(uri)
        return ApkScanResult(
            fileName = fileName,
            filePath = uri.toString(),
            fileSize = 0L,
            packageName = "com.example.demo",
            versionName = "1.0.0",
            versionCode = 1,
            permissions = listOf("INTERNET", "READ_EXTERNAL_STORAGE"),
            scanResult = "APK analyzed successfully - No risks found"
        )
    }
}