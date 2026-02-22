package com.tejaswin.caloriesnap

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tejaswin.caloriesnap.data.DownloadProgress
import com.tejaswin.caloriesnap.data.FoodAnalyzer
import com.tejaswin.caloriesnap.data.FoodEntry
import com.tejaswin.caloriesnap.data.FoodEstimate
import com.tejaswin.caloriesnap.data.FoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ModelState { NOT_READY, DOWNLOADING, READY, ERROR }

class MainViewModel(
    private val analyzer: FoodAnalyzer,
    private val repository: FoodRepository,
) : ViewModel() {

    private val _modelState = MutableStateFlow(ModelState.NOT_READY)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    private val _modelError = MutableStateFlow<String?>(null)
    val modelError: StateFlow<String?> = _modelError.asStateFlow()

    // Download progress: 0f..1f
    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    fun downloadModel() {
        if (_modelState.value == ModelState.DOWNLOADING) return
        _modelState.value = ModelState.DOWNLOADING
        _modelError.value = null
        _downloadProgress.value = 0f
        var totalBytes = 0L
        viewModelScope.launch {
            try {
                analyzer.download().collect { progress ->
                    when (progress) {
                        is DownloadProgress.Started -> {
                            totalBytes = progress.totalBytes
                            _downloadProgress.value = 0f
                        }
                        is DownloadProgress.InProgress -> {
                            if (totalBytes > 0) {
                                _downloadProgress.value =
                                    (progress.bytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
                            }
                        }
                        is DownloadProgress.Completed -> {
                            _downloadProgress.value = 1f
                            _modelState.value = ModelState.READY
                        }
                        is DownloadProgress.Failed -> {
                            _modelState.value = ModelState.ERROR
                            _modelError.value = progress.message
                        }
                    }
                }
            } catch (e: Exception) {
                _modelState.value = ModelState.ERROR
                _modelError.value = e.message ?: "Download failed"
            }
        }
    }

    // Current capture state
    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap: StateFlow<Bitmap?> = _capturedBitmap.asStateFlow()

    private val _capturedPhotoPath = MutableStateFlow<String?>(null)

    private val _estimate = MutableStateFlow<FoodEstimate?>(null)
    val estimate: StateFlow<FoodEstimate?> = _estimate.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _selectedExtras = MutableStateFlow<Set<String>>(emptySet())
    val selectedExtras: StateFlow<Set<String>> = _selectedExtras.asStateFlow()

    // Today's entries
    val todayEntries: StateFlow<List<FoodEntry>> = run {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val endOfDay = startOfDay + 86_400_000L
        repository.getEntriesForDay(startOfDay, endOfDay)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun onPhotoCaptured(bitmap: Bitmap, photoPath: String) {
        _capturedBitmap.value = bitmap
        _capturedPhotoPath.value = photoPath
        _selectedExtras.value = emptySet()
        analyze(bitmap, emptyList())
    }

    fun toggleExtra(extra: String) {
        val current = _selectedExtras.value.toMutableSet()
        if (extra in current) current.remove(extra) else current.add(extra)
        _selectedExtras.value = current

        val bitmap = _capturedBitmap.value ?: return
        analyze(bitmap, current.toList())
    }

    private fun analyze(bitmap: Bitmap, extras: List<String>) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                _estimate.value = analyzer.estimate(bitmap, extras)
            } catch (e: Exception) {
                _estimate.value = FoodEstimate(
                    foodName = "Analysis failed",
                    calories = 0,
                    proteinG = 0f,
                    carbsG = 0f,
                    fatG = 0f,
                )
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun save() {
        val est = _estimate.value ?: return
        val path = _capturedPhotoPath.value ?: return
        viewModelScope.launch {
            repository.save(
                FoodEntry(
                    photoPath = path,
                    foodName = est.foodName,
                    calories = est.calories,
                    proteinG = est.proteinG,
                    carbsG = est.carbsG,
                    fatG = est.fatG,
                    extras = _selectedExtras.value.joinToString(", "),
                )
            )
            clearCapture()
        }
    }

    fun clearCapture() {
        _capturedBitmap.value = null
        _capturedPhotoPath.value = null
        _estimate.value = null
        _selectedExtras.value = emptySet()
    }

    override fun onCleared() {
        super.onCleared()
        analyzer.close()
    }
}
