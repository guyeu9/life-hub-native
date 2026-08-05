package com.lifehub.data.repository

import com.lifehub.data.dao.WishItemDao
import com.lifehub.data.entity.WishItemEntity
import kotlinx.coroutines.flow.Flow

class WishRepository(private val dao: WishItemDao) {
    fun getAll(): Flow<List<WishItemEntity>> = dao.getAll()
    fun getPending(): Flow<List<WishItemEntity>> = dao.getPending()
    suspend fun insert(item: WishItemEntity) = dao.insert(item)
    suspend fun update(item: WishItemEntity) = dao.update(item)
    suspend fun delete(item: WishItemEntity) = dao.delete(item)
    suspend fun getAllOnce() = dao.getAllOnce()
    suspend fun clearAll() = dao.clearAll()
    suspend fun insertAll(items: List<WishItemEntity>) = dao.insertAll(items)
}
