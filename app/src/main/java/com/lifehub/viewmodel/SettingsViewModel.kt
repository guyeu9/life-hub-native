package com.lifehub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifehub.LifeHubApplication
import com.lifehub.data.SettingsRepository
import com.lifehub.ui.settings.FieldTab
import com.lifehub.util.DemoDataUtil
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val ledgerCount: Int = 0,
    val scheduleCount: Int = 0,
    val wishCount: Int = 0,
    val habitCount: Int = 0,
    val fitnessCount: Int = 0,
    val mediaCount: Int = 0,
    val pendingCount: Int = 0,
    val storageSizeKb: Double = 0.0,
    val quickAmounts: SettingsRepository.QuickAmounts = SettingsRepository.QuickAmounts(),
    val fields: SettingsRepository.FieldTable = SettingsRepository.FieldTable(),
    val toastMessage: String? = null
)

class SettingsViewModel(private val app: LifeHubApplication) : ViewModel() {

    private val container = app.container

    private val _toastFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        container.ledger.getAll(),
        container.schedule.getAll(),
        container.wish.getAll(),
        container.habit.getActive(),
        container.fitness.getAll(),
        container.media.getAll(),
        container.settings.backupCount,
        container.settings.quickAmounts,
        container.settings.fields,
        _toastFlow,
        flow { emit(container.settings.approximateStorageSizeKb()) }
    ) { arr ->
        val ledger = arr[0] as List<*>
        val schedule = arr[1] as List<*>
        val wish = arr[2] as List<*>
        val habits = arr[3] as List<*>
        val fitness = arr[4] as List<*>
        val media = arr[5] as List<*>
        val pending = arr[6] as Int
        val qk = arr[7] as SettingsRepository.QuickAmounts
        val fields = arr[8] as SettingsRepository.FieldTable
        val toast = arr[9] as String?
        val storage = arr[10] as Double
        SettingsUiState(
            ledgerCount = ledger.size,
            scheduleCount = schedule.size,
            wishCount = wish.size,
            habitCount = habits.size,
            fitnessCount = fitness.size,
            mediaCount = media.size,
            pendingCount = pending,
            storageSizeKb = storage,
            quickAmounts = qk,
            fields = fields,
            toastMessage = toast
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun toast(msg: String) {
        viewModelScope.launch {
            _toastFlow.emit(msg)
        }
    }

    fun clearToast() {
        viewModelScope.launch {
            _toastFlow.emit(null)
        }
    }

    suspend fun saveQuickAmounts(q: SettingsRepository.QuickAmounts) {
        container.settings.setQuickAmounts(q)
        toast("常用金额已保存")
    }

    suspend fun clearAll() {
        container.ledger.clearAll()
        container.habit.clearAll()
        container.schedule.clearAll()
        container.fitness.clearAll()
        container.fitnessPlan.clearAll()
        container.wish.clearAll()
        container.media.clearAll()
        container.settings.resetBackupCount()
        toast("全部数据已清空")
    }

    suspend fun loadDemoData() {
        clearAll()
        DemoDataUtil.seed(app)
        toast("演示数据已载入")
    }

    suspend fun setFields(table: SettingsRepository.FieldTable) {
        container.settings.setFields(table)
    }

    /** 计算某一字段的引用次数，对应网页版 fieldUsage / mtUsage */
    suspend fun fieldUsage(tab: FieldTab, name: String): Int {
        return when (tab) {
            FieldTab.EXPENSE -> container.ledger.getAllOnce().count { it.type == "expense" && it.category == name }
            FieldTab.INCOME -> container.ledger.getAllOnce().count { it.type == "income" && it.category == name }
            FieldTab.REBATE -> container.ledger.getAllOnce().count { it.type == "rebate" && it.category == name }
            FieldTab.TAG -> container.schedule.getAllOnce().count { it.tag == name }
            FieldTab.PRIORITY -> container.schedule.getAllOnce().count { it.priority == name } +
                    container.wish.getAllOnce().count { it.priority == name }
            FieldTab.MEDIA -> container.media.getAllOnce().count { it.type == name }
        }
    }

    /** 改名后同步历史数据，对应网页版 renameField */
    suspend fun renameField(tab: FieldTab, oldName: String, newName: String) {
        if (oldName == newName) return
        when (tab) {
            FieldTab.EXPENSE -> {
                container.ledger.getAllOnce()
                    .filter { it.type == "expense" && it.category == oldName }
                    .forEach { container.ledger.update(it.copy(category = newName)) }
            }
            FieldTab.INCOME -> {
                container.ledger.getAllOnce()
                    .filter { it.type == "income" && it.category == oldName }
                    .forEach { container.ledger.update(it.copy(category = newName)) }
            }
            FieldTab.REBATE -> {
                container.ledger.getAllOnce()
                    .filter { it.type == "rebate" && it.category == oldName }
                    .forEach { container.ledger.update(it.copy(category = newName)) }
            }
            FieldTab.TAG -> {
                container.schedule.getAllOnce()
                    .filter { it.tag == oldName }
                    .forEach { container.schedule.update(it.copy(tag = newName)) }
            }
            FieldTab.PRIORITY -> {
                container.schedule.getAllOnce()
                    .filter { it.priority == oldName }
                    .forEach { container.schedule.update(it.copy(priority = newName)) }
                container.wish.getAllOnce()
                    .filter { it.priority == oldName }
                    .forEach { container.wish.update(it.copy(priority = newName)) }
            }
            FieldTab.MEDIA -> {
                container.media.getAllOnce()
                    .filter { it.type == oldName }
                    .forEach { container.media.update(it.copy(type = newName)) }
            }
        }
    }

    /** 删除前把在用的记录迁移到 target；对应网页版 reassignField */
    suspend fun reassignField(tab: FieldTab, oldName: String, targetName: String): Int {
        return when (tab) {
            FieldTab.EXPENSE -> {
                val items = container.ledger.getAllOnce().filter { it.type == "expense" && it.category == oldName }
                items.forEach { container.ledger.update(it.copy(category = targetName)) }
                items.size
            }
            FieldTab.INCOME -> {
                val items = container.ledger.getAllOnce().filter { it.type == "income" && it.category == oldName }
                items.forEach { container.ledger.update(it.copy(category = targetName)) }
                items.size
            }
            FieldTab.REBATE -> {
                val items = container.ledger.getAllOnce().filter { it.type == "rebate" && it.category == oldName }
                items.forEach { container.ledger.update(it.copy(category = targetName)) }
                items.size
            }
            FieldTab.TAG -> {
                val items = container.schedule.getAllOnce().filter { it.tag == oldName }
                items.forEach { container.schedule.update(it.copy(tag = targetName)) }
                items.size
            }
            FieldTab.PRIORITY -> {
                val s = container.schedule.getAllOnce().filter { it.priority == oldName }
                val w = container.wish.getAllOnce().filter { it.priority == oldName }
                s.forEach { container.schedule.update(it.copy(priority = targetName)) }
                w.forEach { container.wish.update(it.copy(priority = targetName)) }
                s.size + w.size
            }
            FieldTab.MEDIA -> {
                val items = container.media.getAllOnce().filter { it.type == oldName }
                items.forEach { container.media.update(it.copy(type = targetName)) }
                items.size
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
class SettingsViewModelFactory(private val app: LifeHubApplication) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(app) as T
    }
}
