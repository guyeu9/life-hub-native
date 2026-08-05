package com.lifehub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 习惯定义
 * type: "check" (勾选) | "count" (计数) | "value" (数值)
 */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,            // 习惯名
    val type: String = "check",  // check | count | value
    val target: Int = 1,         // 每日目标值 (check=1, count=次数, value=数值)
    val unit: String = "",       // 单位（杯/分钟/页等）
    val color: String = "#5D7561", // 胶囊颜色 (hex)
    val createdAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false  // 归档（不删除历史）
)
