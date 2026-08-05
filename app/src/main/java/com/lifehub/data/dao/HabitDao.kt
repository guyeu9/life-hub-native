package com.lifehub.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifehub.data.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY createdAt ASC")
    fun getActive(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    fun getAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits")
    suspend fun getAllOnce(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)

    @Delete
    suspend fun delete(habit: HabitEntity)

    @Query("DELETE FROM habits")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<HabitEntity>)
}
