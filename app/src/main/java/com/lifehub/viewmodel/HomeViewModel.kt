package com.lifehub.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifehub.LifeHubApplication
import com.lifehub.data.entity.FitnessEntity
import com.lifehub.data.entity.HabitEntity
import com.lifehub.data.entity.HabitLogEntity
import com.lifehub.data.entity.LedgerEntity
import com.lifehub.data.entity.ScheduleEntity
import com.lifehub.data.entity.WishItemEntity
import com.lifehub.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlin.math.roundToInt

/**
 * 首页状态 —— 对齐原网页 lifeIndex + viewToday 逻辑
 */
data class HomeUiState(
    // 待办 / 习惯
    val todoTotal: Int = 0,
    val todoDone: Int = 0,
    val overdueCount: Int = 0,
    val habitTotal: Int = 0,
    val habitDoneToday: Int = 0,

    // 今日记账
    val ledgerIncome: Double = 0.0,
    val ledgerExpense: Double = 0.0,
    val ledgerRebate: Double = 0.0,
    val ledgerNet: Double = 0.0,
    val ledgerCount: Int = 0,
    val loggedToday: Boolean = false,
    val ledgerConfigured: Boolean = false,

    // 预算
    val budget: Double = 0.0,
    val netRebate: Boolean = false,
    val budgetUsed: Double = 0.0,

    // 身体
    val todayWeight: Double? = null,
    val todayBodyFat: Double? = null,
    val todayIntake: Double? = null,
    val todayBurn: Double? = null,
    val weighedToday: Boolean = false,
    val lastWeight: Double? = null,
    val lastWeightDate: String = "",
    val tdee: Double = 1600.0,

    // 其他速览
    val maxStreak: Int = 0,
    val p0WishCount: Int = 0,

    // 今日待处理清单
    val todayItems: List<HomeItem> = emptyList()
)

/**
 * 首页「今天要处理」列表项
 * action 表示点击右侧按钮应执行的操作
 */
data class HomeItem(
    val id: Long,
    val type: String,   // "schedule" | "habit" | "ledger_tip" | "fitness_tip" | "wish"
    val title: String,
    val sub: String,
    val overdue: Boolean = false,
    val action: String = "",      // 按钮文字
    val habitType: String = "check",
    val habitTarget: Int = 1,
    val habitUnit: String = "",
    val habitCurrent: Double = 0.0
)

class HomeViewModel(app: LifeHubApplication) : AndroidViewModel(app) {
    private val container = app.container

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(
        listOf(
            container.schedule.getAll(),           // 全部日程（用于区分今日/逾期）
            container.habit.getActive(),             // 活跃习惯
            container.habit.getLogRange(todayKey(), todayKey()), // 今日习惯日志
            container.habit.getAllLogs(),            // 全部习惯日志（用于计算连续打卡）
            container.ledger.getAll(),               // 全部账本（再分今日/本月）
            container.fitness.getAll(),              // 全部身体记录
            container.wish.getAll(),                 // 全部待买
            container.settings.monthlyBudget,        // 月预算
            container.settings.netRebate,            // 预算是否抵扣返利
            container.settings.fitnessProfile        // 健身档案（TDEE）
        )
    ) { arr ->
        val allSchedules = arr[0] as List<ScheduleEntity>
        val habits = arr[1] as List<HabitEntity>
        val todayLogs = arr[2] as List<HabitLogEntity>
        val allLogs = arr[3] as List<HabitLogEntity>
        val allLedger = arr[4] as List<LedgerEntity>
        val fitness = arr[5] as List<FitnessEntity>
        val wishes = arr[6] as List<WishItemEntity>
        val budget = arr[7] as Double
        val netRebate = arr[8] as Boolean
        val profile = arr[9] as com.lifehub.data.SettingsRepository.FitnessProfile

        val todayStart = startOfToday()
        val todayEnd = endOfToday()
        val todayKey = todayKey()
        val monthStart = startOfMonth()

        // 逾期：截止日 < 今天 0 点 且 未完成
        val overdue = allSchedules.filter { it.due in 1 until todayStart && !it.done }.sortedBy { it.due }
        // 今日日程：截止日在今天范围内
        val todaySchedules = allSchedules.filter { it.due in todayStart..todayEnd }
        val doneCount = todaySchedules.count { it.done }
        val totalTodos = overdue.size + todaySchedules.size

        // 今日记账
        val todayLedger = allLedger.filter { it.date in todayStart..todayEnd }
        val inc = todayLedger.filter { it.type == "income" }.sumOf { it.amount }
        val exp = todayLedger.filter { it.type == "expense" }.sumOf { it.amount }
        val reb = todayLedger.filter { it.type == "rebate" }.sumOf { it.amount }
        val ledgerConfigured = allLedger.isNotEmpty()

        // 本月记账（用于预算口径，对齐网页 monthSum(curYM())）
        val monthLedger = allLedger.filter { it.date >= monthStart }
        val monthExp = monthLedger.filter { it.type == "expense" }.sumOf { it.amount }
        val monthReb = monthLedger.filter { it.type == "rebate" }.sumOf { it.amount }
        val used = if (netRebate) monthExp - monthReb else monthExp

        // 习惯完成：按 habitDone 逻辑（check>=1，count/value>=target）
        fun habitDone(h: HabitEntity, dayLogs: List<HabitLogEntity>): Boolean {
            val log = dayLogs.find { it.habitId == h.id }
            val v = log?.value ?: 0.0
            return if (h.type == "check") v >= 1 else v >= h.target
        }
        val habitDoneCount = habits.count { habitDone(it, todayLogs) }

        // 身体
        val todayFit = fitness.find { it.dateKey == todayKey }
        val sortedFit = fitness.sortedBy { it.dateKey }
        val lastFit = sortedFit.lastOrNull()

        // 最大连续打卡
        val maxStreak = habits.maxOfOrNull { streak(it, allLogs) } ?: 0

        // P0 待买
        val p0Wishes = wishes.filter { !it.bought && it.priority == "P0" }

        // 今日待处理条目（严格对齐网页顺序）
        val items = buildList {
            overdue.forEach {
                val days = diffDaysKey(dateKey(it.due), todayKey)
                add(HomeItem(
                    id = it.id, type = "schedule",
                    title = it.title,
                    sub = "原定 ${cnDateKey(dateKey(it.due))} · 已逾期 ${if (days > 0) days else 1} 天 · ${it.priority}",
                    overdue = true, action = "完成"
                ))
            }
            todaySchedules.filter { !it.done }.forEach {
                add(HomeItem(
                    id = it.id, type = "schedule",
                    title = it.title,
                    sub = "今天${if (it.time.isNotBlank()) " ${it.time}" else ""} · ${it.tag} · ${it.priority}",
                    overdue = false, action = "完成"
                ))
            }
            habits.filter { !habitDone(it, todayLogs) }.forEach { h ->
                val log = todayLogs.find { it.habitId == h.id }
                val cur = log?.value ?: 0.0
                val sub = when (h.type) {
                    "check" -> "今天还没打卡"
                    else -> "今天 ${cur.toInt()} / ${h.target} ${h.unit}"
                }
                add(HomeItem(
                    id = h.id, type = "habit",
                    title = "习惯 · ${h.name}",
                    sub = sub,
                    overdue = false,
                    action = if (h.type == "check") "打卡" else "+1",
                    habitType = h.type,
                    habitTarget = h.target,
                    habitUnit = h.unit,
                    habitCurrent = cur
                ))
            }
            if (todayLedger.isEmpty()) {
                add(HomeItem(
                    id = -1, type = "ledger_tip",
                    title = "今天还没记一笔账",
                    sub = "哪怕是一杯咖啡，记下来才有数",
                    action = "去记账"
                ))
            }
            if (todayFit == null) {
                add(HomeItem(
                    id = -1, type = "fitness_tip",
                    title = "今天还没称体重",
                    sub = "早上空腹称，数据才可比",
                    action = "去记录"
                ))
            }
            p0Wishes.forEach {
                add(HomeItem(
                    id = it.id, type = "wish",
                    title = "待买 · ${it.name}",
                    sub = "高优先级 · ¥${money0(it.estPrice)}",
                    action = "已买"
                ))
            }
        }

        HomeUiState(
            todoTotal = totalTodos,
            todoDone = doneCount,
            overdueCount = overdue.size,
            habitTotal = habits.size,
            habitDoneToday = habitDoneCount,
            ledgerIncome = inc,
            ledgerExpense = exp,
            ledgerRebate = reb,
            ledgerNet = exp - reb,
            ledgerCount = todayLedger.size,
            loggedToday = todayLedger.isNotEmpty(),
            ledgerConfigured = ledgerConfigured,
            budget = budget,
            netRebate = netRebate,
            budgetUsed = used,
            todayWeight = todayFit?.takeIf { it.weight > 0 }?.weight,
            todayBodyFat = todayFit?.takeIf { it.fat > 0 }?.fat,
            todayIntake = todayFit?.takeIf { it.intake > 0 }?.intake,
            todayBurn = todayFit?.takeIf { it.burn > 0 }?.burn,
            weighedToday = todayFit != null && todayFit.weight > 0,
            lastWeight = lastFit?.takeIf { it.weight > 0 }?.weight,
            lastWeightDate = lastFit?.dateKey?.let { cnDateKey(it) } ?: "",
            tdee = profile.tdee,
            maxStreak = maxStreak,
            p0WishCount = p0Wishes.size,
            todayItems = items
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    /** 待办完成率（只统计逾期+今日任务） */
    fun todoPct(state: HomeUiState): Int {
        if (state.todoTotal == 0) return 0
        return ((state.todoDone.toFloat() / state.todoTotal) * 100).toInt().coerceIn(0, 100)
    }

    /** 习惯完成率 */
    fun habitPct(state: HomeUiState): Int {
        if (state.habitTotal == 0) return 0
        return ((state.habitDoneToday.toFloat() / state.habitTotal) * 100).toInt().coerceIn(0, 100)
    }

    /** 综合生活指数 (0-100)，对齐网页 lifeIndex */
    fun overallPct(state: HomeUiState): Int {
        val todoCfg = state.todoTotal > 0
        val habitCfg = state.habitTotal > 0
        val moneyCfg = state.ledgerCount > 0
        val bodyCfg = state.lastWeight != null

        val bud = state.budget
        val spent = state.budgetUsed
        val budOK = if (bud > 0) (1 - (spent / bud - 0.85) / 0.4).coerceIn(0.0, 1.0) else 0.0
        val bodyS = if (state.weighedToday) 10 else 0
        val bodyExtra = if (state.weighedToday && deficitToday(state) > 0) 10 else if (state.weighedToday) 4 else 0

        val total = (
            (if (todoCfg) todoPct(state) / 100.0 * 30 else 0.0) +
            (if (habitCfg) habitPct(state) / 100.0 * 30 else 0.0) +
            (if (moneyCfg && state.loggedToday) 10 else 0) +
            (10 * budOK).roundToInt() +
            bodyS + bodyExtra
        ).toInt().coerceIn(0, 100)
        return total
    }

    private fun deficitToday(state: HomeUiState): Double {
        // 热量缺口 = TDEE + 运动消耗 - 摄入（对齐网页 deficit）
        return state.tdee + (state.todayBurn ?: 0.0) - (state.todayIntake ?: 0.0)
    }

    /** 习惯连续打卡天数（对齐网页 habitStreak） */
    private fun streak(habit: HabitEntity, allLogs: List<HabitLogEntity>): Int {
        val logsByDay = allLogs.filter { it.habitId == habit.id }.associateBy { it.dateKey }
        fun habitDoneOn(day: String): Boolean {
            val log = logsByDay[day]
            val v = log?.value ?: 0.0
            return if (habit.type == "check") v >= 1 else v >= habit.target
        }
        var n = 0
        for (i in 0 until 400) {
            val day = dayOffsetKey(-i)
            if (habitDoneOn(day)) {
                n++
            } else if (i > 0) {
                break
            }
        }
        return n
    }

    /** 完成一条日程 */
    suspend fun completeSchedule(id: Long) {
        val item = container.schedule.getAllOnce().find { it.id == id } ?: return
        container.schedule.update(item.copy(done = true))
    }

    /** 习惯快速打卡 / +1 */
    suspend fun quickHabit(item: HomeItem) {
        if (item.type != "habit") return
        if (item.habitType == "check") {
            container.habit.toggleLog(item.id, todayKey())
        } else {
            container.habit.incrementLog(item.id, todayKey(), 1)
        }
    }

    /** 待买标记已买 */
    suspend fun buyDone(id: Long) {
        val item = container.wish.getAllOnce().find { it.id == id } ?: return
        container.wish.update(item.copy(bought = true))
    }
}
