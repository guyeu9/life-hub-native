package com.lifehub.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifehub.data.entity.FitnessPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FitnessPlanDao {

    @Query("SELECT * FROM fitness_plan ORDER BY dayIndex ASC")
    fun getAll(): Flow<List<FitnessPlanEntity>>

    @Query("SELECT * FROM fitness_plan ORDER BY dayIndex ASC")
    suspend fun getAllOnce(): List<FitnessPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: FitnessPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<FitnessPlanEntity>)

    @Update
    suspend fun update(item: FitnessPlanEntity)

    @Query("UPDATE fitness_plan SET done = :done WHERE dayIndex = :dayIndex")
    suspend fun setDone(dayIndex: Int, done: Boolean)

    @Query("UPDATE fitness_plan SET done = 0")
    suspend fun resetAllDone()

    @Query("DELETE FROM fitness_plan")
    suspend fun clearAll()
}
