package com.lifehub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 健身记录（体重/体脂/摄入/消耗，同一天一条，重复写入覆盖）
 * 对齐 HTML 版 DB.fitness.logs 的单条多字段结构
 */
@Entity(tableName = "fitness")
data class FitnessEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String,          // yyyy-MM-dd（同一天唯一，用于去重覆盖）
    val weight: Double = 0.0,     // 体重 kg
    val fat: Double = 0.0,        // 体脂 %
    val intake: Double = 0.0,     // 摄入 kcal
    val burn: Double = 0.0,       // 运动消耗 kcal
    val note: String = "",
    val date: Long                // 时间戳 (millis)
)
