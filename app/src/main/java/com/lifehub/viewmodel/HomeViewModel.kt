package com.lifehub.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifehub.LifeHubApplication
import com.lifehub.data.entity.LedgerEntity
import com.lifehub.data.entity.ScheduleEntity
import com.lifehub.util.dateKey
import com.lifehub.util.endOfToday
import com.lifehub.util.startOfToday
import com.lifehub.util.todayKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.Calendar

/**
 * 首页状态
 */
data class HomeUiState(
    val todoTotal: Int = 0,
    val todoDone: Int = 0,
    val overdueCount: Int = 0,
    val habitTotal: Int = 0,
    val habitDoneToday: Int = 0,
    val ledgerIncome: Double = 0.0,
    val ledgerExpense: Double = 0.0,
    val ledgerRebate: Double = 0.0,
    val ledgerCount: Int = 0,
    val todayWeight: Double? = null,
    val todayBodyFat: Double? = null,
    val todayItems: List<HomeItem> = emptyList()
)

data class HomeItem(
    val id: Long,
    val type: String,   // "schedule" | "habit"
    val title: String,
    val sub: String,
    val overdue: Boolean = false
)

class HomeViewModel(app: LifeHubApplication) : AndroidViewModel(app) {
    private val container = app.container

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(
        listOf(
            container.schedule.getPending(),
            container.schedule.getOverdue(System.currentTimeMillis()),
            container.habit.getActive(),
            container.habit.getLogRange(todayKey(), todayKey()),
            ledgerTodayFlow(),
            container.fitness.getAll(),
            container.schedule.getAll()
        )
    ) { arr ->
        val pending = arr[0] as List<ScheduleEntity>
        val overdue = arr[1] as List<ScheduleEntity>
        val habits = arr[2] as List<com.lifehub.data.entity.HabitEntity>
        val logs = arr[3] as List<com.lifehub.data.entity.HabitLogEntity>
        val ledger = arr[4] as List<LedgerEntity>
        val fitness = arr[5] as List<com.lifehub.data.entity.FitnessEntity>
        val allSchedules = arr[6] as List<ScheduleEntity>

        // 待办完成率：已完成 / 全部
        val doneCount = allSchedules.count { it.done }
        val totalTodos = allSchedules.size

        // 记账当日汇总
        val inc = ledger.filter { it.type == "income" }.sumOf { it.amount }
        val exp = ledger.filter { it.type == "expense" }.sumOf { it.amount }
        val reb = ledger.filter { it.type == "rebate" }.sumOf { it.amount }

        // 习惯今日完成
        val habitDone = habits.count { h ->
            logs.any { it.habitId == h.id && it.dateKey == todayKey() }
        }

        // 今天的身体数据（按 dateKey 匹配今天）
        val todayFit = fitness.firstOrNull { it.dateKey == todayKey() }

        // 今日待处理条目
        val items = buildList {
            overdue.forEach {
                add(HomeItem(it.id, "schedule", it.title, "逾期", overdue = true))
            }
            pending.take(5).forEach {
                add(HomeItem(it.id, "schedule", it.title, it.priority, overdue = false))
            }
            habits.filter { h -> !logs.any { it.habitId == h.id && it.dateKey == todayKey() } }
                .take(3).forEach {
                    add(HomeItem(it.id, "habit", it.name, "今日未打卡", overdue = false))
                }
        }

        HomeUiState(
            todoTotal = totalTodos,
            todoDone = doneCount,
            overdueCount = overdue.size,
            habitTotal = habits.size,
            habitDoneToday = habitDone,
            ledgerIncome = inc,
            ledgerExpense = exp,
            ledgerRebate = reb,
            ledgerCount = ledger.size,
            todayWeight = todayFit?.takeIf { it.weight > 0 }?.weight,
            todayBodyFat = todayFit?.takeIf { it.fat > 0 }?.fat,
            todayItems = items
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    private fun ledgerTodayFlow(): Flow<List<LedgerEntity>> {
        val s = startOfToday()
        val e = endOfToday()
        return container.ledger.getRange(s, e)
    }

    /** 待办完成率 */
    fun todoPct(state: HomeUiState): Int {
        if (state.todoTotal == 0) return 0
        return ((state.todoDone.toFloat() / state.todoTotal) * 100).toInt().coerceIn(0, 100)
    }

    /** 习惯完成率 */
    fun habitPct(state: HomeUiState): Int {
        if (state.habitTotal == 0) return 0
        return ((state.habitDoneToday.toFloat() / state.habitTotal) * 100).toInt().coerceIn(0, 100)
    }
}
