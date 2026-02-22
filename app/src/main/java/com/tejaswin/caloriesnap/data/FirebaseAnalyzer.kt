package com.tejaswin.caloriesnap.data

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FirebaseAnalyzer : FoodAnalyzer {

    private val model = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel(
            modelName = "gemini-2.5-flash",
            generationConfig = generationConfig {
                temperature = 0.2f
                topK = 16
                maxOutputTokens = 256
            },
        )

    override fun download(): Flow<DownloadProgress> = flowOf(DownloadProgress.Completed)

    override suspend fun estimate(bitmap: Bitmap, extras: List<String>): FoodEstimate {
        val prompt = EstimateParser.buildPrompt(extras)
        val response = model.generateContent(
            content {
                image(bitmap)
                text(prompt)
            }
        )
        val text = response.text.orEmpty()
        return EstimateParser.parseResponse(text)
    }

    override fun close() {
        // No resources to release for the cloud model.
    }
}
