package com.tejaswin.caloriesnap

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tejaswin.caloriesnap.data.AppDatabase
import com.tejaswin.caloriesnap.data.CalorieEstimator
import com.tejaswin.caloriesnap.data.FallbackAnalyzer
import com.tejaswin.caloriesnap.data.FirebaseAnalyzer
import com.tejaswin.caloriesnap.data.RoomFoodRepository

class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = AppDatabase.getInstance(context)
        val repository = RoomFoodRepository(db.foodDao())
        val analyzer = FallbackAnalyzer(
            onDevice = CalorieEstimator(),
            cloud = FirebaseAnalyzer(),
        )
        return MainViewModel(analyzer, repository) as T
    }
}
