package com.lifehub.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifehub.LifeHubApplication
import com.lifehub.data.entity.LedgerEntity
import com.lifehub.util.startOfMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LedgerSummary(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val rebate: Double = 0.0,
    val net: Double = 0.0,        // expense - rebate（净支出）
    val budget: Double = 6000.0,
    val budgetUsed: Boolean = false,
    val byCategory: List<Pair<String, Double>> = emptyList()
)

class LedgerViewModel(app: LifeHubApplication) : AndroidViewModel(app) {
    private val container = app.container

    val all: StateFlow<List<LedgerEntity>> = container.ledger.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

        LedgerSummary(income, expense, rebate, net, budget, true, byCat)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LedgerSummary())

    fun insert(typeCode: String, category: String, amount: Double, note: String) {
        viewModelScope.launch {
            container.ledger.insert(
                LedgerEntity(type = typeCode, category = category, amount = amount, note = note, date = System.currentTimeMillis())
            )
        }
    }

    fun delete(item: LedgerEntity) {
        viewModelScope.launch { container.ledger.delete(item) }
    }
}
