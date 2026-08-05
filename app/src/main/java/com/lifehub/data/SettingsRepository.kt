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
        val expenseCats: List<CategoryDef> = listOf(
            CategoryDef("餐饮", "#A2543C"),
            CategoryDef("交通", "#647D8E"),
            CategoryDef("日用", "#5D7561"),
            CategoryDef("娱乐", "#C8893B")
        ),
        val incomeCats: List<CategoryDef> = listOf(
            CategoryDef("工资", "#5D7561"),
            CategoryDef("奖金", "#C8893B"),
            CategoryDef("其他", "#647D8E")
        ),
        val rebateCats: List<CategoryDef> = listOf(
            CategoryDef("购物返利", "#C8893B"),
            CategoryDef("餐饮返利", "#A2543C")
        ),
        val priorities: List<CategoryDef> = listOf(
            CategoryDef("P0", "#A2543C"),
            CategoryDef("P1", "#C8893B"),
            CategoryDef("P2", "#5D7561")
        ),
        val mediaTypes: List<CategoryDef> = listOf(
            CategoryDef("书", "#5D7561"),
            CategoryDef("影", "#647D8E"),
            CategoryDef("音", "#C8893B")
        )
    )

    @Serializable
    data class FitnessProfile(
        val height: Double = 170.0,    // 身高 cm
        val startWeight: Double = 0.0, // 起点体重 kg（0 = 取首条记录）
        val targetWeight: Double = 60.0,// 目标体重 kg
        val tdee: Double = 1600.0       // 基础代谢 + 日常消耗 kcal
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

    companion object {
        private val KEY_BUDGET = doublePreferencesKey("monthly_budget")
        private val KEY_NET_REBATE = booleanPreferencesKey("net_rebate")
        private val KEY_FIELDS = stringPreferencesKey("field_table")
        private val KEY_BACKUP_COUNT = intPreferencesKey("backup_count")
        private val KEY_FITNESS_PROFILE = stringPreferencesKey("fitness_profile")
    }
}
