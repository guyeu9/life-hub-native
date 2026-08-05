package com.lifehub.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * DataStore — 应用层配置（预算、动态字段表、主题等）
 * 替代原 HTML 版 DB.fields / DB.budget
 */

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "life_hub_settings")

class SettingsRepository(private val context: Context) {

    @Serializable
    data class CategoryDef(
        val name: String,
        val color: String = "#A2543C"
    )

    @Serializable
    data class FieldTable(
        // 对齐 life-hub.html 默认字段
        val expenseCats: List<CategoryDef> = listOf(
            CategoryDef("餐饮", "#A2543C"),
            CategoryDef("交通", "#4A6478"),
            CategoryDef("居家", "#5D7561"),
            CategoryDef("购物", "#A8842F"),
            CategoryDef("娱乐", "#8D6B52"),
            CategoryDef("医疗", "#B0433A"),
            CategoryDef("学习", "#6B6A94"),
            CategoryDef("人情", "#C2755A"),
            CategoryDef("其他", "#918C81")
        ),
        val incomeCats: List<CategoryDef> = listOf(
            CategoryDef("工资", "#4C7554"),
            CategoryDef("兼职", "#5D7561"),
            CategoryDef("理财", "#A8842F"),
            CategoryDef("红包", "#C2755A"),
            CategoryDef("其他收入", "#918C81")
        ),
        val rebateCats: List<CategoryDef> = listOf(
            CategoryDef("外卖返利", "#A8842F"),
            CategoryDef("堂食返利", "#C2955A"),
            CategoryDef("团购优惠", "#8D6B52"),
            CategoryDef("信用卡返现", "#6B6A94"),
            CategoryDef("平台补贴", "#5D7561"),
            CategoryDef("其他返利", "#918C81")
        ),
        val planTags: List<String> = listOf("生活", "工作", "家人", "健康", "家务", "学习"),
        val priorities: List<String> = listOf("P0", "P1", "P2"),
        val mediaTypes: List<CategoryDef> = listOf(
            CategoryDef("书", "#A2543C"),
            CategoryDef("影视", "#4A6478"),
            CategoryDef("音乐", "#A8842F")
        )
    )

    @Serializable
    data class FitnessProfile(
        val height: Double = 170.0,    // 身高 cm
        val startWeight: Double = 0.0, // 起点体重 kg（0 = 取首条记录）
        val targetWeight: Double = 60.0,// 目标体重 kg
        val tdee: Double = 1600.0       // 基础代谢 + 日常消耗 kcal
    )

    @Serializable
    data class QuickAmounts(
        val expense: List<Double> = listOf(10.0, 20.0, 50.0, 100.0),
        val income: List<Double> = listOf(500.0, 1000.0, 2000.0, 5000.0),
        val rebate: List<Double> = listOf(1.0, 3.0, 5.0, 10.0)
    )

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    // 月预算
    val monthlyBudget: Flow<Double> = context.dataStore.data.map { it[KEY_BUDGET] ?: 6000.0 }
    suspend fun setBudget(v: Double) { context.dataStore.edit { it[KEY_BUDGET] = v } }

    // 返利是否冲抵支出
    val netRebate: Flow<Boolean> = context.dataStore.data.map { it[KEY_NET_REBATE] ?: false }
    suspend fun setNetRebate(v: Boolean) { context.dataStore.edit { it[KEY_NET_REBATE] = v } }

    // 动态字段表
    val fields: Flow<FieldTable> = context.dataStore.data.map { prefs ->
        prefs[KEY_FIELDS]?.let {
            try {
                json.decodeFromString(FieldTable.serializer(), it)
            } catch (e: Exception) { FieldTable() }
        } ?: FieldTable()
    }
    suspend fun setFields(table: FieldTable) {
        context.dataStore.edit { it[KEY_FIELDS] = json.encodeToString(FieldTable.serializer(), table) }
    }

    // 备份提醒计数
    val backupCount: Flow<Int> = context.dataStore.data.map { it[KEY_BACKUP_COUNT] ?: 0 }
    suspend fun incrementBackupCount() {
        context.dataStore.edit { prefs ->
            prefs[KEY_BACKUP_COUNT] = (prefs[KEY_BACKUP_COUNT] ?: 0) + 1
        }
    }
    suspend fun resetBackupCount() {
        context.dataStore.edit { it[KEY_BACKUP_COUNT] = 0 }
    }

    // 健身档案（身高/起点体重/目标体重/基代 TDEE）— 对齐 HTML 版 DB.fitness.profile
    val fitnessProfile: Flow<FitnessProfile> = context.dataStore.data.map { prefs ->
        prefs[KEY_FITNESS_PROFILE]?.let {
            try {
                json.decodeFromString(FitnessProfile.serializer(), it)
            } catch (e: Exception) { FitnessProfile() }
        } ?: FitnessProfile()
    }
    suspend fun setFitnessProfile(p: FitnessProfile) {
        context.dataStore.edit { it[KEY_FITNESS_PROFILE] = json.encodeToString(FitnessProfile.serializer(), p) }
    }

    // 首页常用金额
    val quickAmounts: Flow<QuickAmounts> = context.dataStore.data.map { prefs ->
        prefs[KEY_QUICK_AMOUNTS]?.let {
            try {
                json.decodeFromString(QuickAmounts.serializer(), it)
            } catch (e: Exception) { QuickAmounts() }
        } ?: QuickAmounts()
    }
    suspend fun setQuickAmounts(q: QuickAmounts) {
        context.dataStore.edit { it[KEY_QUICK_AMOUNTS] = json.encodeToString(QuickAmounts.serializer(), q) }
    }

    /** 估算应用本地存储占用（KB），与网页版 localStorage 大小提示对齐 */
    fun approximateStorageSizeKb(): Double {
        var bytes = 0L
        val dataDir = context.filesDir.parentFile
        if (dataDir != null && dataDir.isDirectory) {
            dataDir.walkTopDown().filter { it.isFile }.forEach { bytes += it.length() }
        }
        return if (bytes <= 0) 0.0 else (bytes / 1024.0)
    }

    companion object {
        private val KEY_BUDGET = doublePreferencesKey("monthly_budget")
        private val KEY_NET_REBATE = booleanPreferencesKey("net_rebate")
        private val KEY_FIELDS = stringPreferencesKey("field_table")
        private val KEY_BACKUP_COUNT = intPreferencesKey("backup_count")
        private val KEY_FITNESS_PROFILE = stringPreferencesKey("fitness_profile")
        private val KEY_QUICK_AMOUNTS = stringPreferencesKey("quick_amounts")
    }
}
