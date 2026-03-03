package com.app.maldroid.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
data class AppScanResult(
    val name: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val permissions: List<String>,
    val isSystemApp: Boolean,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val apkPath: String,
    val targetSdkVersion: Int,
    val scanTimestamp: Long = System.currentTimeMillis(),
    val isMalicious: Boolean = false,
    val mlConfidence: Float = 0.0f,
    val detectionMethod: String = "RULE_BASED",
    val threatLevel: String = "LOW",
    val detectedThreatTypes: List<String> = emptyList(),
    val securityWarnings: List<String> = emptyList(),
    val recommendations: List<String> = emptyList()
) : Parcelable, Serializable