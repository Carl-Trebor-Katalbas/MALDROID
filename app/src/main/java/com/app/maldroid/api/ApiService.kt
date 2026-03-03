package com.app.maldroid.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Streaming

data class AnalyzeRequest(
    val permissions: List<String>,
    val apiCalls: List<String>,
    val intentFilters: List<String>,
    val fileSize: Long,
    val certificateInfo: String,
    val nativeLibraries: List<String>,
    val suspiciousStrings: List<String>,
    val entropy: Double
)
data class AnalyzeResponse(val prediction: Int, val confidence: Float, val label: String, val triggers: List<String>?,val permissions: List<String>? = emptyList())

interface ApiService {
    @Multipart
    @POST("analyze")
    @Streaming
    suspend fun analyzeApk(@Part apkFile: MultipartBody.Part): AnalyzeResponse

    @Multipart
    @POST("analyze_split")
    @Streaming
    suspend fun analyzeSplitApk(
        @Part apkFiles: List<MultipartBody.Part>
    ): AnalyzeResponse

    @POST("analyze_batch")
    @Streaming
    suspend fun analyzeAllApk(@Part apkFiles: List<MultipartBody.Part>): AnalyzeResponse

    @POST("cancel_scan")
    @Streaming
    suspend fun cancelScan(): Response<Map<String, String>>
}