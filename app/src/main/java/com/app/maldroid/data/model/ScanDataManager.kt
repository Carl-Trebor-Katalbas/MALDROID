package com.app.maldroid.data

import android.content.Context
import android.util.Log
import com.app.maldroid.data.model.AppScanResult
import com.app.maldroid.data.model.ApkScanResult
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

object ScanDataManager {

    private val scannedApps = mutableListOf<AppScanResult>()

    private val apkScanResults = mutableListOf<ApkScanResult>()

    private const val HISTORY_FILE_NAME = "scan_history_cache.dat"


    fun getScannedApps(): List<AppScanResult> {
        return scannedApps.toList()
    }

    fun addAppResult(app: AppScanResult, context: Context) {
        scannedApps.add(app)
        saveHistoryToCache(context)
    }

    fun addApkScanResult(result: ApkScanResult, context: Context) {
        apkScanResults.add(result)
        saveHistoryToCache(context)
    }

    fun getApkScanResults(): List<ApkScanResult> {
        return apkScanResults.toList()
    }

    fun clearAll(context: Context) {
        scannedApps.clear()
        apkScanResults.clear()
        try {
            val file = File(context.cacheDir, HISTORY_FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e("ScanDataManager", "Error deleting cache file", e)
        }
    }

    private fun saveHistoryToCache(context: Context) {
        try {
            val cacheFile = File(context.cacheDir, HISTORY_FILE_NAME)
            val objectOut = ObjectOutputStream(cacheFile.outputStream())
            objectOut.writeObject(scannedApps)
            objectOut.writeObject(apkScanResults)
            objectOut.close()
            Log.d("ScanDataManager", "History saved: ${cacheFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("ScanDataManager", "Failed to save history", e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun loadHistoryFromCache(context: Context) {
        val file = File(context.cacheDir, HISTORY_FILE_NAME)
        try {
            if (!file.exists()) return

            val objectIn = ObjectInputStream(file.inputStream())

            val savedApps = objectIn.readObject() as? MutableList<AppScanResult>
            val savedApks = objectIn.readObject() as? MutableList<ApkScanResult>

            if (savedApps != null) {
                scannedApps.clear()
                scannedApps.addAll(savedApps)
            }
            if (savedApks != null) {
                apkScanResults.clear()
                apkScanResults.addAll(savedApks)
            }

            objectIn.close()
            Log.d("ScanDataManager", "History loaded successfully.")
        } catch (e: Exception) {
            Log.e("ScanDataManager", "Failed to load history (Schema might have changed)", e)
            if (file.exists()) {
                file.delete()
            }
        }
    }
}