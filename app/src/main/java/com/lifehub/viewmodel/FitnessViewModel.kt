package com.lifehub.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifehub.LifeHubApplication
import com.lifehub.data.SettingsRepository
import com.lifehub.data.entity.FitnessEntity
import com.lifehub.data.entity.FitnessPlanEntity
import com.lifehub.util.todayKey
import com.lifehub.util.weekMondayKey
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FitnessUiState(
    val profile: SettingsRepository.FitnessProfile = SettingsRepository.FitnessProfile(),
    val logs: List<FitnessEntity> = emptyList(),     // 升序（旧→新）
    val plan: List<FitnessPlanEntity> = emptyList(), // 7 条
    val todayLog: FitnessEntity? = null
)

class FitnessViewModel(app: LifeHubApplication) : AndroidViewModel(app) {
    private val container = app.container

    val uiState: StateFlow<FitnessUiState> = combine(
        container.settings.fitnessProfile,
        container.fitness.getAll(),
        container.fitnessPlan.getAll()
    ) { profile, logsRaw, planRaw ->
        // logs 升序
        val logs = logsRaw.sortedBy { it.dateKey }
        // 周一自动重置 done
        val thisWeek = weekMondayKey()
        val plan = if (planRaw.isNotEmpty() && planRaw.first().planWeek != thisWeek) {
            planRaw.map { it.copy(done = false, planWeek = thisWeek) }
        } else if (planRaw.isEmpty()) {
            // 首次初始化 7 条空计划
            (0..6).map { FitnessPlanEntity(dayIndex = it, planWeek = thisWeek) }
        } else {
            planRaw
        }
        FitnessUiState(
            profile = profile,
            logs = logs,
            plan = plan,
            todayLog = logs.firstOrNull { it.dateKey == todayKey() }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FitnessUiState())

    init {
        // 首次启动时把 7 条空计划写进库
        viewModelScope.launch {
            val current = container.fitnessPlan.getAllOnce()
            if (current.isEmpty()) {
                val wk = weekMondayKey()
                container.fitnessPlan.upsertAll((0..6).map { FitnessPlanEntity(dayIndex = it, planWeek = wk) })
            } else if (current.first().planWeek != weekMondayKey()) {
                // 跨周重置
                container.fitnessPlan.resetAllDone()
                current.forEach { container.fitnessPlan.upsert(it.copy(planWeek = weekMondayKey())) }
            }
        }
    }

    fun saveLog(dateKey: String, weight: Double, fat: Double, intake: Double, burn: Double, note: String) {
        viewModelScope.launch {
            container.fitness.upsertByDateKey(
                FitnessEntity(
                    dateKey = dateKey,
                    weight = weight,
                    fat = fat,
                    intake = intake,
                    burn = burn,
                    note = note,
                    date = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteLog(item: FitnessEntity) {
        viewModelScope.launch { container.fitness.delete(item) }
    }

    fun togglePlanDone(dayIndex: Int) {
        viewModelScope.launch {
            val plan = uiState.value.plan
            val cur = plan.firstOrNull { it.dayIndex == dayIndex } ?: return@launch
            container.fitnessPlan.setDone(dayIndex, !cur.done)
        }
    }

    fun editPlan(dayIndex: Int, title: String, detail: String) {
        viewModelScope.launch {
            val plan = uiState.value.plan
            val cur = plan.firstOrNull { it.dayIndex == dayIndex } ?: return@launch
            container.fitnessPlan.upsert(cur.copy(title = title, detail = detail))
        }
    }

    fun updateProfile(p: SettingsRepository.FitnessProfile) {
        viewModelScope.launch { container.settings.setFitnessProfile(p) }
    }
}
