package com.lifehub.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifehub.data.entity.MediaItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {

    @Query("SELECT * FROM media_items ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE type = :type ORDER BY updatedAt DESC")
    fun getByType(type: String): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items")
    suspend fun getAllOnce(): List<MediaItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MediaItemEntity): Long

    @Update
    suspend fun update(item: MediaItemEntity)

    @Delete
    suspend fun delete(item: MediaItemEntity)

    @Query("DELETE FROM media_items")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaItemEntity>)
}
