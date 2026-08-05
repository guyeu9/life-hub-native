package com.lifehub.data.repository

import com.lifehub.data.dao.MediaItemDao
import com.lifehub.data.entity.MediaItemEntity
import kotlinx.coroutines.flow.Flow

class MediaRepository(private val dao: MediaItemDao) {
    fun getAll(): Flow<List<MediaItemEntity>> = dao.getAll()
    fun getByType(type: String): Flow<List<MediaItemEntity>> = dao.getByType(type)
    suspend fun insert(item: MediaItemEntity) = dao.insert(item)
    suspend fun update(item: MediaItemEntity) = dao.update(item)
    suspend fun delete(item: MediaItemEntity) = dao.delete(item)
    suspend fun getAllOnce() = dao.getAllOnce()
    suspend fun clearAll() = dao.clearAll()
    suspend fun insertAll(items: List<MediaItemEntity>) = dao.insertAll(items)
}
