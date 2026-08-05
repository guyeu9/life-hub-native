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
    val doneToday: Boolean,
    val streak: Int,
    val monthDays: List<Boolean>  // 最近 30 天打卡
)

class HabitViewModel(app: LifeHubApplication) : AndroidViewModel(app) {
    private val container = app.container

    val habits: StateFlow<List<HabitView>> = combine(
        container.habit.getActive(),
        container.habit.getLogRange(recentDateKeys(30).first(), todayKey())
    ) { habits, logs ->
        val logMap = logs.groupBy { it.habitId }
        habits.map { h ->
            val hLogs = logMap[h.id] ?: emptyList()
            val doneSet = hLogs.map { it.dateKey }.toSet()
            val monthDays = recentDateKeys(30).map { doneSet.contains(it) }
            HabitView(
                habit = h,
                doneToday = doneSet.contains(todayKey()),
                streak = computeStreak(doneSet),
                monthDays = monthDays
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun computeStreak(doneSet: Set<String>): Int {
        var streak = 0
        val cal = Calendar.getInstance()
        while (true) {
            val key = dateKey(cal.timeInMillis)
            if (doneSet.contains(key)) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else break
        }
        return streak
    }

    fun toggleToday(habitId: Long) {
        viewModelScope.launch {
            container.habit.toggleLog(habitId, todayKey())
        }
    }

    fun addHabit(name: String, color: String) {
        viewModelScope.launch {
            container.habit.insertHabit(HabitEntity(name = name, color = color))
        }
    }

    fun deleteHabit(h: HabitEntity) {
        viewModelScope.launch { container.habit.deleteHabit(h) }
    }
}
