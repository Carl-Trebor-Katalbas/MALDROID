package com.app.maldroid.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
data class ApkScanResult(
    val fileName: String?,
    val filePath: String,
    val fileSize: Long,
    val packageName: String?,
    val versionName: String?,
    val versionCode: Int,
    val permissions: List<String>?,
    val scanTimestamp: Long = System.currentTimeMillis(),
    val scanResult: String = "No risks found",
    val isSafe: Boolean = true,
    val scanDuration: Long = 0,
    val detectedThreats: List<String> = emptyList()
) : Parcelable, Serializable