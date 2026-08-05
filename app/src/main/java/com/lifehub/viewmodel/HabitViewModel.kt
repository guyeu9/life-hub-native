package com.lifehub.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifehub.LifeHubApplication
import com.lifehub.data.entity.HabitEntity
import com.lifehub.data.entity.HabitLogEntity
import com.lifehub.util.dateKey
import com.lifehub.util.recentDateKeys
import com.lifehub.util.todayKey
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class HabitView(
    val habit: HabitEntity,
    val todayDone: Boolean,
    val todayValue: Double,
    val streak: Int,
    val rate30: Float,
    val last30States: List<Float>  // 热力图数据 0..1 ratio
)

class HabitViewModel(app: LifeHubApplication) : AndroidViewModel(app) {
    private val container = app.container

    val habits: StateFlow<List<HabitView>> = combine(
        container.habit.getActive(),
        container.habit.getLogRange(recentDateKeys(30).first(), todayKey())
    ) { habits, logs ->
        val logMap = logs.groupBy { it.habitId }
        val keys = recentDateKeys(30)
        habits.map { h ->
            val hLogs = logMap[h.id] ?: emptyList()
            val logByDate = hLogs.associateBy { it.dateKey }
            val doneSet = hLogs.filter { it.done }.map { it.dateKey }.toSet()
            val todayLog = logByDate[todayKey()]
            val last30States = keys.map { k -> logRatio(logByDate[k], h.target) }
            HabitView(
                habit = h,
                todayDone = todayLog?.done == true,
                todayValue = todayLog?.value ?: 0.0,
                streak = computeStreak(doneSet),
                rate30 = computeRate30(doneSet, keys),
                last30States = last30States
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun computeStreak(doneSet: Set<String>): Int {
        var streak = 0
        val cal = Calendar.getInstance()
        // 今天没打卡不算断，从昨天继续数（对齐 HTML 版 habitStreak）
        for (i in 0 until 400) {
            val key = dateKey(cal.timeInMillis)
            if (doneSet.contains(key)) {
                streak++
            } else if (i > 0) {
                break
            } else {
                // 今天未打卡，跳过，继续检查昨天
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    private fun computeRate30(doneSet: Set<String>, keys: List<String>): Float {
        if (keys.isEmpty()) return 0f
        val done = keys.count { doneSet.contains(it) }
        return done.toFloat() / keys.size
    }

    /** 热力图 ratio：done 时至少为 1，否则按 value/target 计算 */
    private fun logRatio(log: HabitLogEntity?, target: Int): Float {
        if (log == null) return 0f
        if (target <= 0) return if (log.done) 1f else 0f
        val r = log.value.toFloat() / target
        return when {
            log.done && r <= 0f -> 1f
            else -> r.coerceIn(0f, 1f)
        }
    }

    fun toggleCheck(habit: HabitEntity) {
        viewModelScope.launch {
            container.habit.toggleLog(habit.id, todayKey())
        }
    }

    fun incrementCount(habit: HabitEntity, delta: Int) {
        viewModelScope.launch {
            container.habit.incrementLog(habit.id, todayKey(), delta)
        }
    }

    fun setValue(habit: HabitEntity, value: Double) {
        viewModelScope.launch {
            container.habit.setValueLog(habit.id, todayKey(), value)
        }
    }

    fun addHabit(name: String, type: String, target: Int, unit: String) {
        viewModelScope.launch {
            val count = container.habit.getAllHabitsOnce().size
            val palette = listOf("#a2543c", "#5d7561", "#4a6478", "#a8842f", "#8d6b52", "#6b6a94")
            val color = palette[count % palette.size]
            container.habit.insertHabit(
                HabitEntity(
                    name = name,
                    type = type,
                    target = if (type == "check") 1 else if (target < 1) 1 else target,
                    unit = unit,
                    color = color
                )
            )
        }
    }

    fun deleteHabit(h: HabitEntity) {
        viewModelScope.launch { container.habit.deleteHabit(h) }
    }
}
