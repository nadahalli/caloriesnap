package com.tejaswin.caloriesnap.data

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FallbackAnalyzer(
    private val onDevice: FoodAnalyzer,
    private val cloud: FoodAnalyzer,
) : FoodAnalyzer {

    private var delegate: FoodAnalyzer = onDevice

    override fun download(): Flow<DownloadProgress> = flow {
        try {
            onDevice.download().collect { progress ->
                if (progress is DownloadProgress.Failed) {
                    Log.d(TAG, "On-device download failed: ${progress.message}, switching to cloud")
                    delegate = cloud
                    emit(DownloadProgress.Completed)
                } else {
                    emit(progress)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "On-device download threw: ${e.message}, switching to cloud")
            delegate = cloud
            emit(DownloadProgress.Completed)
        }
    }

    override suspend fun estimate(bitmap: Bitmap, extras: List<String>): FoodEstimate =
        delegate.estimate(bitmap, extras)

    override fun close() {
        delegate.close()
    }

    companion object {
        private const val TAG = "FallbackAnalyzer"
    }
}
