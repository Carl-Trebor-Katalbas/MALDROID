package com.app.maldroid.homeactivities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.app.maldroid.MainActivity
import com.app.maldroid.R
import com.app.maldroid.data.ScanDataManager
import com.app.maldroid.data.model.ApkScanResult
import com.app.maldroid.data.model.ScanResult
import kotlinx.coroutines.launch

class ScanningProgress : AppCompatActivity() {

    private lateinit var circleProgress: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var currentStepText: TextView
    private lateinit var appNameText: TextView

    private lateinit var pulseRing1: View
    private lateinit var pulseRing2: View
    private val pulseAnimators = mutableListOf<AnimatorSet>()
    private val handler = Handler(Looper.getMainLooper())
    private var isScanning = false
    private var scanCompleted = false
    private var currentSimulatedProgress = 0
    private var isWaitingForML = false

    private var lastProgress = 0
    private var startTime: Long = 0

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ApkScannerService.ACTION_SCAN_PROGRESS -> {
                    val progress = intent.getIntExtra(ApkScannerService.EXTRA_PROGRESS, 0)
                    val step = intent.getStringExtra(ApkScannerService.EXTRA_CURRENT_STEP) ?: ""
                    lastProgress = progress
                    if (progress > currentSimulatedProgress) {
                        currentSimulatedProgress = progress
                        updateProgress(progress, step)
                    }
                }
                ApkScannerService.ACTION_SCAN_COMPLETE -> {
                    val scanResult = intent.getSerializableExtra(ApkScannerService.EXTRA_SCAN_RESULT) as? ScanResult
                    completeScan(scanResult)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanning_progress)

        circleProgress = findViewById(R.id.circularProgressBar)
        progressText = findViewById(R.id.progressPercentageText)
        currentStepText = findViewById(R.id.currentStepText)
        appNameText = findViewById(R.id.appNameText)

        pulseRing1 = findViewById(R.id.pulseRing1)
        pulseRing2 = findViewById(R.id.pulseRing2)
        startTime = System.currentTimeMillis()

        val fileName = intent.getStringExtra("FILE_NAME") ?: "Unknown APK"
        appNameText.text = "Scanning: $fileName"

        val cancelButton = findViewById<Button>(R.id.cancel_button)
        cancelButton.setOnClickListener {
            cancelScanning()
        }

        if (savedInstanceState == null) {
            startSmoothScanAnimation()
            val apkPath = intent.getStringExtra("APK_PATH")
            apkPath?.let { startScanningService(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(ApkScannerService.ACTION_SCAN_PROGRESS)
            addAction(ApkScannerService.ACTION_SCAN_COMPLETE)
        }

        ContextCompat.registerReceiver(
            this,
            broadcastReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        if (isScanning && !scanCompleted) {
            startPulseAnimation()
            if (!handler.hasMessages(0)) {
                startSimulatedProgress()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) { }

        stopPulseAnimation()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScanning()
    }

    @SuppressLint("SetTextI18n")
    private fun startSmoothScanAnimation() {
        isScanning = true
        scanCompleted = false
        currentSimulatedProgress = 0
        isWaitingForML = false

        circleProgress.progress = 0
        progressText.text = "0%"
        currentStepText.text = "Starting APK analysis..."

        startPulseAnimation()
        startSimulatedProgress()
    }

    private fun startPulseAnimation() {
        if (pulseAnimators.isNotEmpty()) return

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
                    if (isScanning && !scanCompleted && !isFinishing) {
                        start()
                    }
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

    private fun cancelScanning() {
        stopScanning()

        lifecycleScope.launch {
            try {
                com.app.maldroid.api.ApiClient.api.cancelScan()
            } catch (e: Exception) { }
        }

        val serviceIntent = Intent(this, ApkScannerService::class.java)
        stopService(serviceIntent)

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun startSimulatedProgress() {
        val maxProgress = 85
        var dotAnimationCounter = 0

        val dotAnimationRunnable = object : Runnable {
            override fun run() {
                if (!isScanning || scanCompleted) return

                dotAnimationCounter = (dotAnimationCounter + 1) % 4
                val baseText = "Waiting for ML analysis...."
                val spannable = SpannableString(baseText)

                val dotsToShow = dotAnimationCounter + 1
                val dotsToHide = 4 - dotsToShow

                if (dotsToHide > 0) {
                    val start = baseText.length - dotsToHide
                    val end = baseText.length
                    spannable.setSpan(
                        ForegroundColorSpan(Color.TRANSPARENT),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }

                updateProgress(currentSimulatedProgress, spannable)
                handler.postDelayed(this, 500)
            }
        }

        val progressRunnable = object : Runnable {
            override fun run() {
                if (!isScanning || scanCompleted) return

                if (currentSimulatedProgress < maxProgress) {
                    currentSimulatedProgress += 1
                    updateProgress(currentSimulatedProgress, getStepMessage(currentSimulatedProgress))

                    val delay = (50L..150L).random()
                    handler.postDelayed(this, delay)
                } else if (currentSimulatedProgress >= maxProgress && !isWaitingForML) {
                    isWaitingForML = true
                    handler.post(dotAnimationRunnable)
                }
            }
        }

        if (!isWaitingForML) {
            handler.post(progressRunnable)
        } else {
            handler.post(dotAnimationRunnable)
        }
    }

    private fun getStepMessage(progress: Int): String {
        return when (progress) {
            in 0..10 -> "Loading ML engine..."
            in 11..25 -> "Extracting APK data..."
            in 26..45 -> "Analyzing permissions..."
            in 46..65 -> "Running static analysis..."
            in 66..80 -> "Checking for malware patterns..."
            in 81..85 -> "Final security assessment..."
            else -> "Generating report..."
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateProgress(progress: Int, step: CharSequence) {
        runOnUiThread {
            circleProgress.progress = progress
            progressText.text = "$progress%"
            currentStepText.text = step
        }
    }

    private fun startScanningService(apkPath: String) {
        val intent = Intent(this, ApkScannerService::class.java).apply {
            action = ApkScannerService.ACTION_SCAN_APK
            putExtra(ApkScannerService.EXTRA_APK_PATH, apkPath)
        }
        startService(intent)
    }

    @SuppressLint("SetTextI18n")
    private fun completeScan(scanResult: ScanResult?) {
        if (scanCompleted) return

        scanCompleted = true
        isScanning = false

        runOnUiThread {
            updateProgress(100, "ML Analysis Complete!")
            stopPulseAnimation()

            handler.postDelayed({
                navigateToResults(scanResult)
            }, 1000)
        }
    }

    private fun shouldSaveHistory(): Boolean {
        val sharedPrefs = getSharedPreferences("MaldroidSettings", Context.MODE_PRIVATE)
        return sharedPrefs.getBoolean("keep_scan_history", true)
    }

    private fun saveScanResultToHistory(scanResult: ScanResult, durationMs: Long) {
        if (!shouldSaveHistory()) return

        val fileName = intent.getStringExtra("FILE_NAME") ?: "Unknown File"
        val filePath = intent.getStringExtra("FILE_PATH") ?: ""

        val (resultText, isSafe, threatList) = when (scanResult) {
            is ScanResult.Malicious -> {
                val names = scanResult.detectedThreats.map { it.description }
                Triple("Malicious threat detected", false, names)
            }
            is ScanResult.Suspicious -> {
                val names = scanResult.warnings.map { it.description }
                Triple("Suspicious activity detected", false, names)
            }
            is ScanResult.Clean -> {
                Triple("Clean - No threats detected", true, emptyList<String>())
            }
            is ScanResult.Error -> {
                Triple("Scan error: ${scanResult.message}", false, emptyList<String>())
            }
        }

        val apkScanResult = ApkScanResult(
            fileName = fileName,
            filePath = filePath,
            fileSize = 0L,
            packageName = "N/A",
            versionName = "N/A",
            versionCode = 0,
            permissions = emptyList(),
            scanTimestamp = System.currentTimeMillis(),
            scanResult = resultText,
            isSafe = isSafe,
            scanDuration = durationMs,
            detectedThreats = threatList
        )

        ScanDataManager.addApkScanResult(apkScanResult, this)
    }

    private fun navigateToResults(scanResult: ScanResult?) {
        try {
            val endTime = System.currentTimeMillis()
            val durationMs = endTime - startTime

            val fileName = intent.getStringExtra("FILE_NAME")
            val originalUri = intent.getStringExtra("ORIGINAL_URI")
            val filePath = intent.getStringExtra("FILE_PATH")

            if (scanResult != null) {
                saveScanResultToHistory(scanResult, durationMs)
            }

            val intent = when {
                scanResult is ScanResult.Clean -> {
                    Intent(this, ApkResultsAllGood::class.java).apply {
                        putExtra("SCAN_RESULT", scanResult)
                        putExtra("FILE_NAME", fileName)
                        putExtra("SCAN_START_TIME", startTime)
                        putExtra("SCAN_DURATION", durationMs)
                        putExtra("ORIGINAL_URI", originalUri)
                        putExtra("FILE_PATH", filePath)
                    }
                }
                scanResult is ScanResult.Suspicious || scanResult is ScanResult.Malicious -> {
                    Intent(this, ApkResultsBad::class.java).apply {
                        putExtra("SCAN_RESULT", scanResult)
                        putExtra("FILE_NAME", fileName)
                        putExtra("SCAN_START_TIME", startTime)
                        putExtra("SCAN_DURATION", durationMs)
                        putExtra("ORIGINAL_URI", originalUri)
                        putExtra("FILE_PATH", filePath)
                    }
                }
                scanResult is ScanResult.Error -> {
                    Intent(this, ApkResultsBad::class.java).apply {
                        putExtra("SCAN_RESULT", scanResult)
                        putExtra("FILE_NAME", fileName)
                        putExtra("ERROR_MESSAGE", scanResult.message)
                        putExtra("SCAN_START_TIME", startTime)
                        putExtra("SCAN_DURATION", durationMs)
                        putExtra("ORIGINAL_URI", originalUri)
                        putExtra("FILE_PATH", filePath)
                    }
                }
                else -> {
                    Intent(this, ApkResultsBad::class.java).apply {
                        putExtra("ERROR_MESSAGE", "Unknown scan result")
                        putExtra("FILE_NAME", fileName)
                        putExtra("SCAN_START_TIME", startTime)
                        putExtra("SCAN_DURATION", durationMs)
                        putExtra("ORIGINAL_URI", originalUri)
                    }
                }
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    private fun stopScanning() {
        isScanning = false
        stopPulseAnimation()
        handler.removeCallbacksAndMessages(null)
    }
}