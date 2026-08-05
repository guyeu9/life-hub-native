package com.lifehub.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifehub.LifeHubApplication
import com.lifehub.data.entity.LedgerEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class LedgerSummary(
    val month: String = "",
    val income: Double = 0.0,
    val incomeCount: Int = 0,
    val expense: Double = 0.0,
    val expenseCount: Int = 0,
    val rebate: Double = 0.0,
    val rebateCount: Int = 0,
    val net: Double = 0.0,        // 实际结余 = 收入 + 返利 − 支出
    val budget: Double = 6000.0,
    val netRebate: Boolean = false,
    val byCategory: List<Pair<String, Double>> = emptyList()
)

/**
 * 记账筛选条件
 * - month: "" = 全部，"yyyy-MM" = 指定月
 * - type:  "" = 全部，"expense" / "income" / "rebate"
 * - category: "" = 全部分类
 * - search: 模糊匹配备注/分类（忽略大小写）
 */
data class LedgerFilter(
    val month: String = "",
    val type: String = "",
    val category: String = "",
    val search: String = ""
)

/** 近 6 个月收支返利对比 */
data class MonthlyTrend(
    val labels: List<String> = emptyList(),
    val expense: List<Double> = emptyList(),
    val income: List<Double> = emptyList(),
    val rebate: List<Double> = emptyList(),
    val net: List<Double> = emptyList()
)

/** 返利概览 */
data class RebateOverview(
    val month: String = "",
    val total: Double = 0.0,
    val rebateCount: Int = 0,
    val base: Double = 0.0,
    val rate: Double = 0.0,
    val foodExpense: Double = 0.0,
    val discount: Double? = null,
    val byCategory: List<Pair<String, Double>> = emptyList(),
    val trend: List<Pair<String, Double>> = emptyList(),
    val bestNote: String = "",
    val bestAmount: Double = 0.0
)

class LedgerViewModel(app: LifeHubApplication) : AndroidViewModel(app) {
    private val container = app.container

    val all: StateFlow<List<LedgerEntity>> = container.ledger.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _filter = MutableStateFlow(LedgerFilter())
    val filter: StateFlow<LedgerFilter> = _filter.asStateFlow()

    /** 按月份/类型/分类/搜索筛选后的明细列表 */
    @OptIn(ExperimentalCoroutinesApi::class)
    val filtered: StateFlow<List<LedgerEntity>> = combine(all, _filter) { list, f ->
        list.filter { item ->
            (f.month.isEmpty() || monthKeyOf(item.date) == f.month) &&
                (f.type.isEmpty() || item.type == f.type) &&
                (f.category.isEmpty() || item.category == f.category) &&
                (f.search.isEmpty() ||
                    item.note.contains(f.search, ignoreCase = true) ||
                    item.category.contains(f.search, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 与 Web 版对齐：汇总跟随当前筛选月份，未选月份时取本月 */
    @OptIn(ExperimentalCoroutinesApi::class)
    val summary: StateFlow<LedgerSummary> = combine(
        all,
        _filter,
        container.settings.monthlyBudget,
        container.settings.netRebate
    ) { arr ->
        val list = arr[0] as List<LedgerEntity>
        val f = arr[1] as LedgerFilter
        val budget = arr[2] as Double
        val netRebate = arr[3] as Boolean

        val month = f.month.ifEmpty { monthKeyOf(System.currentTimeMillis()) }
        val monthList = list.filter { monthKeyOf(it.date) == month }

        val income = monthList.filter { it.type == "income" }.sumOf { it.amount }
        val expense = monthList.filter { it.type == "expense" }.sumOf { it.amount }
        val rebate = monthList.filter { it.type == "rebate" }.sumOf { it.amount }
        val net = income + rebate - expense

        val byCat = monthList.filter { it.type == "expense" }
            .groupBy { it.category }
            .map { (cat, items) -> cat to items.sumOf { it.amount } }
            .sortedByDescending { it.second }

        LedgerSummary(
            month = month,
            income = income,
            incomeCount = monthList.count { it.type == "income" },
            expense = expense,
            expenseCount = monthList.count { it.type == "expense" },
            rebate = rebate,
            rebateCount = monthList.count { it.type == "rebate" },
            net = net,
            budget = budget,
            netRebate = netRebate,
            byCategory = byCat
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LedgerSummary())

    /** 近 6 个月对比数据 */
    @OptIn(ExperimentalCoroutinesApi::class)
    val monthlyTrend: StateFlow<MonthlyTrend> = all.map { list ->
        val labels = mutableListOf<String>()
        val exp = mutableListOf<Double>()
        val inc = mutableListOf<Double>()
        val reb = mutableListOf<Double>()
        val net = mutableListOf<Double>()
        val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
        for (i in 5 downTo 0) {
            val c = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -i) }
            val key = monthKeyOf(c.timeInMillis)
            val label = "${c.get(Calendar.MONTH) + 1}月"
            labels.add(label)
            val monthList = list.filter { monthKeyOf(it.date) == key }
            val e = monthList.filter { it.type == "expense" }.sumOf { it.amount }
            val income = monthList.filter { it.type == "income" }.sumOf { it.amount }
            val rebate = monthList.filter { it.type == "rebate" }.sumOf { it.amount }
            exp.add(e)
            inc.add(income)
            reb.add(rebate)
            net.add(income + rebate - e)
        }
        MonthlyTrend(labels, exp, inc, reb, net)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlyTrend())

    /** 返利概览（跟随筛选月份） */
    @OptIn(ExperimentalCoroutinesApi::class)
    val rebateOverview: StateFlow<RebateOverview> = combine(all, _filter) { arr ->
        val list = arr[0] as List<LedgerEntity>
        val f = arr[1] as LedgerFilter
        val month = f.month.ifEmpty { monthKeyOf(System.currentTimeMillis()) }

        val rows = list.filter { it.type == "rebate" && monthKeyOf(it.date) == month }
        val total = rows.sumOf { it.amount }
        val base = rows.sumOf { it.rebateOf }
        val rate = if (base > 0) total / base * 100 else 0.0

        val food = list.filter {
            it.type == "expense" && monthKeyOf(it.date) == month && it.category == "餐饮"
        }.sumOf { it.amount }
        val discount = if (food > 0) ((food - total) / food * 10).coerceAtLeast(0.0) else null

        val byCat = rows.groupBy { it.category }
            .map { (cat, items) -> cat to items.sumOf { it.amount } }
            .sortedByDescending { it.second }

        val trendLabels = mutableListOf<String>()
        val trendVals = mutableListOf<Double>()
        val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
        for (i in 5 downTo 0) {
            val c = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -i) }
            val key = monthKeyOf(c.timeInMillis)
            trendLabels.add("${c.get(Calendar.MONTH) + 1}月")
            trendVals.add(list.filter { it.type == "rebate" && monthKeyOf(it.date) == key }.sumOf { it.amount })
        }

        val best = rows.maxByOrNull { it.amount }
        RebateOverview(
            month = month,
            total = total,
            rebateCount = rows.size,
            base = base,
            rate = rate,
            foodExpense = food,
            discount = discount,
            byCategory = byCat,
            trend = trendLabels.zip(trendVals),
            bestNote = best?.note?.takeIf { it.isNotBlank() } ?: best?.category ?: "",
            bestAmount = best?.amount ?: 0.0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RebateOverview())

    fun setFilter(f: LedgerFilter) {
        _filter.value = f
    }

    fun insert(typeCode: String, cat: String, amt: Double, note: String, rebateOf: Double, date: Long) {
        viewModelScope.launch {
            container.ledger.insert(
                LedgerEntity(
                    type = typeCode,
                    category = cat,
                    amount = amt,
                    note = note,
                    rebateOf = rebateOf,
                    date = date
                )
            )
        }
    }

    fun delete(item: LedgerEntity) {
        viewModelScope.launch { container.ledger.delete(item) }
    }

    /** 把时间戳转为 "yyyy-MM" 月份键 */
    private fun monthKeyOf(ts: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ts
        return "%04d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }
}
