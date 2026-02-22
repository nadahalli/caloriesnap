package com.tejaswin.caloriesnap.data

import kotlinx.coroutines.flow.Flow

class RoomFoodRepository(private val dao: FoodDao) : FoodRepository {
    override suspend fun save(entry: FoodEntry) = dao.insert(entry)
    override fun getEntriesForDay(startOfDay: Long, endOfDay: Long): Flow<List<FoodEntry>> =
        dao.getEntriesForDay(startOfDay, endOfDay)
    override fun getAllEntries(): Flow<List<FoodEntry>> = dao.getAllEntries()
    override suspend fun deleteById(id: Int) = dao.deleteById(id)
}
