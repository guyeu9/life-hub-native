package com.lifehub.ui.media

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import com.lifehub.ui.components.AnimatedHeader
import com.lifehub.ui.components.AnimatedNumber
import com.lifehub.ui.components.ConfettiOverlay
import com.lifehub.ui.components.animateItemSlide
import com.lifehub.ui.components.EmptyState
import com.lifehub.ui.components.LifeCard
import com.lifehub.ui.components.SuccessButton
import com.lifehub.ui.components.hapticClick
import com.lifehub.ui.components.pressScale
import com.lifehub.ui.components.toggleClick
import com.lifehub.ui.theme.*
import com.lifehub.util.cnDateKey
import com.lifehub.util.todayKey
import com.lifehub.util.vibrateLight
import com.lifehub.util.vibrateMedium
import com.lifehub.util.vibrateSuccess
import com.lifehub.util.vibrateTick
import com.lifehub.viewmodel.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun MediaScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as LifeHubApplication
    val vm: MediaViewModel = viewModel(factory = MediaViewModelFactory(app))
    val state by vm.uiState.collectAsState()
    val typeScope by vm.typeScope.collectAsState()
    val statusScope by vm.statusScope.collectAsState()
    val viewMode by vm.viewMode.collectAsState()
    val list = remember(state, typeScope, statusScope) { vm.filtered(state) }

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MediaItemEntity?>(null) }
    var confettiKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    AnimatedHeader(
                        title = "书影音收藏",
                        subtitle = "看过听过的东西，值得留一句话。"
                    )
                    FilledIconButton(
                        onClick = {
                            context.vibrateSuccess()
                            showAdd = true
                        },
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
                itemsIndexed(list) { index, item ->
                    MediaRow(
                        item = item,
                        onOpen = { editing = item },
                        onDelete = {
                            context.vibrateSuccess()
                            vm.delete(item)
                        },
                        modifier = Modifier.animateItemSlide(index)
                    )
                }
            }
        }

        ConfettiOverlay(trigger = confettiKey, modifier = Modifier.fillMaxSize())
    }

    if (showAdd) {
        MediaAddDialog(
            onDismiss = { showAdd = false },
            onAdd = { type, title, author, status, rating, review ->
                val color = defaultColorFor(type)
                vm.add(type, title, author, status, rating, review, color)
                if (status == "done" || rating > 0f) {
                    context.vibrateSuccess()
                    confettiKey++
                }
                showAdd = false
            }
        )
    }

    editing?.let { item ->
        MediaEditDialog(
            item = item,
            onDismiss = { editing = null },
            onStatus = { status ->
                if (status == "done" && item.status != "done") {
                    context.vibrateSuccess()
                    confettiKey++
                }
                vm.setStatus(item, status)
                editing = item.copy(
                    status = status,
                    finishDate = when {
                        status == "done" && item.finishDate.isBlank() -> todayKey()
                        status != "done" -> ""
                        else -> item.finishDate
                    }
                )
            },
            onRating = { rating ->
                if (rating > 0f && item.rating == 0f) {
                    context.vibrateSuccess()
                    confettiKey++
                }
                vm.setRating(item, rating)
                editing = if (rating > 0f && item.status != "done") {
                    item.copy(
                        rating = rating,
                        status = "done",
                        finishDate = if (item.finishDate.isBlank()) todayKey() else item.finishDate
                    )
                } else {
                    item.copy(rating = rating)
                }
            },
            onDate = { vm.setFinishDate(item, it); editing = item.copy(finishDate = it) },
            onReview = { vm.setReview(item, it); editing = item.copy(review = it) },
            onCover = { uri ->
                scope.launch {
                    val path = saveCoverImage(context, uri, item.id)
                    if (!path.isNullOrBlank() && editing?.id == item.id) {
                        vm.setCover(item, path)
                        editing = item.copy(cover = path)
                    }
                }
            },
            onClearCover = {
                vm.clearCover(item)
                editing = item.copy(cover = "")
            },
            onDelete = {
                context.vibrateSuccess()
                vm.delete(item)
                editing = null
            }
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
                AnimatedNumber(value = value, color = color)
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
    val ctx = LocalContext.current
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
                    .clickable {
                        ctx.vibrateLight()
                        onSelect(keys[i])
                    }
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
            .pressScale { onOpen(item) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4.2f)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            CoverImage(
                cover = item.cover,
                modifier = Modifier.fillMaxSize(),
                fallback = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
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
                }
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
private fun MediaRow(
    item: MediaItemEntity,
    onOpen: (MediaItemEntity) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = parseColor(item.color, Clay)
    var showDeleteConfirm by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxWidth().clickable { onOpen(item) },
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
            val ctx = LocalContext.current
            TextButton(
                onClick = {
                    ctx.vibrateMedium()
                    showDeleteConfirm = true
                }
            ) {
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
            SuccessButton(
                text = "添加",
                onClick = {
                    if (title.isNotBlank()) onAdd(type, title, author, status, rating, review)
                },
                enabled = title.isNotBlank()
            )
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
    onCover: (String) -> Unit,
    onClearCover: () -> Unit,
    onDelete: () -> Unit
) {
    var review by remember(item.id) { mutableStateOf(item.review) }
    var date by remember(item.id) { mutableStateOf(item.finishDate) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val color = parseColor(item.color, Clay)

    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onCover(it.toString()) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { SuccessButton(onClick = onDismiss, text = "完成") },
        dismissButton = {
            TextButton(onClick = { showDeleteConfirm = true }) { Text("删除", color = Danger) }
        },
        title = { Text(item.title, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 封面
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .width(96.dp)
                            .aspectRatio(3f / 4.2f)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        CoverImage(
                            cover = item.cover,
                            modifier = Modifier.fillMaxSize(),
                            fallback = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
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
                            }
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { coverPicker.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (item.cover.isNotBlank()) "换封面" else "上传封面", style = MaterialTheme.typography.labelSmall)
                        }
                        if (item.cover.isNotBlank()) {
                            OutlinedButton(
                                onClick = onClearCover,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)
                            ) {
                                Text("移除封面", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

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
                modifier = Modifier.toggleClick { onPick(if (i == value.toInt()) 0f else i.toFloat()) }.padding(horizontal = 2.dp)
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

/** 加载封面：支持 content URI、file 路径、base64 data URL */
@Composable
private fun CoverImage(
    cover: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable () -> Unit
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, cover) {
        value = loadCoverBitmap(context, cover)
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        fallback()
    }
}

private suspend fun loadCoverBitmap(context: Context, cover: String): ImageBitmap? = withContext(Dispatchers.IO) {
    if (cover.isBlank()) return@withContext null
    try {
        val bmp = when {
            cover.startsWith("content://") || cover.startsWith("file://") -> {
                context.contentResolver.openInputStream(Uri.parse(cover))?.use {
                    BitmapFactory.decodeStream(it)
                }
            }
            cover.startsWith("data:") -> {
                val base64 = cover.substringAfter(",", cover)
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            else -> BitmapFactory.decodeFile(cover)
        }
        bmp?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

/** 把用户选择的图片压缩到 240px 宽后存到应用私有目录，返回本地路径 */
private suspend fun saveCoverImage(context: Context, uriString: String, id: Long): String? = withContext(Dispatchers.IO) {
    try {
        val uri = Uri.parse(uriString)
        context.contentResolver.openInputStream(uri)?.use { input ->
            var bmp = BitmapFactory.decodeStream(input) ?: return@withContext null
            val maxWidth = 240
            if (bmp.width > maxWidth) {
                val ratio = maxWidth.toFloat() / bmp.width
                val h = (bmp.height * ratio).toInt()
                bmp = Bitmap.createScaledBitmap(bmp, maxWidth, h, true)
            }
            val dir = File(context.filesDir, "covers").apply { mkdirs() }
            val file = File(dir, "$id.jpg")
            file.outputStream().use { out ->
                bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        }
    } catch (_: Exception) {
        null
    }
}

private fun parseColor(hex: String, fallback: Color): Color {
    return try {
        val h = hex.removePrefix("#")
        Color(("FF$h").toLong(16).toInt())
    } catch (_: Exception) { fallback }
}

private fun todayYear(): String = com.lifehub.util.todayKey().slice(0..3)
