//package com.app.maldroid.homeactivities
//
//import android.app.Service
//import android.content.Intent
//import android.content.pm.ApplicationInfo
//import android.content.pm.PackageManager
//import android.os.IBinder
//import android.util.Log
//import com.app.maldroid.api.ApiClient
//import com.app.maldroid.api.AnalyzeResponse
//import com.app.maldroid.data.model.*
//import kotlinx.coroutines.*
//import okhttp3.MediaType.Companion.toMediaTypeOrNull
//import okhttp3.MultipartBody
//import okhttp3.RequestBody.Companion.asRequestBody
//import java.io.File
//
//class AppScannerService : Service() {
//
//    companion object {
//        const val ACTION_SCAN_APP = "ACTION_SCAN_APP"
//        const val ACTION_SCAN_PROGRESS = "ACTION_SCAN_PROGRESS"
//        const val ACTION_SCAN_COMPLETE = "ACTION_SCAN_COMPLETE"
//
//        const val EXTRA_PACKAGE_NAME = "EXTRA_PACKAGE_NAME"
//        const val EXTRA_PROGRESS = "EXTRA_PROGRESS"
//        const val EXTRA_CURRENT_STEP = "EXTRA_CURRENT_STEP"
//        const val EXTRA_SCAN_RESULT = "EXTRA_SCAN_RESULT"
//
//        // FILTER LOGCAT WITH THIS TAG
//        private const val TAG = "AppScannerService"
//    }
//
//    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
//
//    override fun onBind(intent: Intent?): IBinder? = null
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        Log.d(TAG, "onStartCommand called with action: ${intent?.action}")
//
//        if (intent?.action == ACTION_SCAN_APP) {
//            val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
//            if (packageName != null) {
//                Log.d(TAG, "Starting scan for package: $packageName")
//                performScan(packageName)
//            } else {
//                Log.e(TAG, "Error: Package name was null")
//                stopSelf()
//            }
//        }
//        return START_NOT_STICKY
//    }
//
//    private fun performScan(pkgName: String) {
//        serviceScope.launch {
//            try {
//                // --- Step 1: Locate Files ---
//                Log.d(TAG, "Step 1: Locating APK files...")
//                updateProgress(10, "Locating app files...")
//
//                val appInfo = getApplicationInfo(pkgName)
//                if (appInfo == null) {
//                    Log.e(TAG, "Error: Could not find ApplicationInfo for $pkgName")
//                    throw Exception("App not found on device")
//                }
//
//                val baseApk = File(appInfo.sourceDir)
//                Log.d(TAG, "Found Base APK: ${baseApk.absolutePath} (Size: ${baseApk.length()} bytes)")
//
//                val splitApks = appInfo.splitSourceDirs?.map { File(it) } ?: emptyList()
//                if (splitApks.isNotEmpty()) {
//                    Log.d(TAG, "Found ${splitApks.size} Split APKs")
//                }
//
//                val allFiles = listOf(baseApk) + splitApks
//
//                // --- Step 2: Prepare Upload ---
//                Log.d(TAG, "Step 2: Preparing upload for ${allFiles.size} file(s)...")
//                updateProgress(40, "Uploading to analysis engine...")
//
//                val mediaType = "application/vnd.android.package-archive".toMediaTypeOrNull()
//
//                // --- Step 3: API Call ---
//                Log.d(TAG, "Step 3: Sending network request to ${ApiClient.BASE_URL}...")
//
//                val apiResponse: AnalyzeResponse = if (splitApks.isNotEmpty()) {
//                    val parts = allFiles.map { file ->
//                        val requestBody = file.asRequestBody(mediaType)
//                        MultipartBody.Part.createFormData("apkFiles", file.name, requestBody)
//                    }
//                    ApiClient.api.analyzeSplitApk(parts)
//                } else {
//                    val requestBody = baseApk.asRequestBody(mediaType)
//                    val part = MultipartBody.Part.createFormData("apkFile", baseApk.name, requestBody)
//                    ApiClient.api.analyzeApk(part)
//                }
//
//                // --- Step 4: Handle Response ---
//                Log.d(TAG, "Step 4: Response Received!")
//                Log.d(TAG, ">> API Prediction: ${apiResponse.prediction}")
//                Log.d(TAG, ">> API Confidence: ${apiResponse.confidence}")
//                Log.d(TAG, ">> API Label: ${apiResponse.label}")
//
//                updateProgress(90, "Processing results...")
//                handleBackendResponse(apiResponse)
//
//            } catch (e: Exception) {
//                Log.e(TAG, "!!! SCAN FAILED !!!", e)
//
//                val errorMsg = when {
//                    e.message?.contains("Failed to connect") == true -> "Connection failed. Check Server IP/WiFi."
//                    e.message?.contains("timeout") == true -> "Upload timed out. File too large?"
//                    else -> e.message ?: "Unknown error"
//                }
//
//                sendScanResult(ScanResult.Error(errorMsg))
//            } finally {
//                Log.d(TAG, "Service stopping.")
//                stopSelf()
//            }
//        }
//    }
//
//    private fun handleBackendResponse(response: AnalyzeResponse) {
//        val result = when (response.label.uppercase()) {
//            "MALICIOUS" -> {
//                Log.w(TAG, "Result is MALICIOUS")
//                ScanResult.Malicious(
//                    threatLevel = ThreatLevel.HIGH,
//                    confidence = response.confidence,
//                    detectedThreats = listOf(
//                        DetectedThreat(
//                            type = ThreatType.KNOWN_MALWARE,
//                            severity = ThreatLevel.HIGH,
//                            description = "ML Engine detected malicious patterns",
//                            details = "Confidence: ${(response.confidence * 100).toInt()}%"
//                        )
//                    ),
//                    recommendations = listOf("Uninstall Immediately")
//                )
//            }
//            "SUSPICIOUS" -> {
//                Log.w(TAG, "Result is SUSPICIOUS")
//                ScanResult.Suspicious(
//                    threatLevel = ThreatLevel.MEDIUM,
//                    confidence = response.confidence,
//                    warnings = listOf(
//                        SecurityWarning("Suspicious Behavior", "Abnormal code structure detected", ThreatLevel.MEDIUM)
//                    )
//                )
//            }
//            else -> {
//                Log.i(TAG, "Result is CLEAN")
//                ScanResult.Clean(
//                    confidence = response.confidence,
//                    notes = listOf(SecurityNote("Clean", "No threats found"))
//                )
//            }
//        }
//        sendScanResult(result)
//    }
//
//    private fun getApplicationInfo(packageName: String): ApplicationInfo? {
//        return try {
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
//                packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
//            } else {
//                @Suppress("DEPRECATION")
//                packageManager.getApplicationInfo(packageName, 0)
//            }
//        } catch (e: Exception) { null }
//    }
//
//    private fun updateProgress(progress: Int, step: String) {
//        Log.d(TAG, "Progress Update: $progress% - $step")
//        val intent = Intent(ACTION_SCAN_PROGRESS).apply {
//            putExtra(EXTRA_PROGRESS, progress)
//            putExtra(EXTRA_CURRENT_STEP, step)
//            setPackage(packageName)
//        }
//        sendBroadcast(intent)
//    }
//
//    private fun sendScanResult(scanResult: ScanResult) {
//        Log.d(TAG, "Broadcasting Final Result: $scanResult")
//        val intent = Intent(ACTION_SCAN_COMPLETE).apply {
//            putExtra(EXTRA_SCAN_RESULT, scanResult)
//            setPackage(packageName)
//        }
//        sendBroadcast(intent)
//    }
//
//    override fun onDestroy() {
//        Log.d(TAG, "onDestroy called")
//        serviceScope.cancel()
//        super.onDestroy()
//    }
//}