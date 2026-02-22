package com.tejaswin.caloriesnap.data

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow

interface FoodAnalyzer {
    suspend fun estimate(bitmap: Bitmap, extras: List<String> = emptyList()): FoodEstimate
    fun download(): Flow<DownloadProgress>
    fun close()
}

sealed interface DownloadProgress {
    data class Started(val totalBytes: Long) : DownloadProgress
    data class InProgress(val bytesDownloaded: Long) : DownloadProgress
    data object Completed : DownloadProgress
    data class Failed(val message: String) : DownloadProgress
}
