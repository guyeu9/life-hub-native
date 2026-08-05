package com.lifehub.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifehub.data.entity.HabitLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitLogDao {

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY dateKey DESC")
    fun getByHabit(habitId: Long): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE dateKey BETWEEN :startKey AND :endKey ORDER BY dateKey DESC")
    fun getRange(startKey: String, endKey: String): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs ORDER BY dateKey DESC")
    fun getAll(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND dateKey = :dateKey LIMIT 1")
    suspend fun find(habitId: Long, dateKey: String): HabitLogEntity?

    @Query("SELECT * FROM habit_logs")
    suspend fun getAllOnce(): List<HabitLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: HabitLogEntity): Long

    @Delete
    suspend fun delete(log: HabitLogEntity)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND dateKey = :dateKey")
    suspend fun deleteByKey(habitId: Long, dateKey: String)

    @Query("UPDATE habit_logs SET value = :value, done = :done WHERE habitId = :habitId AND dateKey = :dateKey")
    suspend fun updateValue(habitId: Long, dateKey: String, value: Double, done: Boolean)

    @Query("DELETE FROM habit_logs")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<HabitLogEntity>)
}
