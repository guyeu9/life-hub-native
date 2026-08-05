package com.lifehub

import android.app.Application
import com.lifehub.data.LifeHubDatabase
import com.lifehub.data.SettingsRepository
import com.lifehub.data.repository.*

/**
 * 应用入口 — 集中初始化数据库与各 Repository（简易依赖容器）
 * 新手不用管这个，ViewModel 会自动从这里取数据层
 */
class LifeHubApplication : Application() {

    val database by lazy { LifeHubDatabase.get(this) }

    val container by lazy {
        AppContainer(
            ledger = LedgerRepository(database.ledgerDao()),
            habit = HabitRepository(database.habitDao(), database.habitLogDao()),
            schedule = ScheduleRepository(database.scheduleDao()),
            fitness = FitnessRepository(database.fitnessDao()),
            fitnessPlan = FitnessPlanRepository(database.fitnessPlanDao()),
            wish = WishRepository(database.wishItemDao()),
            media = MediaRepository(database.mediaItemDao()),
            settings = SettingsRepository(this)
        )
    }

    data class AppContainer(
        val ledger: LedgerRepository,
        val habit: HabitRepository,
        val schedule: ScheduleRepository,
        val fitness: FitnessRepository,
        val fitnessPlan: FitnessPlanRepository,
        val wish: WishRepository,
        val media: MediaRepository,
        val settings: SettingsRepository
    )
}
