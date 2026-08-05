package com.lifehub.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 书影音收藏
 * type: "book" | "movie" | "music"
 * status: "want" | "doing" | "done"
 * 对齐 HTML 版 DB.media
 */
@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,           // "book" | "movie" | "music"
    val title: String,          // 标题
    val author: String = "",    // 作者/导演/歌手
    val status: String = "want",// "want" | "doing" | "done"
    val rating: Float = 0f,     // 评分 0-5
    val review: String = "",    // 短评
    val cover: String = "",     // 封面（本地路径或 base64，暂留字段）
    val color: String = "#A2543C", // 标签颜色
    val finishDate: String = "",// yyyy-MM-dd 完成日期
    val updatedAt: Long = System.currentTimeMillis()
)
