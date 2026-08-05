package com.lifehub.ui.ledger

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.lifehub.charts.DonutChart
import com.lifehub.charts.RingProgress
import com.lifehub.data.SettingsRepository
import com.lifehub.data.entity.LedgerEntity
import com.lifehub.ui.components.*
import com.lifehub.ui.theme.*
import com.lifehub.util.fullTime
import com.lifehub.util.money0
import com.lifehub.util.money2
import com.lifehub.util.vibrateLight
import com.lifehub.viewmodel.LedgerFilter
import com.lifehub.viewmodel.LedgerSummary
import com.lifehub.viewmodel.LedgerViewModel
import com.lifehub.viewmodel.LedgerViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun LedgerScreen() {
    val app = LocalContext.current.applicationContext as LifeHubApplication
    val vm: LedgerViewModel = viewModel(factory = LedgerViewModelFactory(app))
    val all by vm.all.collectAsState()
    val filtered by vm.filtered.collectAsState()
    val filter by vm.filter.collectAsState()
    val summary by vm.summary.collectAsState()
    val fields by app.container.settings.fields.collectAsState(initial = SettingsRepository.FieldTable())
    val scope = rememberCoroutineScope()

    var showSheet by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var confettiKey by remember { mutableIntStateOf(0) }

    if (showSheet) {
        AddLedgerSheet(
            fields = fields,
            onDismiss = { showSheet = false },
            onSave = { typeCode, cat, amt, note, rebateOf, date ->
                vm.insert(typeCode, cat, amt, note, rebateOf, date)
                confettiKey++
                showSheet = false
            }
        )
    }

    if (showBudgetDialog) {
        BudgetEditDialog(
            currentBudget = summary.budget,
            currentNetRebate = summary.netRebate,
            onDismiss = { showBudgetDialog = false },
            onConfirm = { budget, netRebate ->
                scope.launch {
                    app.container.settings.setBudget(budget)
                    app.container.settings.setNetRebate(netRebate)
                }
                confettiKey++
                showBudgetDialog = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
        ) {
            item { AnimatedHeader("记账理财") }
            item { SummaryCard(summary, onEditBudget = { showBudgetDialog = true }) }
            item {
                FilterBar(
                    all = all,
                    filter = filter,
                    onFilterChange = { vm.setFilter(it) }
                )
            }
            if (summary.byCategory.isNotEmpty()) {
                item { CategoryDonut(summary.byCategory) }
            }
            item { Text("明细", style = MaterialTheme.typography.titleMedium, color = Ink) }
            itemsIndexed(filtered) { index, item ->
                LedgerRow(
                    item = item,
                    color = categoryColor(fields, item),
                    onDelete = {
                        vm.delete(item)
                        confettiKey++
                    },
                    modifier = Modifier.animateItemSlide(index)
                )
            }
            if (filtered.isEmpty()) {
                item {
                    EmptyState(if (all.isEmpty()) "还没有记账，点下方按钮开始" else "无匹配明细")
                }
            }
            item {
                SuccessButton(
                    text = "+ 记一笔",
                    onClick = { showSheet = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        ConfettiOverlay(trigger = confettiKey)
    }
}

@Composable
private fun SummaryCard(s: LedgerSummary, onEditBudget: () -> Unit) {
    LifeCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RingProgress(
                progress = if (s.budget > 0) (s.net / s.budget).toFloat().coerceIn(0f, 1f) else 0f,
                color = if (s.net > s.budget) Danger else Success,
                trackColor = Line,
                modifier = Modifier.size(84.dp),
                centerLabel = {
                    Text(
                        "${(if (s.budget > 0) (s.net / s.budget * 100).toInt() else 0)}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = Ink
                    )
                }
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Line("收入", "+${money2(s.income)}", Sage)
                Line("支出", "-${money2(s.expense)}", Clay)
                Line("返利", "+${money2(s.rebate)}", Amber)
                Line("净支出", money2(s.net), if (s.net > s.budget) Danger else Ink)
                Line("月预算", money2(s.budget), InkSoft)
            }
            Text(
                "调整预算",
                modifier = Modifier.hapticClick { onEditBudget() },
                color = Clay,
                style = MaterialTheme.typography.labelSmall
            )
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
    val total = byCategory.sumOf { it.second }.coerceAtLeast(0.0001)
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
                    val pct = (amt / total * 100).toInt()
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(palette[i % palette.size])
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(cat, style = MaterialTheme.typography.labelSmall, color = InkSoft)
                        }
                        Text("${money0(amt)} · ${pct}%", style = MaterialTheme.typography.labelSmall, color = Ink)
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerRow(item: LedgerEntity, color: Color, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val sign = when (item.type) { "income" -> "+"; "rebate" -> "+"; else -> "-" }
    val colorForSign = when (item.type) { "income" -> Sage; "rebate" -> Amber; else -> Clay }
    Surface(
        modifier = modifier.fillMaxWidth(),
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
                if (item.type == "rebate" && item.rebateOf > 0) {
                    val rate = (item.amount / item.rebateOf * 100).toInt().coerceIn(0, 100)
                    Text(
                        "对应消费 ${money2(item.rebateOf)} · 返 ${rate}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Amber
                    )
                } else if (item.note.isNotBlank()) {
                    Text(item.note, style = MaterialTheme.typography.labelSmall, color = InkSoft)
                }
            }
            Text(fullTime(item.date), style = MaterialTheme.typography.labelSmall, color = InkSoft)
            Spacer(Modifier.width(8.dp))
            Text("$sign${money2(item.amount)}", style = MaterialTheme.typography.labelLarge, color = colorForSign)
            Spacer(Modifier.width(8.dp))
            Text(
                "删除",
                modifier = Modifier.hapticClick { onDelete() },
                style = MaterialTheme.typography.labelMedium,
                color = Danger
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLedgerSheet(
    fields: SettingsRepository.FieldTable,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, String, Double, Long) -> Unit
) {
    var type by remember { mutableStateOf("支出") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var rebateOf by remember { mutableStateOf("") }
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.CHINA) }
    var dateText by remember { mutableStateOf(dateFmt.format(Date())) }
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

            OutlinedTextField(
                value = dateText,
                onValueChange = { dateText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("日期 (yyyy-MM-dd)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Clay, unfocusedBorderColor = Line)
            )

            Text("分类", style = MaterialTheme.typography.labelMedium, color = InkSoft)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cats.forEach { cat ->
                    PillTag(
                        cat.name,
                        Color(AndroidColor.parseColor(cat.color)),
                        selectedCat == cat.name,
                        modifier = Modifier.pressScale { selectedCat = cat.name }
                    ) { selectedCat = cat.name }
                }
            }
            AmountStepper(amount) { amount = it }

            if (type == "返利") {
                OutlinedTextField(
                    value = rebateOf,
                    onValueChange = { rebateOf = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("对应消费金额（可选）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Clay, unfocusedBorderColor = Line)
                )
            }

            OutlinedTextField(
                value = note, onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("备注（可选）") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Clay, unfocusedBorderColor = Line)
            )
            SuccessButton(
                text = "保存",
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    val rebOf = rebateOf.toDoubleOrNull() ?: 0.0
                    val date = runCatching {
                        dateFmt.parse(dateText)?.time ?: System.currentTimeMillis()
                    }.getOrDefault(System.currentTimeMillis())
                    if (amt > 0 && selectedCat.isNotBlank()) {
                        val code = when (type) { "支出" -> "expense"; "收入" -> "income"; else -> "rebate" }
                        onSave(code, selectedCat, amt, note, rebOf, date)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BudgetEditDialog(
    currentBudget: Double,
    currentNetRebate: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double, Boolean) -> Unit
) {
    var budgetText by remember { mutableStateOf(currentBudget.toString()) }
    var netRebate by remember { mutableStateOf(currentNetRebate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("调整预算", color = Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("月预算金额") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Clay, unfocusedBorderColor = Line)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("返利冲抵支出", color = InkSoft, style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = netRebate, onCheckedChange = { netRebate = it })
                }
            }
        },
        confirmButton = {
            SuccessButton(
                text = "保存",
                onClick = {
                    val b = budgetText.toDoubleOrNull() ?: 0.0
                    onConfirm(b, netRebate)
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = InkSoft) }
        }
    )
}

@Composable
private fun FilterBar(
    all: List<LedgerEntity>,
    filter: LedgerFilter,
    onFilterChange: (LedgerFilter) -> Unit
) {
    val months = remember(all) {
        all.map { monthKeyOf(it.date) }.distinct().sortedDescending()
    }
    val categories = remember(all) {
        all.map { it.category }.distinct().sorted()
    }
    val typeOptions = listOf(
        "" to "全部类型",
        "expense" to "支出",
        "income" to "收入",
        "rebate" to "返利"
    )

    LifeCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterDropdown(
                    modifier = Modifier.weight(1f),
                    label = "月份",
                    options = listOf("" to "全部月份") + months.map { it to it },
                    selected = filter.month,
                    onSelect = { onFilterChange(filter.copy(month = it)) }
                )
                FilterDropdown(
                    modifier = Modifier.weight(1f),
                    label = "类型",
                    options = typeOptions,
                    selected = filter.type,
                    onSelect = { onFilterChange(filter.copy(type = it)) }
                )
            }
            FilterDropdown(
                modifier = Modifier.fillMaxWidth(),
                label = "分类",
                options = listOf("" to "全部分类") + categories.map { it to it },
                selected = filter.category,
                onSelect = { onFilterChange(filter.copy(category = it)) }
            )
            OutlinedTextField(
                value = filter.search,
                onValueChange = { onFilterChange(filter.copy(search = it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索备注或分类") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Clay, unfocusedBorderColor = Line)
            )
        }
    }
}

@Composable
private fun FilterDropdown(
    modifier: Modifier = Modifier,
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: label
    val ctx = LocalContext.current
    Box(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            shape = RoundedCornerShape(4.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Line),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = InkSoft)
                    Text(selectedLabel, style = MaterialTheme.typography.bodyMedium, color = Ink)
                }
                Text("v", color = InkSoft, style = MaterialTheme.typography.labelSmall)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { (value, lbl) ->
                DropdownMenuItem(
                    text = { Text(lbl) },
                    onClick = {
                        ctx.vibrateLight()
                        onSelect(value)
                        expanded = false
                    }
                )
            }
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

/** 把时间戳转为 "yyyy-MM" 月份键 */
private fun monthKeyOf(ts: Long): String {
    val cal = Calendar.getInstance()
    cal.timeInMillis = ts
    return "%04d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
}
