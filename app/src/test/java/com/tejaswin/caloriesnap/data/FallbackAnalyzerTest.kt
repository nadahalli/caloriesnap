package com.tejaswin.caloriesnap.data

import android.graphics.Bitmap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FallbackAnalyzerTest {

    private val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    @Test
    fun `on-device success uses on-device for estimates`() = runTest {
        val onDevice = FakeAnalyzer(
            downloadFlow = flow { emit(DownloadProgress.Completed) },
            result = FoodEstimate("On-device", 100, 5f, 10f, 3f),
        )
        val cloud = FakeAnalyzer(
            downloadFlow = flow { emit(DownloadProgress.Completed) },
            result = FoodEstimate("Cloud", 200, 10f, 20f, 6f),
        )

        val fallback = FallbackAnalyzer(onDevice, cloud)
        val progress = fallback.download().toList()

        assertEquals(listOf(DownloadProgress.Completed), progress)

        val estimate = fallback.estimate(bitmap)
        assertEquals("On-device", estimate.foodName)
    }

    @Test
    fun `on-device failure falls back to cloud and emits Completed`() = runTest {
        val onDevice = FakeAnalyzer(
            downloadFlow = flow { emit(DownloadProgress.Failed("unsupported")) },
            result = FoodEstimate("On-device", 100, 5f, 10f, 3f),
        )
        val cloud = FakeAnalyzer(
            downloadFlow = flow { emit(DownloadProgress.Completed) },
            result = FoodEstimate("Cloud", 200, 10f, 20f, 6f),
        )

        val fallback = FallbackAnalyzer(onDevice, cloud)
        val progress = fallback.download().toList()

        assertEquals(listOf(DownloadProgress.Completed), progress)

        val estimate = fallback.estimate(bitmap)
        assertEquals("Cloud", estimate.foodName)
    }

    @Test
    fun `on-device progress events are forwarded`() = runTest {
        val onDevice = FakeAnalyzer(
            downloadFlow = flow {
                emit(DownloadProgress.Started(1000L))
                emit(DownloadProgress.InProgress(500L))
                emit(DownloadProgress.Completed)
            },
            result = FoodEstimate("On-device", 100, 5f, 10f, 3f),
        )
        val cloud = FakeAnalyzer(
            downloadFlow = flow { emit(DownloadProgress.Completed) },
            result = FoodEstimate("Cloud", 200, 10f, 20f, 6f),
        )

        val fallback = FallbackAnalyzer(onDevice, cloud)
        val progress = fallback.download().toList()

        assertEquals(
            listOf(
                DownloadProgress.Started(1000L),
                DownloadProgress.InProgress(500L),
                DownloadProgress.Completed,
            ),
            progress,
        )
    }

    private class FakeAnalyzer(
        private val downloadFlow: Flow<DownloadProgress>,
        private val result: FoodEstimate,
    ) : FoodAnalyzer {
        override fun download(): Flow<DownloadProgress> = downloadFlow
        override suspend fun estimate(bitmap: Bitmap, extras: List<String>): FoodEstimate = result
        override fun close() {}
    }
}
