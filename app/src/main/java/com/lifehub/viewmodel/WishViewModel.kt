package com.lifehub.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifehub.LifeHubApplication
import com.lifehub.data.entity.LedgerEntity
import com.lifehub.data.entity.WishItemEntity
import com.lifehub.data.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class BuyScope { TODO, DONE, ALL }

data class WishUiState(
    val all: List<WishItemEntity> = emptyList(),
    val budget: Double = 6000.0,
    val todoSum: Double = 0.0,
    val doneSum: Double = 0.0,
    val todoCount: Int = 0,
    val doneCount: Int = 0,
    val p0Count: Int = 0
)

class WishViewModel(app: LifeHubApplication) : AndroidViewModel(app) {
    private val container = app.container

    private val _scope = MutableStateFlow(BuyScope.TODO)
    val scope: StateFlow<BuyScope> = _scope

    val uiState: StateFlow<WishUiState> = combine(
        container.wish.getAll(),
        container.settings.monthlyBudget
    ) { all, budget ->
        WishUiState(
            all = all,
            budget = budget,
            todoSum = all.filter { !it.bought }.sumOf { it.estPrice },
            doneSum = all.filter { it.bought }.sumOf { it.estPrice },
            todoCount = all.count { !it.bought },
            doneCount = all.count { it.bought },
            p0Count = all.count { !it.bought && it.priority == "P0" }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WishUiState())

    fun setScope(s: BuyScope) { _scope.value = s }

    fun filtered(state: WishUiState): List<WishItemEntity> {
        val order = mapOf("P0" to 0, "P1" to 1, "P2" to 2)
        return state.all.sortedWith(compareBy({ it.bought }, { order[it.priority] ?: 2 }, { -it.createdAt })).filter {
            when (_scope.value) {
                BuyScope.TODO -> !it.bought
                BuyScope.DONE -> it.bought
                BuyScope.ALL -> true
            }
        }
    }

    fun add(name: String, price: Double, priority: String, note: String) {
        viewModelScope.launch {
            container.wish.insert(
                WishItemEntity(name = name, estPrice = price, priority = priority, note = note)
            )
        }
    }

    fun toggle(item: WishItemEntity) {
        viewModelScope.launch { container.wish.update(item.copy(bought = !item.bought)) }
    }

    fun delete(item: WishItemEntity) {
        viewModelScope.launch { container.wish.delete(item) }
    }

    /** 已买并记账：标记已买 + 写一笔支出账目（购物分类），防重复记账 */
    fun markBoughtAndLedger(item: WishItemEntity, fields: SettingsRepository.FieldTable) {
        if (item.boughtLedgerId > 0) {
            if (!item.bought) {
                viewModelScope.launch { container.wish.update(item.copy(bought = true)) }
            }
            return
        }
        viewModelScope.launch {
            val shoppingCat = fields.expenseCats.firstOrNull { it.name == "购物" }?.name
                ?: fields.expenseCats.firstOrNull()?.name
                ?: "购物"
            val ledgerId = container.ledger.insert(
                LedgerEntity(
                    type = "expense",
                    category = shoppingCat,
                    amount = item.estPrice,
                    note = item.name,
                    date = System.currentTimeMillis()
                )
            )
            container.wish.update(item.copy(bought = true, boughtLedgerId = ledgerId))
        }
    }
}
