package com.lifehub.data.repository

import com.lifehub.data.dao.LedgerDao
import com.lifehub.data.entity.LedgerEntity
import kotlinx.coroutines.flow.Flow

class LedgerRepository(private val dao: LedgerDao) {
    fun getAll(): Flow<List<LedgerEntity>> = dao.getAll()
    fun getRange(start: Long, end: Long): Flow<List<LedgerEntity>> = dao.getRange(start, end)
    suspend fun getRangeOnce(start: Long, end: Long) = dao.getRangeOnce(start, end)
    suspend fun insert(item: LedgerEntity) = dao.insert(item)
    suspend fun update(item: LedgerEntity) = dao.update(item)
    suspend fun delete(item: LedgerEntity) = dao.delete(item)
    suspend fun getAllOnce() = dao.getAllOnce()
    suspend fun clearAll() = dao.clearAll()
    suspend fun insertAll(items: List<LedgerEntity>) = dao.insertAll(items)
}
