package com.tejaswin.caloriesnap

import android.content.Context
import android.graphics.Bitmap
import com.tejaswin.caloriesnap.data.DownloadProgress
import com.tejaswin.caloriesnap.data.FoodAnalyzer
import com.tejaswin.caloriesnap.data.FoodEntry
import com.tejaswin.caloriesnap.data.FoodEstimate
import com.tejaswin.caloriesnap.data.FoodRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeAnalyzer: FakeFoodAnalyzer
    private lateinit var fakeRepository: FakeFoodRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAnalyzer = FakeFoodAnalyzer()
        fakeRepository = FakeFoodRepository()
        val prefs = RuntimeEnvironment.getApplication()
            .getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        viewModel = MainViewModel(fakeAnalyzer, fakeRepository, prefs)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `downloadModel transitions through states on success`() = runTest(testDispatcher) {
        viewModel.downloadModel()
        advanceUntilIdle()

        assertEquals(ModelState.READY, viewModel.modelState.value)
        assertEquals(1f, viewModel.downloadProgress.value, 0.01f)
    }

    @Test
    fun `downloadModel sets ERROR on failure`() = runTest(testDispatcher) {
        fakeAnalyzer.downloadResult = flow {
            emit(DownloadProgress.Failed("network error"))
        }

        viewModel.downloadModel()
        advanceUntilIdle()

        assertEquals(ModelState.ERROR, viewModel.modelState.value)
        assertEquals("network error", viewModel.modelError.value)
    }

    @Test
    fun `save persists entry to repository and clears capture`() = runTest(testDispatcher) {
        fakeAnalyzer.estimateResult = FoodEstimate("Salad", 200, 10f, 20f, 5f)

        viewModel.onPhotoCaptured(TEST_BITMAP, "/photos/test.jpg")
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, fakeRepository.saved.size)
        val entry = fakeRepository.saved.first()
        assertEquals("Salad", entry.foodName)
        assertEquals(200, entry.calories)
        assertEquals("/photos/test.jpg", entry.photoPath)
        assertNull(viewModel.capturedBitmap.value)
        assertNull(viewModel.estimate.value)
    }

    @Test
    fun `analyze sets error estimate on failure`() = runTest(testDispatcher) {
        fakeAnalyzer.estimateResult = null // will throw

        viewModel.onPhotoCaptured(TEST_BITMAP, "/photos/test.jpg")
        advanceUntilIdle()

        assertEquals("Analysis failed", viewModel.estimate.value?.foodName)
    }

    @Test
    fun `clearCapture resets all capture state`() = runTest(testDispatcher) {
        fakeAnalyzer.estimateResult = FoodEstimate("Burger", 500, 25f, 40f, 30f)

        viewModel.onPhotoCaptured(TEST_BITMAP, "/photos/test.jpg")
        advanceUntilIdle()

        viewModel.clearCapture()

        assertNull(viewModel.capturedBitmap.value)
        assertNull(viewModel.estimate.value)
        assertEquals(emptySet<String>(), viewModel.selectedExtras.value)
    }

    companion object {
        val TEST_BITMAP: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }
}

// -- Fakes --

class FakeFoodAnalyzer : FoodAnalyzer {
    var estimateResult: FoodEstimate? = FoodEstimate("Test Food", 100, 10f, 20f, 5f)
    var downloadResult: Flow<DownloadProgress> = flow {
        emit(DownloadProgress.Started(1000L))
        emit(DownloadProgress.InProgress(500L))
        emit(DownloadProgress.Completed)
    }
    var closed = false

    override suspend fun estimate(bitmap: Bitmap, extras: List<String>): FoodEstimate =
        estimateResult ?: throw RuntimeException("Estimate failed")

    override fun download(): Flow<DownloadProgress> = downloadResult

    override fun close() { closed = true }
}

class FakeFoodRepository : FoodRepository {
    val saved = mutableListOf<FoodEntry>()
    private val entries = MutableStateFlow<List<FoodEntry>>(emptyList())

    override suspend fun save(entry: FoodEntry) { saved.add(entry) }

    override fun getEntriesForDay(startOfDay: Long, endOfDay: Long): Flow<List<FoodEntry>> = entries

    override fun getAllEntries(): Flow<List<FoodEntry>> = entries

    override suspend fun deleteById(id: Int) {
        entries.value = entries.value.filter { it.id != id }
    }
}
