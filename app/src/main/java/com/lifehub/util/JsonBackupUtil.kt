package com.lifehub.util

import android.content.Context
import android.net.Uri
import com.lifehub.LifeHubApplication
import com.lifehub.data.SettingsRepository
import com.lifehub.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 全量备份导入导出
 * 把 Room 里的所有表 + DataStore 设置序列化成 JSON
 */

@Serializable
private data class BLedger(val id: Long, val type: String, val category: String, val amount: Double, val note: String, val rebateOf: Double, val date: Long)
@Serializable
private data class BHabit(val id: Long, val name: String, val type: String, val target: Int, val unit: String, val color: String, val createdAt: Long, val archived: Boolean)
@Serializable
private data class BHabitLog(val id: Long, val habitId: Long, val dateKey: String, val done: Boolean, val value: Double)
@Serializable
private data class BSchedule(val id: Long, val title: String, val note: String, val priority: String, val tag: String, val time: String, val due: Long, val done: Boolean, val createdAt: Long)
@Serializable
private data class BFitness(val id: Long, val dateKey: String, val weight: Double, val fat: Double, val intake: Double, val burn: Double, val note: String, val date: Long)
@Serializable
private data class BFitnessPlan(val dayIndex: Int, val title: String, val detail: String, val done: Boolean, val planWeek: String)
@Serializable
private data class BWish(val id: Long, val name: String, val estPrice: Double, val priority: String, val note: String, val url: String, val bought: Boolean, val boughtLedgerId: Long, val createdAt: Long)
@Serializable
private data class BMedia(val id: Long, val type: String, val title: String, val author: String, val status: String, val rating: Float, val review: String, val cover: String, val color: String, val finishDate: String, val updatedAt: Long)
@Serializable
private data class BSettings(
    val monthlyBudget: Double,
    val netRebate: Boolean,
    val fields: String,
    val fitnessProfile: String,
    val quickAmounts: String
)

@Serializable
private data class Backup(
    val version: Int = 2,
    val ledger: List<BLedger>,
    val habits: List<BHabit>,
    val habitLogs: List<BHabitLog>,
    val schedules: List<BSchedule>,
    val fitness: List<BFitness>,
    val fitnessPlan: List<BFitnessPlan>,
    val wishes: List<BWish>,
    val media: List<BMedia>,
    val settings: BSettings? = null
)

private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

object JsonBackupUtil {

    /**
     * 导出全部数据到指定 URI
     * @return true 成功, false 失败
     */
    suspend fun exportBackup(context: Context, container: LifeHubApplication.AppContainer, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val settings = BSettings(
                    monthlyBudget = container.settings.monthlyBudget.first(),
                    netRebate = container.settings.netRebate.first(),
                    fields = json.encodeToString(SettingsRepository.FieldTable.serializer(), container.settings.fields.first()),
                    fitnessProfile = json.encodeToString(SettingsRepository.FitnessProfile.serializer(), container.settings.fitnessProfile.first()),
                    quickAmounts = json.encodeToString(SettingsRepository.QuickAmounts.serializer(), container.settings.quickAmounts.first())
                )
                val backup = Backup(
                    ledger = container.ledger.getAllOnce().map { BLedger(it.id, it.type, it.category, it.amount, it.note, it.rebateOf, it.date) },
                    habits = container.habit.getAllHabitsOnce().map { BHabit(it.id, it.name, it.type, it.target, it.unit, it.color, it.createdAt, it.archived) },
                    habitLogs = container.habit.getAllLogsOnce().map { BHabitLog(it.id, it.habitId, it.dateKey, it.done, it.value) },
                    schedules = container.schedule.getAllOnce().map { BSchedule(it.id, it.title, it.note, it.priority, it.tag, it.time, it.due, it.done, it.createdAt) },
                    fitness = container.fitness.getAllOnce().map { BFitness(it.id, it.dateKey, it.weight, it.fat, it.intake, it.burn, it.note, it.date) },
                    fitnessPlan = container.fitnessPlan.getAllOnce().map { BFitnessPlan(it.dayIndex, it.title, it.detail, it.done, it.planWeek) },
                    wishes = container.wish.getAllOnce().map { BWish(it.id, it.name, it.estPrice, it.priority, it.note, it.url, it.bought, it.boughtLedgerId, it.createdAt) },
                    media = container.media.getAllOnce().map { BMedia(it.id, it.type, it.title, it.author, it.status, it.rating, it.review, it.cover, it.color, it.finishDate, it.updatedAt) },
                    settings = settings
                )
                val text = json.encodeToString(backup)
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(text.toByteArray(Charsets.UTF_8))
                }
                container.settings.resetBackupCount()
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * 从指定 URI 导入数据（覆盖模式）
     * @return true 成功, false 失败（文件无效或解析错误）
     */
    suspend fun importBackup(
        context: Context,
        container: LifeHubApplication.AppContainer,
        uri: Uri
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.use { input ->
                    BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
                } ?: return@withContext false

                val backup = json.decodeFromString<Backup>(text)

                // 清空再写入（导入即覆盖）
                container.ledger.clearAll()
                container.habit.clearAll()
                container.schedule.clearAll()
                container.fitness.clearAll()
                container.fitnessPlan.clearAll()
                container.wish.clearAll()
                container.media.clearAll()

                container.ledger.insertAll(backup.ledger.map { LedgerEntity(it.id, it.type, it.category, it.amount, it.note, it.rebateOf, it.date) })
                container.habit.insertAllHabits(backup.habits.map { HabitEntity(it.id, it.name, it.type, it.target, it.unit, it.color, it.createdAt, it.archived) })
                container.habit.insertAllLogs(backup.habitLogs.map { HabitLogEntity(it.id, it.habitId, it.dateKey, it.done, it.value) })
                container.schedule.insertAll(backup.schedules.map { ScheduleEntity(it.id, it.title, it.note, it.priority, it.tag, it.time, it.due, it.done, it.createdAt) })
                container.fitness.insertAll(backup.fitness.map { FitnessEntity(it.id, it.dateKey, it.weight, it.fat, it.intake, it.burn, it.note, it.date) })
                container.fitnessPlan.upsertAll(backup.fitnessPlan.map { FitnessPlanEntity(it.dayIndex, it.title, it.detail, it.done, it.planWeek) })
                container.wish.insertAll(backup.wishes.map { WishItemEntity(it.id, it.name, it.estPrice, it.priority, it.note, it.url, it.bought, it.boughtLedgerId, it.createdAt) })
                container.media.insertAll(backup.media.map { MediaItemEntity(it.id, it.type, it.title, it.author, it.status, it.rating, it.review, it.cover, it.color, it.finishDate, it.updatedAt) })

                // 恢复设置
                backup.settings?.let { s ->
                    container.settings.setBudget(s.monthlyBudget)
                    container.settings.setNetRebate(s.netRebate)
                    if (s.fields.isNotBlank()) {
                        try {
                            val ft = json.decodeFromString(SettingsRepository.FieldTable.serializer(), s.fields)
                            container.settings.setFields(ft)
                        } catch (_: Exception) { }
                    }
                    if (s.fitnessProfile.isNotBlank()) {
                        try {
                            val fp = json.decodeFromString(SettingsRepository.FitnessProfile.serializer(), s.fitnessProfile)
                            container.settings.setFitnessProfile(fp)
                        } catch (_: Exception) { }
                    }
                    if (s.quickAmounts.isNotBlank()) {
                        try {
                            val qa = json.decodeFromString(SettingsRepository.QuickAmounts.serializer(), s.quickAmounts)
                            container.settings.setQuickAmounts(qa)
                        } catch (_: Exception) { }
                    }
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
