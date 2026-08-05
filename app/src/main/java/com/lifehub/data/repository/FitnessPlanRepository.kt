package com.lifehub.data.repository

import com.lifehub.data.dao.FitnessPlanDao
import com.lifehub.data.entity.FitnessPlanEntity
import kotlinx.coroutines.flow.Flow

class FitnessPlanRepository(private val dao: FitnessPlanDao) {
    fun getAll(): Flow<List<FitnessPlanEntity>> = dao.getAll()
    suspend fun getAllOnce() = dao.getAllOnce()
    suspend fun upsert(item: FitnessPlanEntity) = dao.upsert(item)
    suspend fun upsertAll(items: List<FitnessPlanEntity>) = dao.upsertAll(items)
    suspend fun setDone(dayIndex: Int, done: Boolean) = dao.setDone(dayIndex, done)
    suspend fun resetAllDone() = dao.resetAllDone()
    suspend fun clearAll() = dao.clearAll()
}
