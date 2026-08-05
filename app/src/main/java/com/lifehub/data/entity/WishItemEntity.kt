package com.lifehub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 待买清单
 * 对齐 HTML 版 DB.wishlist
 */
@Entity(tableName = "wish_items")
data class WishItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,           // 物品名
    val estPrice: Double = 0.0, // 预估价格
    val priority: String = "P1",// 优先级 P0/P1/P2
    val note: String = "",
    val url: String = "",       // 链接
    val bought: Boolean = false,// 是否已买
    val boughtLedgerId: Long = 0L, // 已买并记账后写入的账目 id（防重复记账）
    val createdAt: Long = System.currentTimeMillis()
)
