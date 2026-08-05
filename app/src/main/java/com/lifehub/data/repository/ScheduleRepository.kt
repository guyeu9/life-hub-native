package com.lifehub.data.repository

import com.lifehub.data.dao.ScheduleDao
import com.lifehub.data.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

class ScheduleRepository(private val dao: ScheduleDao) {
    fun getAll(): Flow<List<ScheduleEntity>> = dao.getAll()
    fun getPending(): Flow<List<ScheduleEntity>> = dao.getPending()
    fun getOverdue(now: Long): Flow<List<ScheduleEntity>> = dao.getOverdue(now)
    suspend fun insert(item: ScheduleEntity) = dao.insert(item)
    suspend fun update(item: ScheduleEntity) = dao.update(item)
    suspend fun delete(item: ScheduleEntity) = dao.delete(item)
    suspend fun getAllOnce() = dao.getAllOnce()
    suspend fun clearAll() = dao.clearAll()
    suspend fun insertAll(items: List<ScheduleEntity>) = dao.insertAll(items)
}
