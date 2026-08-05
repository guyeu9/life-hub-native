package com.lifehub.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifehub.LifeHubApplication
import com.lifehub.data.entity.LedgerEntity
import com.lifehub.util.startOfMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class LedgerSummary(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val rebate: Double = 0.0,
    val net: Double = 0.0,        // expense - rebate（净支出）
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val summary: StateFlow<LedgerSummary> = combine(
        container.ledger.getRange(startOfMonth(), System.currentTimeMillis()),
        container.settings.monthlyBudget,
        container.settings.netRebate
    ) { arr ->
        val list = arr[0] as List<LedgerEntity>
        val budget = arr[1] as Double
        val netRebate = arr[2] as Boolean

        val income = list.filter { it.type == "income" }.sumOf { it.amount }
        val expense = list.filter { it.type == "expense" }.sumOf { it.amount }
        val rebate = list.filter { it.type == "rebate" }.sumOf { it.amount }
        val net = if (netRebate) expense - rebate else expense

        val byCat = list.filter { it.type == "expense" }
            .groupBy { it.category }
            .map { (cat, items) -> cat to items.sumOf { it.amount } }
            .sortedByDescending { it.second }

        LedgerSummary(income, expense, rebate, net, budget, netRebate, byCat)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LedgerSummary())

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
