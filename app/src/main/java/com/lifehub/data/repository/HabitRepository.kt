package com.lifehub.data.repository

import com.lifehub.data.dao.HabitDao
import com.lifehub.data.dao.HabitLogDao
import com.lifehub.data.entity.HabitEntity
import com.lifehub.data.entity.HabitLogEntity
import kotlinx.coroutines.flow.Flow

class HabitRepository(
    private val habitDao: HabitDao,
    private val logDao: HabitLogDao
) {
    fun getActive(): Flow<List<HabitEntity>> = habitDao.getActive()
    fun getAll(): Flow<List<HabitEntity>> = habitDao.getAll()
    fun getLogsByHabit(habitId: Long): Flow<List<HabitLogEntity>> = logDao.getByHabit(habitId)
    fun getLogRange(startKey: String, endKey: String): Flow<List<HabitLogEntity>> =
        logDao.getRange(startKey, endKey)

    suspend fun findLog(habitId: Long, dateKey: String) = logDao.find(habitId, dateKey)
    suspend fun toggleLog(habitId: Long, dateKey: String) {
        val existing = logDao.find(habitId, dateKey)
        if (existing == null) {
            logDao.insert(HabitLogEntity(habitId = habitId, dateKey = dateKey, done = true))
        } else {
            logDao.deleteByKey(habitId, dateKey)
        }
    }

    suspend fun insertHabit(habit: HabitEntity) = habitDao.insert(habit)
    suspend fun updateHabit(habit: HabitEntity) = habitDao.update(habit)
    suspend fun deleteHabit(habit: HabitEntity) = habitDao.delete(habit)
    suspend fun getAllHabitsOnce() = habitDao.getAllOnce()
    suspend fun getAllLogsOnce() = logDao.getAllOnce()
    suspend fun clearAll() { logDao.clearAll(); habitDao.clearAll() }
    suspend fun insertAllHabits(items: List<HabitEntity>) = habitDao.insertAll(items)
    suspend fun insertAllLogs(items: List<HabitLogEntity>) = logDao.insertAll(items)
}
