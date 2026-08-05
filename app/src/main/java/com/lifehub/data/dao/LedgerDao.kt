package com.lifehub.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifehub.data.entity.LedgerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {

    @Query("SELECT * FROM ledger ORDER BY date DESC")
    fun getAll(): Flow<List<LedgerEntity>>

    @Query("SELECT * FROM ledger WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun getRange(start: Long, end: Long): Flow<List<LedgerEntity>>

    @Query("SELECT * FROM ledger WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    suspend fun getRangeOnce(start: Long, end: Long): List<LedgerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: LedgerEntity): Long

    @Update
    suspend fun update(item: LedgerEntity)

    @Delete
    suspend fun delete(item: LedgerEntity)

    @Query("SELECT * FROM ledger")
    suspend fun getAllOnce(): List<LedgerEntity>

    @Query("DELETE FROM ledger")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<LedgerEntity>)
}
