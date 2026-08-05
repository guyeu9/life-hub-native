package com.lifehub.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import com.lifehub.data.dao.*
import com.lifehub.data.entity.*

/**
 * 生活台 Room 数据库
 * 8 张表 + 1 Habits↔HabitLogs 一对多关系
 */
@Database(
    entities = [
        LedgerEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        ScheduleEntity::class,
        FitnessEntity::class,
        FitnessPlanEntity::class,
        WishItemEntity::class,
        MediaItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LifeHubDatabase : androidx.room.RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun fitnessDao(): FitnessDao
    abstract fun fitnessPlanDao(): FitnessPlanDao
    abstract fun wishItemDao(): WishItemDao
    abstract fun mediaItemDao(): MediaItemDao

    companion object {
        @Volatile
        private var INSTANCE: LifeHubDatabase? = null

        fun get(context: Context): LifeHubDatabase {
            return INSTANCE ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    LifeHubDatabase::class.java,
                    "life_hub.db"
                )
                    .fallbackToDestructiveMigration() // 简单策略：版本不匹配时重建（后续加迁移逐版本做）
                    .build()
                INSTANCE = db
                db
            }
        }
    }
}
