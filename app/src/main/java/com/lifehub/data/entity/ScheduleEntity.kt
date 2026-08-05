package com.lifehub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 待办/日程
 * priority: "P0" | "P1" | "P2"  (P0=重要紧急 P1=重要 P2=常规)
 * tag: 标签（生活/工作/家人/健康/家务/学习）
 * time: 具体时间 "HH:mm"（可为空）
 */
@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,           // 标题
    val note: String = "",       // 备注
    val priority: String = "P2", // 优先级
    val tag: String = "生活",    // 标签
    val time: String = "",       // 具体时间 "HH:mm"（可为空）
    val due: Long = 0L,          // 截止日期时间戳 (0 = 无截止)
    val done: Boolean = false,   // 是否完成
    val createdAt: Long = System.currentTimeMillis()
)
