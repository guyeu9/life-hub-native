package com.lifehub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 习惯打卡记录（每日一条）
 * dateKey 格式: "yyyy-MM-dd"
 * done: 是否达标 (value >= target 时为 true)
 * value: count/value 类型的具体数值 (check 类型始终为 1.0)
 */
@Entity(tableName = "habit_logs")
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,        // 对应 HabitEntity.id
    val dateKey: String,      // "2024-06-15"
    val done: Boolean = true, // 是否达标
    val value: Double = 0.0   // count/value 类型的具体数值
)
