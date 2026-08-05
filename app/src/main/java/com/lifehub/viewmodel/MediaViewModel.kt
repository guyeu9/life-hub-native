package com.lifehub.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifehub.LifeHubApplication
import com.lifehub.data.entity.MediaItemEntity
import com.lifehub.util.todayKey
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class MediaScope(val key: String, val label: String) {
    ALL("all", "全部"), BOOK("book", "书"), MOVIE("movie", "影"), MUSIC("music", "音")
}

enum class MediaStatusScope(val key: String, val label: String) {
    ALL("all", "不限"), WANT("want", "想看"), DOING("doing", "在看"), DONE("done", "看完")
}

enum class MediaViewMode(val key: String, val label: String) {
    WALL("wall", "封面墙"), LIST("list", "列表")
}

data class MediaUiState(
    val all: List<MediaItemEntity> = emptyList(),
    val yearBook: Int = 0,
    val yearMovie: Int = 0,
    val yearMusic: Int = 0,
    val avgRating: Float = 0f,
    val ratedCount: Int = 0,
    /** 当年 12 个月完成分布（按完成月份统计） */
    val monthBars: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
)

class MediaViewModel(app: LifeHubApplication) : AndroidViewModel(app) {
    private val container = app.container

    private val _typeScope = MutableStateFlow(MediaScope.ALL)
    val typeScope: StateFlow<MediaScope> = _typeScope

    private val _statusScope = MutableStateFlow(MediaStatusScope.ALL)
    val statusScope: StateFlow<MediaStatusScope> = _statusScope

    private val _viewMode = MutableStateFlow(MediaViewMode.WALL)
    val viewMode: StateFlow<MediaViewMode> = _viewMode

    val uiState: StateFlow<MediaUiState> = container.media.getAll().map { all ->
        val yr = todayKey().slice(0..3)
        val doneThisYear = all.filter { it.status == "done" && it.finishDate.startsWith(yr) }
        val months = IntArray(12)
        doneThisYear.forEach { x ->
            if (x.finishDate.length >= 7) {
                val m = x.finishDate.substring(5, 7).toIntOrNull()
                if (m != null && m in 1..12) months[m - 1]++
            }
        }
        val rated = doneThisYear.filter { it.rating > 0 }
        val avg = if (rated.isEmpty()) 0f else rated.sumOf { it.rating.toDouble() }.toFloat() / rated.size
        MediaUiState(
            all = all,
            yearBook = doneThisYear.count { it.type == "book" },
            yearMovie = doneThisYear.count { it.type == "movie" },
            yearMusic = doneThisYear.count { it.type == "music" },
            avgRating = avg,
            ratedCount = rated.size,
            monthBars = months.toList()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MediaUiState())

    fun setTypeScope(s: MediaScope) { _typeScope.value = s }
    fun setStatusScope(s: MediaStatusScope) { _statusScope.value = s }
    fun setViewMode(v: MediaViewMode) { _viewMode.value = v }

    /** HTML 版排序：doing → want → done；同状态按完成日期倒序（新→旧） */
    fun filtered(state: MediaUiState): List<MediaItemEntity> {
        val order = mapOf("doing" to 0, "want" to 1, "done" to 2)
        return state.all.filter { x ->
            (_typeScope.value == MediaScope.ALL || x.type == _typeScope.value.key) &&
            (_statusScope.value == MediaStatusScope.ALL || x.status == _statusScope.value.key)
        }.sortedWith(compareBy(
            { order[it.status] ?: 3 },
            { it.finishDate.ifEmpty { "0000-00-00" } }
        )).let { it.asReversed() } // 日期倒序
            .sortedWith(compareBy { order[it.status] ?: 3 }) // 重新按状态分组
    }

    fun add(type: String, title: String, author: String, status: String, rating: Float, review: String, color: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val finishDate = if (status == "done") todayKey() else ""
            container.media.insert(
                MediaItemEntity(
                    type = type, title = title.trim(), author = author.trim(),
                    status = status, rating = rating, review = review.trim(),
                    color = color, finishDate = finishDate
                )
            )
        }
    }

    fun update(item: MediaItemEntity) {
        viewModelScope.launch { container.media.update(item.copy(updatedAt = System.currentTimeMillis())) }
    }

    fun setStatus(item: MediaItemEntity, status: String) {
        val finishDate = when {
            status == "done" && item.finishDate.isBlank() -> todayKey()
            status != "done" -> ""
            else -> item.finishDate
        }
        update(item.copy(status = status, finishDate = finishDate))
    }

    fun setRating(item: MediaItemEntity, rating: Float) = update(item.copy(rating = rating))
    fun setFinishDate(item: MediaItemEntity, date: String) = update(item.copy(finishDate = date))
    fun setReview(item: MediaItemEntity, review: String) = update(item.copy(review = review))

    fun delete(item: MediaItemEntity) {
        viewModelScope.launch { container.media.delete(item) }
    }
}
