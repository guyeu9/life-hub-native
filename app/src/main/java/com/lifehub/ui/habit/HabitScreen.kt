package com.lifehub.ui.habit

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifehub.LifeHubApplication
import com.lifehub.charts.HeatmapChart
import com.lifehub.data.entity.HabitEntity
import com.lifehub.ui.components.*
import com.lifehub.ui.theme.*
import com.lifehub.util.vibrateLight
import com.lifehub.util.vibrateSuccess
import com.lifehub.util.vibrateTick
import com.lifehub.util.vibrateMedium
import com.lifehub.viewmodel.HabitView
import com.lifehub.viewmodel.HabitViewModel
import com.lifehub.viewmodel.HabitViewModelFactory

@Composable
fun HabitScreen() {
    val app = LocalContext.current.applicationContext as LifeHubApplication
    val vm: HabitViewModel = viewModel(factory = HabitViewModelFactory(app))
    val habits by vm.habits.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<HabitEntity?>(null) }
    var confettiKey by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    val total = habits.size
    val doneTodayCount = habits.count { it.todayDone }
    val maxStreak = habits.maxOfOrNull { it.streak } ?: 0
    val avgRate30 = if (total > 0) habits.map { it.rate30 }.average().toFloat() else 0f

    deleteTarget?.let { h ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除习惯") },
            text = { Text("确定删除「${h.name}」及其所有打卡记录？") },
            confirmButton = {
                TextButton(onClick = {
                    context.vibrateMedium()
                    vm.deleteHabit(h)
                    deleteTarget = null
                }) {
                    Text("删除", color = Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }

    if (showAdd) {
        AddHabitSheet(
            onDismiss = { showAdd = false },
            onAdd = { name, type, target, unit ->
                vm.addHabit(name, type, target, unit)
                context.vibrateSuccess()
                confettiKey++
                showAdd = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    AnimatedHeader("习惯健康")
                    OutlinedButton(onClick = {
                        context.vibrateLight()
                        showAdd = true
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("习惯")
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard("今日完成", "$doneTodayCount/$total", Modifier.weight(1f))
                    MetricCard("最长连续", "${maxStreak}天", Modifier.weight(1f))
                    MetricCard("30天达成", "${(avgRate30 * 100).toInt()}%", Modifier.weight(1f))
                    MetricCard("正在跟踪", "$total", Modifier.weight(1f))
                }
            }
            if (habits.isEmpty()) {
                item { EmptyState("还没有习惯，点右上角添加一个") }
            }
            itemsIndexed(habits, key = { _, hv -> hv.habit.id }) { index, hv ->
                HabitCard(
                    hv = hv,
                    modifier = Modifier.animateItemSlide(index),
                    onToggleCheck = {
                        if (!hv.todayDone) {
                            context.vibrateSuccess()
                            confettiKey++
                        } else {
                            context.vibrateTick()
                        }
                        vm.toggleCheck(hv.habit)
                    },
                    onIncrement = { delta ->
                        context.vibrateTick()
                        vm.incrementCount(hv.habit, delta)
                    },
                    onSetValue = { v ->
                        if (v != hv.todayValue) {
                            context.vibrateTick()
                        }
                        vm.setValue(hv.habit, v)
                    },
                    onDelete = {
                        context.vibrateLight()
                        deleteTarget = hv.habit
                    }
                )
            }
        }

        ConfettiOverlay(
            trigger = confettiKey,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = PaperCard,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Line)
    ) {
        Column(
            Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = InkSoft)
            AnimatedNumber(value = value, color = Ink)
        }
    }
}

@Composable
private fun HabitCard(
    hv: HabitView,
    modifier: Modifier = Modifier,
    onToggleCheck: () -> Unit,
    onIncrement: (Int) -> Unit,
    onSetValue: (Double) -> Unit,
    onDelete: () -> Unit
) {
    val color = Color(AndroidColor.parseColor(hv.habit.color))
    LifeCard(modifier = modifier) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(hv.habit.name, style = MaterialTheme.typography.titleMedium, color = Ink)
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "删除", tint = InkSoft)
            }
        }
        Spacer(Modifier.height(2.dp))
        val typeLabel = when (hv.habit.type) {
            "check" -> "勾选"
            "count" -> "计数"
            "value" -> "数值"
            else -> "勾选"
        }
        val unitPart = if (hv.habit.unit.isNotBlank()) " ${hv.habit.unit}" else ""
        val ratePct = (hv.rate30 * 100).toInt()
        Text(
            "$typeLabel · 目标 ${hv.habit.target}$unitPart · 连续 ${hv.streak} 天 · 30天达成 $ratePct%",
            style = MaterialTheme.typography.labelSmall,
            color = InkSoft
        )
        Spacer(Modifier.height(10.dp))
        when (hv.habit.type) {
            "count" -> CountStepper(
                cur = hv.todayValue.toInt(),
                target = hv.habit.target,
                color = color,
                onIncrement = onIncrement
            )
            "value" -> ValueInput(
                habitId = hv.habit.id,
                cur = hv.todayValue,
                target = hv.habit.target,
                unit = hv.habit.unit,
                color = color,
                onSetValue = onSetValue
            )
            else -> CheckButton(checked = hv.todayDone, color = color, onClick = onToggleCheck)
        }
        Spacer(Modifier.height(10.dp))
        HeatmapChart(states = hv.last30States, color = color)
    }
}

@Composable
private fun CheckButton(checked: Boolean, color: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (checked) color else Color.Transparent)
                .border(1.5.dp, color, CircleShape)
                .toggleClick { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "完成",
                    tint = PaperCard,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CountStepper(
    cur: Int,
    target: Int,
    color: Color,
    onIncrement: (Int) -> Unit
) {
    val ctx = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = {
            ctx.vibrateTick()
            onIncrement(-1)
        }) {
            Icon(Icons.Filled.Remove, contentDescription = "减少", tint = color)
        }
        Text("$cur / $target", style = MaterialTheme.typography.titleMedium, color = Ink)
        IconButton(onClick = {
            ctx.vibrateTick()
            onIncrement(1)
        }) {
            Icon(Icons.Filled.Add, contentDescription = "增加", tint = color)
        }
    }
}

@Composable
private fun ValueInput(
    habitId: Long,
    cur: Double,
    target: Int,
    unit: String,
    color: Color,
    onSetValue: (Double) -> Unit
) {
    var valueText by remember(habitId) { mutableStateOf(formatDouble(cur)) }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = valueText,
            onValueChange = {
                valueText = it
                val v = it.toDoubleOrNull()
                if (v != null) onSetValue(v)
            },
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("0") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = color,
                unfocusedBorderColor = Line
            )
        )
        Text(
            "/ $target $unit",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft
        )
    }
}

private fun formatDouble(v: Double): String {
    if (v == 0.0) return ""
    return v.toBigDecimal().stripTrailingZeros().toPlainString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddHabitSheet(
    onDismiss: () -> Unit,
    onAdd: (String, String, Int, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var typeLabel by remember { mutableStateOf("勾选") }
    var targetText by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("") }

    val typeMap = mapOf("勾选" to "check", "计数" to "count", "数值" to "value")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("新习惯", style = MaterialTheme.typography.headlineMedium, color = Ink)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("习惯名称，如：喝水/阅读") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Clay, unfocusedBorderColor = Line)
            )
            Text("类型", style = MaterialTheme.typography.labelMedium, color = InkSoft)
            SegmentedButton(
                options = listOf("勾选", "计数", "数值"),
                selected = typeLabel,
                onSelect = { typeLabel = it }
            )
            if (typeLabel != "勾选") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.weight(1f),
                        label = { Text("目标") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Clay, unfocusedBorderColor = Line)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("单位") },
                        placeholder = { Text("杯/分钟/页") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Clay, unfocusedBorderColor = Line)
                    )
                }
            }
            SuccessButton(
                text = "添加",
                onClick = {
                    if (name.isNotBlank()) {
                        val target = targetText.toIntOrNull() ?: 1
                        onAdd(name, typeMap[typeLabel] ?: "check", target, unit)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
