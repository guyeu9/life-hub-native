package com.lifehub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 记账记录（支出/收入/返利）
 * type: "expense" | "income" | "rebate"
 */
@Entity(tableName = "ledger")
data class LedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,       // "expense" | "income" | "rebate"
    val category: String,   // 分类名（动态字段表引用）
    val amount: Double,     // 金额
    val note: String = "",  // 备注
    val rebateOf: Double = 0.0, // 返利对应消费原价（仅 rebate 类型）
    val date: Long           // 记录时间戳 (millis)
)
