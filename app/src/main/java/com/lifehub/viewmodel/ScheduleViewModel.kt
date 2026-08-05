package com.lifehub.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifehub.LifeHubApplication
import com.lifehub.data.entity.ScheduleEntity
import com.lifehub.util.diffDaysKey
import com.lifehub.util.todayKey
import com.lifehub.util.weekMondayKey
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class PlanScope { TODAY, WEEK, TODO, DONE }

data class ScheduleUiState(
    val all: List<ScheduleEntity> = emptyList(),
    val overdueCount: Int = 0,
    val todayCount: Int = 0,
    val todayDone: Int = 0,
    val weekCount: Int = 0,
    val doneCount: Int = 0
)

data class ScheduleRow(
    val item: ScheduleEntity,
    val overdue: Boolean,
    val overdueDays: Int
)

class ScheduleViewModel(app: LifeHubApplication) : AndroidViewModel(app) {
    private val container = app.container

    private val _scope = MutableStateFlow(PlanScope.TODAY)
    val scope: StateFlow<PlanScope> = _scope

    val uiState: StateFlow<ScheduleUiState> = container.schedule.getAll().map { all ->
        val t = todayKey()
        val mon = weekMondayKey()
        ScheduleUiState(
            all = all,
            overdueCount = all.count { !it.done && it.due > 0 && dateKeyFromDue(it.due) < t },
            todayCount = all.count { dateKeyFromDue(it.due) == t },
            todayDone = all.count { dateKeyFromDue(it.due) == t && it.done },
            weekCount = all.count { !it.done && dateKeyFromDue(it.due) in mon..t },
            doneCount = all.count { it.done }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleUiState())

    fun setScope(s: PlanScope) { _scope.value = s }

    fun filtered(state: ScheduleUiState): List<ScheduleRow> {
        val t = todayKey()
        val mon = weekMondayKey()
        val sorted = state.all.sortedWith(compareBy({ it.done }, { it.due }))
        return sorted.filter { x ->
            val dk = dateKeyFromDue(x.due)
            when (_scope.value) {
                PlanScope.TODAY -> !x.done && dk <= t
                PlanScope.WEEK -> !x.done && dk in mon..weekSundayKey()
                PlanScope.TODO -> !x.done
                PlanScope.DONE -> x.done
            }
        }.map {
            val dk = dateKeyFromDue(it.due)
            val over = !it.done && dk < t
            ScheduleRow(it, over, if (over) diffDaysKey(dk, t) else 0)
        }
    }

    fun add(title: String, due: Long, priority: String, note: String) {
        viewModelScope.launch {
            container.schedule.insert(
                ScheduleEntity(title = title, note = note, priority = priority, due = due, done = false)
            )
        }
    }

    fun toggle(item: ScheduleEntity) {
        viewModelScope.launch { container.schedule.update(item.copy(done = !item.done)) }
    }

    fun delete(item: ScheduleEntity) {
        viewModelScope.launch { container.schedule.delete(item) }
    }

    private fun dateKeyFromDue(due: Long): String =
        if (due > 0) com.lifehub.util.dateKey(due) else ""

    private fun weekSundayKey(): String {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0); cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0); cal.set(java.util.Calendar.MILLISECOND, 0)
        val w = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        cal.add(java.util.Calendar.DAY_OF_YEAR, -w + 6)
        return com.lifehub.util.dateKey(cal.timeInMillis)
    }
}
