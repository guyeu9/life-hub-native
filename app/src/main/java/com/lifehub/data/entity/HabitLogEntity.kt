package com.lifehub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 习惯打卡记录（每日一条）
 * dateKey 格式: "yyyy-MM-dd"
 */
@Entity(tableName = "habit_logs")
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,       // 对应 HabitEntity.id
    val dateKey: String,     // "2024-06-15"
    val done: Boolean = true // 打卡=完成
)
