package com.app.maldroid.data.model

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Environment
import android.util.Log
import java.io.File
import java.util.*

class FileManager {

    fun scanForApkFiles(context: Context): List<ApkFile> {
        val allFiles = mutableListOf<ApkFile>()

        try {
            allFiles.addAll(getInstalledApps(context))
            Log.d("FileManager", "Found ${getInstalledApps(context).size} installed apps")
        } catch (e: Exception) {
            Log.e("FileManager", "Error getting installed apps: ${e.message}")
        }

        try {
            val downloadFiles = scanDownloadsDirectory()
            allFiles.addAll(downloadFiles)
            Log.d("FileManager", "Found ${downloadFiles.size} APK files in downloads")
        } catch (e: SecurityException) {
            Log.w("FileManager", "Storage permission not granted for downloads scan")
        } catch (e: Exception) {
            Log.e("FileManager", "Error scanning downloads: ${e.message}")
        }

        Log.d("FileManager", "Total files found: ${allFiles.size}")
        return allFiles
    }

    private fun scanDownloadsDirectory(): List<ApkFile> {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return scanDirectoryForApk(downloadsDir)
    }

    private fun scanDirectoryForApk(directory: File): List<ApkFile> {
        if (!directory.exists() || !directory.isDirectory) return emptyList()

        return directory.listFiles { file ->
            file.extension.equals("apk", ignoreCase = true)
        }?.map { file ->
            ApkFile(
                id = UUID.randomUUID().toString(),
                fileName = file.name,
                filePath = file.absolutePath,
                fileSize = file.length(),
                packageName = extractPackageNameFromPath(file.absolutePath),
                versionName = null,
                icon = null,
                lastModified = Date(file.lastModified())
            )
        } ?: emptyList()
    }

    private fun getInstalledApps(context: Context): List<ApkFile> {
        val packageManager = context.packageManager
        val installedPackages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)

        return installedPackages.mapNotNull { packageInfo ->
            try {
                val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null
                val sourceDir = appInfo.sourceDir ?: return@mapNotNull null

                val sourceFile = File(sourceDir)
                if (!sourceFile.exists()) return@mapNotNull null

                val appLabel = packageManager.getApplicationLabel(appInfo).toString()
                val fileName = if (appLabel.isNotBlank()) {
                    "$appLabel.apk"
                } else {
                    "${appInfo.packageName ?: "unknown"}.apk"
                }

                ApkFile(
                    id = packageInfo.packageName ?: UUID.randomUUID().toString(),
                    fileName = fileName,
                    filePath = sourceDir,
                    fileSize = sourceFile.length(),
                    packageName = packageInfo.packageName ?: "unknown",
                    versionName = packageInfo.versionName,
                    icon = drawableToBitmap(appInfo.loadIcon(packageManager)),
                    lastModified = Date(sourceFile.lastModified()),
                    isInstalledApp = true
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun extractPackageNameFromPath(filePath: String): String {
        return File(filePath).nameWithoutExtension
    }

    fun getLargeFiles(files: List<ApkFile>, thresholdMB: Long = 100): List<ApkFile> {
        val thresholdBytes = thresholdMB * 1024 * 1024
        return files.filter { it.fileSize > thresholdBytes }
            .sortedByDescending { it.fileSize }
    }

    fun getRecentFiles(files: List<ApkFile>, days: Int = 7): List<ApkFile> {
        val cutoffDate = Date(System.currentTimeMillis() - days * 24 * 60 * 60 * 1000L)
        return files.filter { it.lastModified.after(cutoffDate) }
            .sortedByDescending { it.lastModified }
    }
}