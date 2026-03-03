package com.app.maldroid.data.model

import android.annotation.SuppressLint
import com.app.maldroid.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.Serializable
import kotlin.random.Random

class MLAPKScanner {

    data class MLFeatures(
        val permissions: List<String> = emptyList(),
        val apiCalls: List<String> = emptyList(),
        val intentFilters: List<String> = emptyList(),
        val fileSize: Long = 0L,
        val certificateInfo: String = "",
        val nativeLibraries: List<String> = emptyList(),
        val suspiciousStrings: List<String> = emptyList(),
        val entropy: Double = 0.0
    ) : Serializable

    @SuppressLint("SuspiciousIndentation")
    suspend fun analyzeWithML(apkPath: String): ScanResult {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(apkPath)

                val requestBody = file.asRequestBody("application/vnd.android.package-archive".toMediaType())
                val apkPart = MultipartBody.Part.createFormData("file", file.name, requestBody)

                val response = ApiClient.api.analyzeApk(apkPart)
                    when (response.label.lowercase()) {
                        "malware" -> ScanResult.Malicious(
                            threatLevel = ThreatLevel.HIGH,
                            confidence = response.confidence,
                            detectedThreats = listOf(
                                DetectedThreat(
                                    type = ThreatType.KNOWN_MALWARE,
                                    severity = ThreatLevel.HIGH,
                                    description = "Malware detected based on ML model"
                                )
                            ),
                            recommendations = listOf("Uninstall immediately", "Avoid installing unknown sources")
                        )
                        "benign" -> ScanResult.Clean(
                            confidence = response.confidence,
                            notes = listOf(SecurityNote("Safe", "App is clean according to the model"))
                        )
                        else -> ScanResult.Suspicious(
                            threatLevel = ThreatLevel.MEDIUM,
                            confidence = response.confidence,
                            warnings = listOf(SecurityWarning("Uncertain", "Model uncertain about this app", ThreatLevel.MEDIUM))
                        )
                    }
            } catch (e: Exception) {
                ScanResult.Error("Network/ML analysis failed: ${e.message}")
            }
        }
    }


    private fun calculateBaseRisk(features: MLFeatures): Float {
        var riskScore = 0.0f


        riskScore += features.permissions.count { isDangerousPermission(it) } * 0.1f

        if (features.fileSize > 100 * 1024 * 1024) riskScore += 0.2f
        if (features.fileSize < 1 * 1024 * 1024) riskScore += 0.3f

        riskScore += features.suspiciousStrings.size * 0.05f

        riskScore += features.nativeLibraries.size * 0.08f

        return riskScore.coerceIn(0.0f, 1.0f)
    }

    private fun isDangerousPermission(permission: String): Boolean {
        val dangerousPermissions = listOf(
            "android.permission.READ_SMS",
            "android.permission.SEND_SMS",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS"
        )
        return dangerousPermissions.any { permission.contains(it, ignoreCase = true) }
    }

    private fun analyzeThreatTypes(features: MLFeatures): List<DetectedThreat> {
        val threats = mutableListOf<DetectedThreat>()


        val dangerousPerms = features.permissions.filter { isDangerousPermission(it) }
        if (dangerousPerms.isNotEmpty()) {
            threats.add(DetectedThreat(
                type = ThreatType.DANGEROUS_PERMISSIONS,
                severity = ThreatLevel.MEDIUM,
                description = "App requests ${dangerousPerms.size} sensitive permissions",
                details = "Permissions: ${dangerousPerms.joinToString(", ")}"
            ))
        }


        if (features.suspiciousStrings.isNotEmpty()) {
            threats.add(DetectedThreat(
                type = ThreatType.SUSPICIOUS_BEHAVIOR,
                severity = ThreatLevel.HIGH,
                description = "Contains potentially malicious code patterns",
                details = "Found ${features.suspiciousStrings.size} suspicious strings"
            ))
        }


        if (features.permissions.any { it.contains("LOCATION") || it.contains("CONTACTS") }) {
            threats.add(DetectedThreat(
                type = ThreatType.PRIVACY_RISK,
                severity = ThreatLevel.MEDIUM,
                description = "Potential privacy risk detected",
                details = "App accesses sensitive user data"
            ))
        }

        return threats
    }





    fun extractFeatures(apkPath: String): MLFeatures {
        val extracted = MLFeatures(
            permissions = extractPermissionsSimulation(),
            apiCalls = extractAPICallsSimulation(),
            intentFilters = extractIntentFiltersSimulation(),
            fileSize = File(apkPath).length(),
            certificateInfo = "Simulated Certificate",
            nativeLibraries = listOf("libnative.so", "libencrypt.so"),
            suspiciousStrings = extractSuspiciousStringsSimulation(),
            entropy = 0.75
        )

        val featureVector = com.app.maldroid.mlmodel.FeatureMapper.mapToFeatureVector(extracted)

        println("Feature Vector for backend: $featureVector")

        return extracted
    }

    private fun extractPermissionsSimulation(): List<String> {
        return listOf(
            "android.permission.GET_ACCOUNTS",
            "android.permission.AUTHENTICATE_ACCOUNTS",
            "android.permission.MANAGE_ACCOUNTS",
            "android.permission.USE_CREDENTIALS",
            "android.permission.READ_PROFILE",
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.WAKE_LOCK",
            "android.permission.RECORD_AUDIO",
            "android.permission.RECEIVE_BOOT_COMPLETED",
            "android.permission.RECEIVE_WAP_PUSH",
            "android.permission.READ_CALENDAR",
            "android.permission.FLASHLIGHT",
            "android.permission.SET_WALLPAPER",
            "android.permission.ACCESS_LOCATION_EXTRA_COMMANDS",
            "android.permission.BROADCAST_STICKY",
            "android.permission.NFC",
            "android.permission.CHANGE_WIFI_MULTICAST_STATE",
            "android.permission.SET_ALARM",
            "android.permission.SUBSCRIBED_FEEDS_WRITE",
            "android.permission.PROCESS_OUTGOING_CALLS",
            "android.permission.READ_USER_DICTIONARY",
            "android.permission.INTERNET",
        )
    }

    private fun extractAPICallsSimulation(): List<String> {
        return listOf(
            "getDeviceId",
            "getSubscriberId",
            "exec",
            "mount",
            "TelephonyManager.getLine1Number",
            "TelephonyManager.getSubscriberId",
            "android.telephony.SmsManager",
            "android.telephony.gsm.SmsManager",
            "android.os.Binder",
            "transact",
            "getBinder",
            "getCallingUid",
            "bindService",
            "onServiceConnected",
            "ServiceConnection",
            "attachInterface",
            "createSubprocess",
            "sendDataMessage",
            "abortBroadcast",
            "chmod",
            "chown",
            "remount",
            "Ljava.lang.Class.getMethods",
            "Ljava.lang.Class.getCanonicalName",
            "Ljava.lang.Class.cast",
            "Ljava.lang.Class.getField",
            "Ljava.lang.Class.getDeclaredField",
            "Ljava.net.URLDecoder",
            "Ljavax.crypto.spec.SecretKeySpec",
            "Landroid.content.Context.registerReceiver",
            "Landroid.content.Context.unregisterReceiver",
            "ClassLoader",
            "DexClassLoader",
            "URLClassLoader","TelephonyManager.getDeviceId",
            "TelephonyManager.getSimSerialNumber",
            "android.content.pm.Signature",
            "android.intent.action.PACKAGE_ADDED",
            "android.intent.action.PACKAGE_REMOVED",
            "android.intent.action.NEW_OUTGOING_CALL",
            "android.intent.action.TIMEZONE_CHANGED",
            "android.intent.action.SEND_MULTIPLE",
            "android.intent.action.ACTION_POWER_DISCONNECTED",
            "Runtime.getRuntime",
            "Runtime.load",
            "System.loadLibrary",
            "divideMessage",
            "HttpGet.init",
            "PackageInstaller",
            "KeySpec",
            "SecretKey",
            "Ljavax.crypto.Cipher",
            "PathClassLoader",
        )
    }

    private fun extractIntentFiltersSimulation(): List<String> {
        return listOf(
            "android.intent.action.MAIN",
            "android.intent.category.LAUNCHER",
            "android.intent.action.BOOT_COMPLETED",
            "android.intent.action.SEND",
            "android.intent.action.PACKAGE_REPLACED",
            "android.intent.action.TIME_SET",
            "android.intent.action.USER_PRESENT",
            "android.intent.action.SCREEN_ON",
            "android.intent.action.SCREEN_OFF","android.intent.action.PACKAGE_ADDED",
            "android.intent.action.PACKAGE_REMOVED",
            "android.intent.action.TIMEZONE_CHANGED",
            "android.intent.action.NEW_OUTGOING_CALL",
            "android.intent.action.SEND_MULTIPLE",
            "android.intent.action.ACTION_POWER_DISCONNECTED",
        )
    }

    private fun extractSuspiciousStringsSimulation(): List<String> {
        return listOf(
            "root",
            "su",
            "mount",
            "system",
            "remount",
            "chmod",
            "chown",
            "createSubprocess",
            "getDeviceId",
            "getSubscriberId",
            "exec",
            "runtime.exec",
            "system/bin",
            "/data/local",
            "android.permission.WRITE_SECURE_SETTINGS",
            "Landroid.content.Context.registerReceiver",
            "Landroid.content.Context.unregisterReceiver"
        )
    }
}