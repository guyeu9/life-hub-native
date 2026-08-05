package com.lifehub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 本周训练计划（固定 7 条，周一到周日）
 * 对齐 HTML 版 DB.fitness.plan（每周一自动重置 done）
 */
@Entity(tableName = "fitness_plan")
data class FitnessPlanEntity(
    @PrimaryKey val dayIndex: Int, // 0=周一 ... 6=周日
    val title: String = "",        // 训练标题
    val detail: String = "",       // 训练内容
    val done: Boolean = false,
    val planWeek: String = ""      // 本周所在周一日期键 yyyy-MM-dd，用于判断是否需要重置
)
