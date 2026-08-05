package com.lifehub.data.repository

import com.lifehub.data.dao.FitnessDao
import com.lifehub.data.entity.FitnessEntity
import kotlinx.coroutines.flow.Flow

class FitnessRepository(private val dao: FitnessDao) {
    fun getAll(): Flow<List<FitnessEntity>> = dao.getAll()
    suspend fun findByDateKey(key: String) = dao.findByDateKey(key)
    suspend fun insert(item: FitnessEntity) = dao.insert(item)
    suspend fun update(item: FitnessEntity) = dao.update(item)
    suspend fun delete(item: FitnessEntity) = dao.delete(item)
    suspend fun deleteById(id: Long) = dao.deleteById(id)
    suspend fun getAllOnce() = dao.getAllOnce()
    suspend fun clearAll() = dao.clearAll()
    suspend fun insertAll(items: List<FitnessEntity>) = dao.insertAll(items)

    /** 同一天重复记录会覆盖（按 dateKey 去重） */
    suspend fun upsertByDateKey(item: FitnessEntity) {
        val existing = dao.findByDateKey(item.dateKey)
        if (existing != null) {
            dao.update(item.copy(id = existing.id))
        } else {
            dao.insert(item)
        }
    }
}
