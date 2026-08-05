package com.lifehub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 习惯定义
 */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,       // 习惯名
    val color: String = "#5D7561", // 胶囊颜色 (hex)
    val createdAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false  // 归档（不删除历史）
)
