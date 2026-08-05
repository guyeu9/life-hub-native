package com.lifehub.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifehub.data.entity.FitnessEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FitnessDao {

    @Query("SELECT * FROM fitness ORDER BY dateKey DESC")
    fun getAll(): Flow<List<FitnessEntity>>

    @Query("SELECT * FROM fitness WHERE dateKey = :key LIMIT 1")
    suspend fun findByDateKey(key: String): FitnessEntity?

    @Query("SELECT * FROM fitness ORDER BY dateKey ASC")
    suspend fun getAllOnce(): List<FitnessEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FitnessEntity): Long

    @Update
    suspend fun update(item: FitnessEntity)

    @Delete
    suspend fun delete(item: FitnessEntity)

    @Query("DELETE FROM fitness WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM fitness")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FitnessEntity>)
}
