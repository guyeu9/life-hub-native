package com.lifehub.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifehub.data.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM schedules ORDER BY done ASC, priority ASC, due ASC")
    fun getAll(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE done = 0 ORDER BY priority ASC, due ASC")
    fun getPending(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE done = 0 AND due > 0 AND due <= :now ORDER BY priority ASC, due ASC")
    fun getOverdue(now: Long): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules")
    suspend fun getAllOnce(): List<ScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ScheduleEntity): Long

    @Update
    suspend fun update(item: ScheduleEntity)

    @Delete
    suspend fun delete(item: ScheduleEntity)

    @Query("DELETE FROM schedules")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ScheduleEntity>)
}
