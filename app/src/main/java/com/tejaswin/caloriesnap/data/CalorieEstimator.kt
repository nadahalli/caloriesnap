package com.tejaswin.caloriesnap.data

import android.graphics.Bitmap
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CalorieEstimator : FoodAnalyzer {

    private val generativeModel: GenerativeModel = Generation.getClient()

    override fun download(): Flow<DownloadProgress> =
        generativeModel.download().map { status ->
            when (status) {
                is DownloadStatus.DownloadStarted ->
                    DownloadProgress.Started(status.bytesToDownload)
                is DownloadStatus.DownloadProgress ->
                    DownloadProgress.InProgress(status.totalBytesDownloaded)
                is DownloadStatus.DownloadCompleted ->
                    DownloadProgress.Completed
                is DownloadStatus.DownloadFailed ->
                    DownloadProgress.Failed(status.e.message ?: "Download failed")
            }
        }

    override suspend fun estimate(bitmap: Bitmap, extras: List<String>): FoodEstimate {
        val prompt = EstimateParser.buildPrompt(extras)

        val response = generativeModel.generateContent(
            generateContentRequest(ImagePart(bitmap), TextPart(prompt)) {
                temperature = 0.2f
                topK = 16
                maxOutputTokens = 256
            }
        )

        val text = response.candidates.firstOrNull()?.text ?: ""
        return EstimateParser.parseResponse(text)
    }

    override fun close() {
        generativeModel.close()
    }
}
