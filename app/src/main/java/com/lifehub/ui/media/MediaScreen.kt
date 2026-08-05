package com.lifehub.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifehub.LifeHubApplication
import com.lifehub.charts.MonthBarChart
import com.lifehub.data.entity.MediaItemEntity
import com.lifehub.ui.components.EmptyState
import com.lifehub.ui.components.LifeCard
import com.lifehub.ui.theme.*
import com.lifehub.util.cnDateKey
import com.lifehub.viewmodel.*

@Composable
fun MediaScreen() {
    val app = LocalContext.current.applicationContext as LifeHubApplication
    val vm: MediaViewModel = viewModel(factory = MediaViewModelFactory(app))
    val state by vm.uiState.collectAsState()
    val typeScope by vm.typeScope.collectAsState()
    val statusScope by vm.statusScope.collectAsState()
    val viewMode by vm.viewMode.collectAsState()
    val list = remember(state, typeScope, statusScope) { vm.filtered(state) }

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MediaItemEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text("书影音收藏", style = MaterialTheme.typography.displayMedium, color = Ink)
                    Text("看过听过的东西，值得留一句话。", style = MaterialTheme.typography.labelMedium, color = InkSoft)
                }
                FilledIconButton(
                    onClick = { showAdd = true },
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Clay)
                ) { Text("+", color = PaperCard, style = MaterialTheme.typography.titleLarge) }
            }
        }

        item { MediaMetrics(state) }

        item {
            LifeCard {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
                    Column {
                        Text("${todayYear()} 年完成分布", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Text("按完成月份统计", style = MaterialTheme.typography.labelSmall, color = InkSoft)
                    }
                    Text("共 ${state.monthBars.sum()} 条", style = MaterialTheme.typography.labelSmall, color = Clay)
                }
                Spacer(Modifier.height(10.dp))
                MonthBarChart(values = state.monthBars, color = Clay)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12").forEach {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = InkSoft, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SegmentedRow(
                    options = MediaScope.values().map { it.label },
                    keys = MediaScope.values().map { it.key },
                    selected = typeScope.key,
                    onSelect = { k -> vm.setTypeScope(MediaScope.values().first { it.key == k }) }
                )
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                    SegmentedRow(
                        modifier = Modifier.weight(1f),
                        options = MediaStatusScope.values().map { it.label },
                        keys = MediaStatusScope.values().map { it.key },
                        selected = statusScope.key,
                        onSelect = { k -> vm.setStatusScope(MediaStatusScope.values().first { it.key == k }) }
                    )
                    SegmentedRow(
                        modifier = Modifier.weight(1f),
                        options = MediaViewMode.values().map { it.label },
                        keys = MediaViewMode.values().map { it.key },
                        selected = viewMode.key,
                        onSelect = { k -> vm.setViewMode(MediaViewMode.values().first { it.key == k }) }
                    )
                }
            }
        }

        if (list.isEmpty()) {
            item { EmptyState("这里还是空的") }
        } else if (viewMode == MediaViewMode.WALL) {
            item {
                Text("收藏架 · ${list.size} 条", style = MaterialTheme.typography.titleMedium, color = Ink)
            }
            item {
                WallGrid(list) { editing = it }
            }
        } else {
            item {
                Text("列表 · ${list.size} 条", style = MaterialTheme.typography.titleMedium, color = Ink)
            }
            items(list) { item ->
                MediaRow(item, onOpen = { editing = item }, onDelete = { vm.delete(item) })
            }
        }
    }

    if (showAdd) {
        MediaAddDialog(
            onDismiss = { showAdd = false },
            onAdd = { type, title, author, status, rating, review ->
                val color = defaultColorFor(type)
                vm.add(type, title, author, status, rating, review, color)
                showAdd = false
            }
        )
    }

    editing?.let { item ->
        MediaEditDialog(
            item = item,
            onDismiss = { editing = null },
            onStatus = { vm.setStatus(item, it); editing = item.copy(status = it) },
            onRating = { vm.setRating(item, it); editing = item.copy(rating = it) },
            onDate = { vm.setFinishDate(item, it); editing = item.copy(finishDate = it) },
            onReview = { vm.setReview(item, it); editing = item.copy(review = it) },
            onDelete = { vm.delete(item); editing = null }
        )
    }
}

@Composable
private fun MediaMetrics(s: MediaUiState) {
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
        Metric("${todayYear()} 年读完", s.yearBook.toString(), "本", "书", Clay)
        Metric("${todayYear()} 年看完", s.yearMovie.toString(), "部", "影视", Sage)
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
        Metric("${todayYear()} 年收藏", s.yearMusic.toString(), "张", "音乐", Ink)
        Metric("平均评分", if (s.avgRating > 0f) String.format("%.1f", s.avgRating) else "—", "/5", "${s.ratedCount} 条已评分", Amber)
    }
}

@Composable
private fun RowScope.Metric(label: String, value: String, unit: String, desc: String, color: Color) {
    Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(10.dp),
        color = PaperCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, Line)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = InkSoft)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.headlineMedium, color = color)
                Spacer(Modifier.width(3.dp))
                Text(unit, style = MaterialTheme.typography.labelSmall, color = InkSoft)
            }
            Spacer(Modifier.height(2.dp))
            Text(desc, style = MaterialTheme.typography.labelSmall, color = InkSoft)
        }
    }
}

@Composable
private fun SegmentedRow(
    options: List<String>,
    keys: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Line, RoundedCornerShape(8.dp))
    ) {
        options.forEachIndexed { i, label ->
            val on = keys[i] == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (on) Ink else Color.Transparent)
                    .clickable { onSelect(keys[i]) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = if (on) PaperCard else InkSoft)
            }
        }
    }
}

@Composable
private fun WallGrid(items: List<MediaItemEntity>, onOpen: (MediaItemEntity) -> Unit) {
    // 用 Column 模拟 3 列网格（LazyColumn 内嵌 LazyVerticalGrid 高度冲突，手动分行）
    val cols = 3
    val rows = (items.size + cols - 1) / cols
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (c in 0 until cols) {
                    val idx = r * cols + c
                    if (idx < items.size) {
                        Box(Modifier.weight(1f)) { CoverCard(items[idx], onOpen) }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverCard(item: MediaItemEntity, onOpen: (MediaItemEntity) -> Unit) {
    val color = parseColor(item.color, Clay)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onOpen(item) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4.2f)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.73f)))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                item.title.take(1),
                color = PaperCard,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            item.title,
            style = MaterialTheme.typography.labelSmall,
            color = Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${typeLabel(item.type)} · ${statusLabel(item.status)}", style = MaterialTheme.typography.labelSmall, color = InkSoft)
        }
        if (item.rating > 0) {
            Text(stars(item.rating), style = MaterialTheme.typography.labelSmall, color = Amber)
        }
    }
}

@Composable
private fun MediaRow(item: MediaItemEntity, onOpen: (MediaItemEntity) -> Unit, onDelete: () -> Unit) {
    val color = parseColor(item.color, Clay)
    var showDeleteConfirm by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onOpen(item) },
        shape = RoundedCornerShape(8.dp),
        color = PaperCard
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(color))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.title, style = MaterialTheme.typography.bodyMedium, color = Ink, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.width(8.dp))
                    if (item.rating > 0) Text(stars(item.rating), style = MaterialTheme.typography.labelSmall, color = Amber)
                }
                Row(Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Tag(typeLabel(item.type), Clay)
                    Tag(statusLabel(item.status), if (item.status == "done") Sage else if (item.status == "doing") Clay else InkSoft)
                    if (item.finishDate.isNotBlank()) Text(cnDateKey(item.finishDate), style = MaterialTheme.typography.labelSmall, color = InkSoft)
                    if (item.review.isNotBlank()) Text(item.review, style = MaterialTheme.typography.labelSmall, color = InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                }
            }
            TextButton(onClick = { showDeleteConfirm = true }) {
                Text("删除", style = MaterialTheme.typography.labelSmall, color = Danger)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("删除", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消", color = InkSoft) }
            },
            title = { Text("确认删除", color = Ink) },
            text = { Text("确定要删除这条记录吗？", color = InkSoft) }
        )
    }
}

@Composable
private fun Tag(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha = 0.15f)) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
    }
}

@Composable
private fun MediaAddDialog(
    onDismiss: () -> Unit,
    onAdd: (type: String, title: String, author: String, status: String, rating: Float, review: String) -> Unit
) {
    var type by remember { mutableStateOf("book") }
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("want") }
    var rating by remember { mutableStateOf(0f) }
    var review by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) onAdd(type, title, author, status, rating, review)
            }) { Text("添加", color = Clay) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = InkSoft) } },
        title = { Text("添加一条", color = Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DropdownSelector(
                        label = "类型", selected = type,
                        options = listOf("book" to "书", "movie" to "影视", "music" to "音乐"),
                        modifier = Modifier.weight(1f)
                    ) { type = it }
                    DropdownSelector(
                        label = "状态", selected = status,
                        options = listOf("want" to "想看", "doing" to "在看", "done" to "看完"),
                        modifier = Modifier.weight(1f)
                    ) { status = it }
                }
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("标题") },
                    singleLine = true,
                    colors = fieldColors()
                )
                OutlinedTextField(
                    value = author, onValueChange = { author = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("作者/导演/歌手（可选）") },
                    singleLine = true,
                    colors = fieldColors()
                )
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("星级", style = MaterialTheme.typography.labelMedium, color = InkSoft)
                    StarPicker(rating) { rating = it }
                }
                OutlinedTextField(
                    value = review, onValueChange = { review = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("短评（可选）") },
                    singleLine = true,
                    colors = fieldColors()
                )
            }
        }
    )
}

@Composable
private fun MediaEditDialog(
    item: MediaItemEntity,
    onDismiss: () -> Unit,
    onStatus: (String) -> Unit,
    onRating: (Float) -> Unit,
    onDate: (String) -> Unit,
    onReview: (String) -> Unit,
    onDelete: () -> Unit
) {
    var review by remember(item.id) { mutableStateOf(item.review) }
    var date by remember(item.id) { mutableStateOf(item.finishDate) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成", color = Clay) } },
        dismissButton = {
            TextButton(onClick = { showDeleteConfirm = true }) { Text("删除", color = Danger) }
        },
        title = { Text(item.title, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DropdownSelector(
                    label = "状态", selected = item.status,
                    options = listOf("want" to "想看", "doing" to "在看", "done" to "看完"),
                    modifier = Modifier.fillMaxWidth()
                ) { onStatus(it) }

                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("星级", style = MaterialTheme.typography.labelMedium, color = InkSoft)
                    StarPicker(item.rating) { onRating(it) }
                }

                OutlinedTextField(
                    value = date, onValueChange = {
                        date = it
                        if (it.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) onDate(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("完成日期 yyyy-MM-dd") },
                    singleLine = true,
                    colors = fieldColors()
                )

                OutlinedTextField(
                    value = review, onValueChange = {
                        review = it
                        onReview(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("短评") },
                    colors = fieldColors()
                )
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("删除", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消", color = InkSoft) }
            },
            title = { Text("确认删除", color = Ink) },
            text = { Text("确定要删除这条记录吗？", color = InkSoft) }
        )
    }
}

@Composable
private fun DropdownSelector(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selLabel = options.firstOrNull { it.first == selected }?.second ?: selected
    Box(modifier) {
        OutlinedTextField(
            value = selLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Text("▾", color = InkSoft) },
            colors = fieldColors()
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (k, v) ->
                DropdownMenuItem(text = { Text(v) }, onClick = { onSelect(k); expanded = false })
            }
        }
        Box(Modifier.matchParentSize().clickable { expanded = true })
    }
}

@Composable
private fun StarPicker(value: Float, onPick: (Float) -> Unit) {
    Row {
        for (i in 1..5) {
            val on = i <= value.toInt()
            Text(
                "★",
                color = if (on) Amber else Line,
                fontSize = 22.sp,
                modifier = Modifier.clickable { onPick(if (i == value.toInt()) 0f else i.toFloat()) }.padding(horizontal = 2.dp)
            )
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(focusedBorderColor = Clay, unfocusedBorderColor = Line)

private fun stars(n: Float): String = "★".repeat(n.toInt())

private fun typeLabel(t: String) = when (t) { "book" -> "书"; "movie" -> "影"; "music" -> "音"; else -> t }
private fun statusLabel(s: String) = when (s) { "want" -> "想看"; "doing" -> "在看"; "done" -> "看完"; else -> s }

private fun defaultColorFor(type: String): String = when (type) {
    "book" -> "#A2543C"
    "movie" -> "#5D7561"
    "music" -> "#647D8E"
    else -> "#A2543C"
}

private fun parseColor(hex: String, fallback: Color): Color {
    return try {
        val h = hex.removePrefix("#")
        Color(("FF$h").toLong(16).toInt())
    } catch (_: Exception) { fallback }
}

private fun todayYear(): String = com.lifehub.util.todayKey().slice(0..3)
