package com.app.maldroid.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class ScanResultItem(
    val appName: String,
    val scanTimestamp: Long,
    val scanResult: String,
    val isSafe: Boolean,
    val packageName: String? = null,
    val filePath: String? = null,
    val scanDuration: Long = 0,
    val threatDetails: List<String> = emptyList(),
    val fullScanResult: @RawValue ScanResult? = null
) : Parcelable

