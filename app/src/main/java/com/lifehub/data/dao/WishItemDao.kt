package com.lifehub.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifehub.data.entity.WishItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WishItemDao {

    @Query("SELECT * FROM wish_items ORDER BY bought ASC, createdAt DESC")
    fun getAll(): Flow<List<WishItemEntity>>

    @Query("SELECT * FROM wish_items WHERE bought = 0 ORDER BY createdAt DESC")
    fun getPending(): Flow<List<WishItemEntity>>

    @Query("SELECT * FROM wish_items")
    suspend fun getAllOnce(): List<WishItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WishItemEntity): Long

    @Update
    suspend fun update(item: WishItemEntity)

    @Delete
    suspend fun delete(item: WishItemEntity)

    @Query("DELETE FROM wish_items")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WishItemEntity>)
}
