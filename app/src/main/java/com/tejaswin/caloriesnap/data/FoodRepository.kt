package com.tejaswin.caloriesnap.data

import kotlinx.coroutines.flow.Flow

interface FoodRepository {
    suspend fun save(entry: FoodEntry)
    fun getEntriesForDay(startOfDay: Long, endOfDay: Long): Flow<List<FoodEntry>>
    fun getAllEntries(): Flow<List<FoodEntry>>
    suspend fun deleteById(id: Int)
}
