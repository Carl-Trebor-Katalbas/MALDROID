package com.app.maldroid.data.model

import android.graphics.Bitmap
import java.util.*

data class ApkFile(
    val id: String,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val packageName: String?,
    val versionName: String?,
    val icon: Bitmap?,
    val lastModified: Date,
    val isInstalledApp: Boolean = false
) {
    fun getFormattedSize(): String {
        return when {
            fileSize >= 1024 * 1024 * 1024 -> "${fileSize / (1024 * 1024 * 1024)} GB"
            fileSize >= 1024 * 1024 -> "${fileSize / (1024 * 1024)} MB"
            fileSize >= 1024 -> "${fileSize / 1024} KB"
            else -> "$fileSize B"
        }
    }
}