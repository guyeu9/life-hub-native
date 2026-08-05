package com.lifehub.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifehub.LifeHubApplication
import com.lifehub.data.entity.ScheduleEntity
import com.lifehub.ui.components.EmptyState
import com.lifehub.ui.components.LifeCard
import com.lifehub.ui.theme.*
import com.lifehub.util.cnDateKey
import com.lifehub.viewmodel.PlanScope
import com.lifehub.viewmodel.ScheduleRow
import com.lifehub.viewmodel.ScheduleUiState
import com.lifehub.viewmodel.ScheduleViewModel
import com.lifehub.viewmodel.ScheduleViewModelFactory
import java.util.Calendar

@Composable
fun ScheduleScreen() {
    val app = LocalContext.current.applicationContext as LifeHubApplication
    val vm: ScheduleViewModel = viewModel(factory = ScheduleViewModelFactory(app))
    val state by vm.uiState.collectAsState()
    val scope by vm.scope.collectAsState()
    val rows = remember(state, scope) { vm.filtered(state) }

    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("P1") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        item { Text("日程统筹", style = MaterialTheme.typography.displayMedium, color = Ink) }
        item { ScheduleMetrics(state) }

        item {
            LifeCard {
                Text("添加日程", style = MaterialTheme.typography.titleMedium, color = Ink)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("要做什么") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Clay, unfocusedBorderColor = Line)
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = note, onValueChange = { note = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("备注（可选）") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Clay, unfocusedBorderColor = Line)
                    )
                    PrioritySelector(priority) { priority = it }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            vm.add(title.trim(), System.currentTimeMillis(), priority, note.trim())
                            title = ""; note = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Clay)
                ) { Text("+ 添加", color = PaperCard) }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("日程列表", style = MaterialTheme.typography.titleMedium, color = Ink)
                ScopeSegmented(scope) { vm.setScope(it) }
            }
        }

        if (rows.isEmpty()) {
            item { EmptyState("这个范围里没有日程") }
        } else {
            items(rows) { row -> ScheduleRowCard(row, onToggle = { vm.toggle(row.item) }, onDelete = { vm.delete(row.item) }) }
        }
    }
}

@Composable
private fun ScheduleMetrics(s: ScheduleUiState) {
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
        Metric("逾期未完成", s.overdueCount.toString(), "项", if (s.overdueCount > 0) "优先清掉" else "很干净", if (s.overdueCount > 0) Danger else Ink)
        Metric("今天安排", s.todayCount.toString(), "项", "已完成 ${s.todayDone}", Ink)
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
        Metric("本周待办", s.weekCount.toString(), "项", "本周内", Ink)
        Metric("累计完成", s.doneCount.toString(), "项", "全部历史", Sage)
    }
}

@Composable
private fun Metric(label: String, value: String, unit: String, desc: String, color: Color) {
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
private fun PrioritySelector(selected: String, onSelect: (String) -> Unit) {
    val pris = listOf("P0", "P1", "P2")
    val colors = mapOf("P0" to Danger, "P1" to Clay, "P2" to Sage)
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, Line, RoundedCornerShape(8.dp))
    ) {
        pris.forEach { p ->
            val on = p == selected
            Box(
                Modifier
                    .clickable { onSelect(p) }
                    .background(if (on) colors[p]!! else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(p, style = MaterialTheme.typography.labelMedium, color = if (on) PaperCard else InkSoft)
            }
        }
    }
}

@Composable
private fun ScopeSegmented(selected: PlanScope, onSelect: (PlanScope) -> Unit) {
    val opts = listOf(
        PlanScope.TODAY to "今天/逾期",
        PlanScope.WEEK to "本周",
        PlanScope.TODO to "全部未完成",
        PlanScope.DONE to "已完成"
    )
    Row(Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, Line, RoundedCornerShape(8.dp))) {
        opts.forEach { (s, label) ->
            val on = s == selected
            Box(
                Modifier
                    .clickable { onSelect(s) }
                    .background(if (on) Ink else Color.Transparent)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = if (on) PaperCard else InkSoft)
            }
        }
    }
}

@Composable
private fun ScheduleRowCard(row: ScheduleRow, onToggle: () -> Unit, onDelete: () -> Unit) {
    val item = row.item
    val priColor = when (item.priority) { "P0" -> Danger; "P1" -> Clay; else -> Sage }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = PaperCard
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.done,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = Sage)
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (row.overdue) Danger else Ink,
                    fontWeight = if (row.overdue) FontWeight.SemiBold else FontWeight.Normal
                )
                Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = priColor.copy(alpha = 0.15f)
                    ) {
                        Text(item.priority, style = MaterialTheme.typography.labelSmall, color = priColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                    }
                    if (item.note.isNotBlank()) Text(item.note, style = MaterialTheme.typography.labelSmall, color = InkSoft)
                    if (item.due > 0) {
                        Text(
                            if (row.overdue) "${cnDateKey(com.lifehub.util.dateKey(item.due))} · 逾期 ${row.overdueDays} 天"
                            else cnDateKey(com.lifehub.util.dateKey(item.due)),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (row.overdue) Danger else InkSoft
                        )
                    }
                }
            }
            Text("🗑", modifier = Modifier.clickable { onDelete() }.padding(4.dp), style = MaterialTheme.typography.labelMedium)
        }
    }
}
