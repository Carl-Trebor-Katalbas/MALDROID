package com.app.maldroid.data.model

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.app.maldroid.data.ScanDataManager
import java.io.File
import kotlin.random.Random

class MLAppScanner(private val context: Context? = null) {

    data class MLAppFeatures(
        val appName: String = "",
        val packageName: String = "",
        val permissions: List<String> = emptyList(),
        val targetSdkVersion: Int = 0,
        val isSystemApp: Boolean = false,
        val fileSize: Long = 0,
        val versionCode: Int = 0,
        val installedTime: Long = 0,
        val updatedTime: Long = 0,
        val suspiciousPatterns: List<String> = emptyList(),
        val permissionRiskScore: Float = 0.0f,
        val behaviorRiskScore: Float = 0.0f
    )


    fun isCacheValid(packageName: String): Boolean {
        if (context == null) return false
        return try {
            val packageManager = context.packageManager

            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            val currentSystemUpdateTime = packageInfo.lastUpdateTime
            val savedApp = ScanDataManager.getScannedApps().find { it.packageName == packageName }
            savedApp != null && savedApp.scanTimestamp >= currentSystemUpdateTime
        } catch (e: Exception) {
            false
        }
    }


    fun isApkCacheValid(filePath: String?, scanTimestamp: Long): Boolean {
        if (filePath.isNullOrEmpty()) return false
        val file = File(filePath)

        return file.exists() && scanTimestamp >= file.lastModified()
    }


    fun extractAppFeatures(appInfo: ApplicationInfo, packageManager: PackageManager): MLAppFeatures {
        val appName = appInfo.loadLabel(packageManager).toString()
        val packageName = appInfo.packageName
        val permissions = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
                    .requestedPermissions?.toList() ?: emptyList()
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                    .requestedPermissions?.toList() ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
        val packageInfo = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
        } catch (e: Exception) {
            null
        }

        val permissionRiskScore = calculatePermissionRiskScore(permissions)
        val behaviorRiskScore = calculateBehaviorRiskScore(appInfo, packageManager)
        val suspiciousPatterns = detectSuspiciousPatterns(appName, packageName, permissions)

        return MLAppFeatures(
            appName = appName,
            packageName = packageName,
            permissions = permissions,
            targetSdkVersion = appInfo.targetSdkVersion,
            isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
            fileSize = getFileSize(appInfo),
            versionCode = packageInfo?.versionCode ?: 0,
            installedTime = packageInfo?.firstInstallTime ?: 0,
            updatedTime = packageInfo?.lastUpdateTime ?: 0,
            suspiciousPatterns = suspiciousPatterns,
            permissionRiskScore = permissionRiskScore,
            behaviorRiskScore = behaviorRiskScore
        )
    }

    private fun getFileSize(appInfo: ApplicationInfo): Long {
        return try {
            val file = java.io.File(appInfo.sourceDir)
            file.length()
        } catch (e: Exception) {
            0L
        }
    }

    private fun detectSuspiciousPatterns(appName: String, packageName: String, permissions: List<String>): List<String> {
        val patterns = mutableListOf<String>()

        val suspiciousKeywords = listOf("crack", "hack", "mod", "cheat", "free", "premium", "unlocked")
        val appNameLower = appName.lowercase()
        val packageNameLower = packageName.lowercase()

        suspiciousKeywords.forEach { keyword ->
            if (appNameLower.contains(keyword) || packageNameLower.contains(keyword)) {
                patterns.add("Suspicious keyword: $keyword")
            }
        }

        val dangerousPerms = permissions.filter { isDangerousPermission(it) }
        if (dangerousPerms.size > 3) {
            patterns.add("Excessive dangerous permissions: ${dangerousPerms.size}")
        }
        val hasSmsAndLocation = permissions.any { it.contains("SMS", ignoreCase = true) } &&
                permissions.any { it.contains("LOCATION", ignoreCase = true) }
        if (hasSmsAndLocation) {
            patterns.add("High-risk permission combination: SMS + Location")
        }

        return patterns
    }

    fun analyzeInstalledApp(features: MLAppFeatures): ScanResult {
        return try {
            val (prediction, confidence) = simulateMLPrediction(features)

            when {
                prediction > 0.7 && confidence > 0.6 -> {
                    val threats = analyzeAppThreatTypes(features)
                    ScanResult.Malicious(
                        threatLevel = ThreatLevel.HIGH,
                        confidence = confidence,
                        detectedThreats = threats,
                        recommendations = generateAppRecommendations(threats)
                    )
                }
                prediction > 0.4 && confidence > 0.5 -> {
                    val warnings = generateAppWarnings(features)
                    ScanResult.Suspicious(
                        threatLevel = ThreatLevel.MEDIUM,
                        confidence = confidence,
                        warnings = warnings
                    )
                }
                else -> {
                    ScanResult.Clean(
                        confidence = confidence,
                        notes = generateAppSecurityNotes(features)
                    )
                }
            }
        } catch (e: Exception) {
            ScanResult.Error("ML app analysis failed: ${e.message}")
        }
    }

    private fun simulateMLPrediction(features: MLAppFeatures): Pair<Float, Float> {
        val baseRisk = calculateAppRisk(features)
        val prediction = baseRisk.coerceIn(0.0f, 1.0f)
        val confidence = 0.7f + Random.nextFloat() * 0.3f
        return Pair(prediction, confidence)
    }

    private fun calculateAppRisk(features: MLAppFeatures): Float {
        var riskScore = 0.0f
        riskScore += features.permissionRiskScore * 0.4f
        riskScore += features.behaviorRiskScore * 0.3f
        if (!features.isSystemApp) riskScore += 0.1f
        if (features.targetSdkVersion < 28) riskScore += 0.1f
        riskScore += features.suspiciousPatterns.size * 0.05f
        if (features.fileSize < 1 * 1024 * 1024) riskScore += 0.2f
        return riskScore.coerceIn(0.0f, 1.0f)
    }

    private fun analyzeAppThreatTypes(features: MLAppFeatures): List<DetectedThreat> {
        val threats = mutableListOf<DetectedThreat>()
        val dangerousPerms = features.permissions.filter { isDangerousPermission(it) }

        if (dangerousPerms.isNotEmpty()) {
            threats.add(DetectedThreat(
                type = ThreatType.DANGEROUS_PERMISSIONS,
                severity = if (dangerousPerms.size > 3) ThreatLevel.HIGH else ThreatLevel.MEDIUM,
                description = "App requests ${dangerousPerms.size} sensitive permissions",
                details = "Permissions: ${dangerousPerms.take(5).joinToString(", ")}"
            ))
        }

        if (features.suspiciousPatterns.isNotEmpty()) {
            threats.add(DetectedThreat(
                type = ThreatType.SUSPICIOUS_BEHAVIOR,
                severity = ThreatLevel.MEDIUM,
                description = "App shows suspicious characteristics",
                details = "Patterns: ${features.suspiciousPatterns.joinToString(", ")}"
            ))
        }

        return threats
    }

    private fun generateAppWarnings(features: MLAppFeatures): List<SecurityWarning> {
        val warnings = mutableListOf<SecurityWarning>()
        if (features.permissions.size > 20) {
            warnings.add(SecurityWarning(
                title = "Excessive Permissions",
                description = "App requests many permissions",
                severity = ThreatLevel.MEDIUM
            ))
        }
        return warnings
    }

    private fun generateAppSecurityNotes(features: MLAppFeatures): List<SecurityNote> {
        return listOf(
            SecurityNote(
                title = "Standard App Profile",
                description = "App shows typical characteristics"
            )
        )
    }

    private fun generateAppRecommendations(threats: List<DetectedThreat>): List<String> {
        return listOf("Review app permissions in Settings")
    }

    private fun isDangerousPermission(permission: String): Boolean {
        val dangerousPermissions = listOf(
            "android.permission.READ_SMS", "android.permission.SEND_SMS",
            "android.permission.ACCESS_FINE_LOCATION", "android.permission.CAMERA"
        )
        return dangerousPermissions.any { permission.contains(it, ignoreCase = true) }
    }

    fun calculatePermissionRiskScore(permissions: List<String>): Float {
        val dangerousCount = permissions.count { isDangerousPermission(it) }
        return if (permissions.isEmpty()) 0.0f else (dangerousCount.toFloat() / permissions.size)
    }

    fun calculateBehaviorRiskScore(appInfo: ApplicationInfo, packageManager: PackageManager): Float {
        var riskScore = 0.0f
        val appName = appInfo.loadLabel(packageManager).toString().lowercase()
        val packageName = appInfo.packageName.lowercase()

        val suspiciousPatterns = listOf("crack", "hack", "mod", "cheat")
        val foundPatterns = suspiciousPatterns.count { pattern ->
            appName.contains(pattern) || packageName.contains(pattern)
        }

        riskScore += foundPatterns * 0.1f
        if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) riskScore += 0.1f
        return riskScore.coerceIn(0.0f, 1.0f)
    }
}