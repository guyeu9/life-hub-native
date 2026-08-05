package com.lifehub.ui.habit

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifehub.LifeHubApplication
import com.lifehub.charts.HeatmapChart
import com.lifehub.ui.components.*
import com.lifehub.ui.theme.*
import com.lifehub.viewmodel.HabitView
import com.lifehub.viewmodel.HabitViewModel
import com.lifehub.viewmodel.HabitViewModelFactory

@Composable
fun HabitScreen() {
    val app = LocalContext.current.applicationContext as LifeHubApplication
    val vm: HabitViewModel = viewModel(factory = HabitViewModelFactory(app))
    val habits by vm.habits.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    if (showAdd) {
        AddHabitSheet(
            onDismiss = { showAdd = false },
            onAdd = { name, color -> vm.addHabit(name, color); showAdd = false }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("习惯健康", style = MaterialTheme.typography.displayMedium, color = Ink)
                OutlinedButton(onClick = { showAdd = true }) { Text("+ 习惯") }
            }
        }
        if (habits.isEmpty()) {
            item { EmptyState("还没有习惯，点右上角添加一个") }
        }
        items(habits) { hv ->
            HabitCard(hv, onToggle = { vm.toggleToday(hv.habit.id) }, onDelete = { vm.deleteHabit(hv.habit) })
        }
    }
}

@Composable
private fun HabitCard(hv: HabitView, onToggle: () -> Unit, onDelete: () -> Unit) {
    val color = Color(AndroidColor.parseColor(hv.habit.color))
    LifeCard {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(hv.habit.name, style = MaterialTheme.typography.titleMedium, color = Ink)
                Text("连续 ${hv.streak} 天", style = MaterialTheme.typography.labelSmall, color = if (hv.streak > 0) Sage else InkSoft)
            }
            Checkbox(
                checked = hv.doneToday,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = color)
            )
            Text("🗑", modifier = Modifier.clickable { onDelete() }.padding(start = 8.dp), style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(10.dp))
        HeatmapChart(states = hv.monthDays, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddHabitSheet(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val palette = listOf("#5D7561", "#A2543C", "#C8893B", "#647D8E", "#B85450")
    var color by remember { mutableStateOf(palette[0]) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("新习惯", style = MaterialTheme.typography.headlineMedium, color = Ink)
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("习惯名称，如：喝水/阅读") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Clay, unfocusedBorderColor = Line)
            )
            Text("颜色", style = MaterialTheme.typography.labelMedium, color = InkSoft)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                palette.forEach { c ->
                    Box(
                        Modifier.size(32.dp).clip(RoundedCornerShape(16.dp))
                            .background(Color(AndroidColor.parseColor(c)))
                            .clickable { color = c }
                    )
                }
            }
            Button(
                onClick = { if (name.isNotBlank()) onAdd(name, color) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Clay)
            ) { Text("添加", color = PaperCard) }
            Spacer(Modifier.height(8.dp))
        }
    }
}
