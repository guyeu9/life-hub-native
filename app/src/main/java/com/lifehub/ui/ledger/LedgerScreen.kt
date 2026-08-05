package com.lifehub.ui.ledger

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
import com.lifehub.charts.DonutChart
import com.lifehub.charts.RingProgress
import com.lifehub.data.SettingsRepository
import com.lifehub.data.entity.LedgerEntity
import com.lifehub.ui.components.*
import com.lifehub.ui.theme.*
import com.lifehub.util.fullTime
import com.lifehub.util.money0
import com.lifehub.util.money2
import com.lifehub.viewmodel.LedgerSummary
import com.lifehub.viewmodel.LedgerViewModel
import com.lifehub.viewmodel.LedgerViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun LedgerScreen() {
    val app = LocalContext.current.applicationContext as LifeHubApplication
    val vm: LedgerViewModel = viewModel(factory = LedgerViewModelFactory(app))
    val all by vm.all.collectAsState()
    val summary by vm.summary.collectAsState()
    val fields by app.container.settings.fields.collectAsState(initial = SettingsRepository.FieldTable())
    val scope = rememberCoroutineScope()

    var showSheet by remember { mutableStateOf(false) }

    if (showSheet) {
        AddLedgerSheet(
            fields = fields,
            onDismiss = { showSheet = false },
            onSave = { typeCode, cat, amt, note ->
                vm.insert(typeCode, cat, amt, note)
                showSheet = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        item { Text("记账理财", style = MaterialTheme.typography.displayMedium, color = Ink) }
        item { SummaryCard(summary) }
        if (summary.byCategory.isNotEmpty()) {
            item { CategoryDonut(summary.byCategory) }
        }
        item { Text("明细", style = MaterialTheme.typography.titleMedium, color = Ink) }
        items(all) { item ->
            LedgerRow(item = item, color = categoryColor(fields, item), onDelete = { vm.delete(item) })
        }
        if (all.isEmpty()) {
            item { EmptyState("还没有记账，点下方按钮开始") }
        }
        item {
            Button(
                onClick = { showSheet = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Clay)
            ) {
                Text("+ 记一笔", color = PaperCard)
            }
        }
    }
}

@Composable
private fun SummaryCard(s: LedgerSummary) {
    LifeCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RingProgress(
                progress = if (s.budget > 0) (s.net / s.budget).toFloat().coerceIn(0f, 1f) else 0f,
                color = if (s.net > s.budget) Danger else Success,
                trackColor = Line,
                modifier = Modifier.size(84.dp),
                centerLabel = {
                    Text("${(if (s.budget > 0) (s.net / s.budget * 100).toInt() else 0)}%", style = MaterialTheme.typography.labelLarge, color = Ink)
                }
            )
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Line("收入", "+${money2(s.income)}", Sage)
                Line("支出", "-${money2(s.expense)}", Clay)
                Line("返利", "+${money2(s.rebate)}", Amber)
                Line("净支出", money2(s.net), if (s.net > s.budget) Danger else Ink)
                Line("月预算", money2(s.budget), InkSoft)
            }
        }
    }
}

@Composable
private fun Line(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = InkSoft)
        Text(value, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun CategoryDonut(byCategory: List<Pair<String, Double>>) {
    val palette = listOf(Clay, Sage, Amber, Slate, Danger, ClayLight, SageLight)
    LifeCard {
        Text("支出分类", style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            DonutChart(
                segments = byCategory.mapIndexed { i, pair -> pair.second.toFloat() to palette[i % palette.size] },
                trackColor = Line,
                modifier = Modifier.size(120.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                byCategory.take(5).forEachIndexed { i, (cat, amt) ->
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(palette[i % palette.size]))
                            Spacer(Modifier.width(6.dp))
                            Text(cat, style = MaterialTheme.typography.labelSmall, color = InkSoft)
                        }
                        Text(money0(amt), style = MaterialTheme.typography.labelSmall, color = Ink)
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerRow(item: LedgerEntity, color: Color, onDelete: () -> Unit) {
    val sign = when (item.type) { "income" -> "+"; "rebate" -> "+"; else -> "-" }
    val colorForSign = when (item.type) { "income" -> Sage; "rebate" -> Amber; else -> Clay }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PaperCard,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.category, style = MaterialTheme.typography.bodyMedium, color = Ink)
                if (item.note.isNotBlank()) {
                    Text(item.note, style = MaterialTheme.typography.labelSmall, color = InkSoft)
                }
            }
            Text(fullTime(item.date), style = MaterialTheme.typography.labelSmall, color = InkSoft)
            Spacer(Modifier.width(8.dp))
            Text("$sign${money2(item.amount)}", style = MaterialTheme.typography.labelLarge, color = colorForSign)
            Spacer(Modifier.width(8.dp))
            Text("🗑", modifier = Modifier.clickable { onDelete() }, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLedgerSheet(
    fields: SettingsRepository.FieldTable,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, String) -> Unit
) {
    var type by remember { mutableStateOf("支出") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val cats = when (type) { "支出" -> fields.expenseCats; "收入" -> fields.incomeCats; else -> fields.rebateCats }
    var selectedCat by remember { mutableStateOf(cats.firstOrNull()?.name ?: "") }
    LaunchedEffect(type) { selectedCat = cats.firstOrNull()?.name ?: "" }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("记一笔", style = MaterialTheme.typography.headlineMedium, color = Ink)
            SegmentedButton(listOf("支出", "收入", "返利"), type) { type = it }
            Text("分类", style = MaterialTheme.typography.labelMedium, color = InkSoft)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cats.forEach { cat ->
                    PillTag(cat.name, Color(AndroidColor.parseColor(cat.color)), selectedCat == cat.name) { selectedCat = cat.name }
                }
            }
            AmountStepper(amount) { amount = it }
            OutlinedTextField(
                value = note, onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("备注（可选）") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Clay, unfocusedBorderColor = Line)
            )
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && selectedCat.isNotBlank()) {
                        val code = when (type) { "支出" -> "expense"; "收入" -> "income"; else -> "rebate" }
                        onSave(code, selectedCat, amt, note)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Clay)
            ) { Text("保存", color = PaperCard) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun categoryColor(fields: SettingsRepository.FieldTable, item: LedgerEntity): Color {
    val list = when (item.type) {
        "income" -> fields.incomeCats
        "rebate" -> fields.rebateCats
        else -> fields.expenseCats
    }
    return list.firstOrNull { it.name == item.category }?.let {
        Color(AndroidColor.parseColor(it.color))
    } ?: Clay
}
