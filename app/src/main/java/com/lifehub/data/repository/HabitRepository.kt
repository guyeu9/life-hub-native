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
    fun getAllLogs(): Flow<List<HabitLogEntity>> = logDao.getAll()

    suspend fun findLog(habitId: Long, dateKey: String) = logDao.find(habitId, dateKey)

    /** check 类型：切换打卡状态 */
    suspend fun toggleLog(habitId: Long, dateKey: String) {
        val existing = logDao.find(habitId, dateKey)
        if (existing == null) {
            val habit = habitDao.getAllOnce().find { it.id == habitId }
            val target = habit?.target ?: 1
            logDao.insert(
                HabitLogEntity(
                    habitId = habitId,
                    dateKey = dateKey,
                    done = true,
                    value = target.toDouble()
                )
            )
        } else {
            logDao.deleteByKey(habitId, dateKey)
        }
    }

    /** count 类型：增减次数，自动计算 done = (value >= target) */
    suspend fun incrementLog(habitId: Long, dateKey: String, delta: Int) {
        val habit = habitDao.getAllOnce().find { it.id == habitId } ?: return
        val target = habit.target
        val existing = logDao.find(habitId, dateKey)
        val newVal = ((existing?.value ?: 0.0) + delta).coerceAtLeast(0.0)
        val done = newVal >= target
        if (existing == null) {
            logDao.insert(
                HabitLogEntity(
                    habitId = habitId,
                    dateKey = dateKey,
                    done = done,
                    value = newVal
                )
            )
        } else {
            logDao.updateValue(habitId, dateKey, newVal, done)
        }
    }

    /** value 类型：设置数值，自动计算 done = (value >= target) */
    suspend fun setValueLog(habitId: Long, dateKey: String, value: Double) {
        val habit = habitDao.getAllOnce().find { it.id == habitId } ?: return
        val target = habit.target
        val done = value >= target
        val existing = logDao.find(habitId, dateKey)
        if (existing == null) {
            logDao.insert(
                HabitLogEntity(
                    habitId = habitId,
                    dateKey = dateKey,
                    done = done,
                    value = value
                )
            )
        } else {
            logDao.updateValue(habitId, dateKey, value, done)
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
