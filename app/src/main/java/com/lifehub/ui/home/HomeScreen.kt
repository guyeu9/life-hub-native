package com.lifehub.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifehub.LifeHubApplication
import com.lifehub.charts.RingProgress
import com.lifehub.data.SettingsRepository
import com.lifehub.ui.components.*
import com.lifehub.ui.theme.*
import com.lifehub.util.money0
import com.lifehub.util.yuan
import com.lifehub.viewmodel.HomeItem
import com.lifehub.viewmodel.HomeUiState
import com.lifehub.viewmodel.HomeViewModel
import com.lifehub.viewmodel.HomeViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as LifeHubApplication
    val vm: HomeViewModel = viewModel(factory = HomeViewModelFactory(app))
    val state by vm.uiState.collectAsState()
    val fields by app.container.settings.fields.collectAsState(initial = SettingsRepository.FieldTable())
    val scope = rememberCoroutineScope()

    // 随手记一笔 UI 状态
    var qlType by remember { mutableStateOf("支出") }
    var qlAmount by remember { mutableStateOf("") }
    var qlNote by remember { mutableStateOf("") }
    var qlRebateOf by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        item {
            // 顶部：标题 + 备份/恢复
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("今", style = MaterialTheme.typography.displayMedium, color = Ink)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onExport) { Text("备份") }
                    OutlinedButton(onClick = onImport) { Text("恢复") }
                }
            }
        }

        item { LifeIndexCard(state, vm) }

        item {
            Text("今天要处理", style = MaterialTheme.typography.titleMedium, color = Ink)
        }

        items(state.todayItems) { item ->
            TodayRow(item, onClick = {
                when (item.type) {
                    "schedule" -> onNavigate("schedule")
                    "habit" -> onNavigate("habit")
                }
            })
        }
        if (state.todayItems.isEmpty()) {
            item { EmptyState("今天没有待处理的事项，真清爽") }
        }

        item {
            Text("随手记一笔", style = MaterialTheme.typography.titleMedium, color = Ink)
        }

        item {
            QuickLedgerCard(
                type = qlType,
                onTypeChange = { qlType = it },
                amount = qlAmount,
                onAmountChange = { qlAmount = it },
                note = qlNote,
                onNoteChange = { qlNote = it },
                rebateOf = qlRebateOf,
                onRebateOfChange = { qlRebateOf = it },
                categories = when (qlType) {
                    "支出" -> fields.expenseCats
                    "收入" -> fields.incomeCats
                    else -> fields.rebateCats
                },
                onSave = { cat ->
                    val typeCode = when (qlType) { "支出" -> "expense"; "收入" -> "income"; else -> "rebate" }
                    val amt = qlAmount.toDoubleOrNull() ?: 0.0
                    val rebOf = qlRebateOf.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        scope.launch {
                            app.container.ledger.insert(
                                com.lifehub.data.entity.LedgerEntity(
                                    type = typeCode, category = cat, amount = amt, note = qlNote,
                                    rebateOf = rebOf,
                                    date = System.currentTimeMillis()
                                )
                            )
                            qlAmount = ""
                            qlNote = ""
                            qlRebateOf = ""
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun LifeIndexCard(state: HomeUiState, vm: HomeViewModel) {
    val todoPct = vm.todoPct(state)
    val habitPct = vm.habitPct(state)
    // 综合生活指数 = 待办完成率×30% + 习惯完成率×30% + 今日记账(10) + 预算健康度(10) + 身体数据(20)
    val overall = vm.overallPct(state) / 100f

    LifeCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RingProgress(
                progress = overall,
                color = Clay,
                trackColor = Line,
                stroke = 14f,
                modifier = Modifier.size(110.dp),
                centerLabel = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${((overall * 100).toInt())}%", style = MaterialTheme.typography.titleLarge, color = Ink)
                        Text("今日指数", style = MaterialTheme.typography.labelSmall, color = InkSoft)
                    }
                }
            )
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DimRow("待办", if (state.todoTotal == 0) "未设置" else "$todoPct%", todoPct / 100f, if (state.todoTotal == 0) Line else Clay)
                DimRow("习惯", if (state.habitTotal == 0) "未设置" else "$habitPct%", habitPct / 100f, if (state.habitTotal == 0) Line else Sage)
                DimMoney(
                    "记账",
                    if (state.ledgerCount == 0) "未记账" else "收${money0(state.ledgerIncome)}·支${money0(state.ledgerExpense)}·记${state.ledgerCount}笔"
                )
                DimMoney(
                    "身体",
                    if (state.todayWeight != null) "今 ${state.todayWeight}kg" + (if (state.todayBodyFat != null) " · ${state.todayBodyFat}%脂" else "") else "未称重"
                )
            }
        }
    }
}

@Composable
private fun DimRow(label: String, value: String, progress: Float, color: Color) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = InkSoft)
            Text(value, style = MaterialTheme.typography.labelMedium, color = Ink)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
            color = color,
            trackColor = Line
        )
    }
}

@Composable
private fun DimMoney(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = InkSoft)
        Text(value, style = MaterialTheme.typography.labelMedium, color = Ink)
    }
}

@Composable
private fun TodayRow(item: HomeItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape)
                    .background(if (item.overdue) Danger else Clay)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyMedium, color = Ink)
                Text(item.sub, style = MaterialTheme.typography.labelSmall, color = if (item.overdue) Danger else InkSoft)
            }
        }
    }
}

@Composable
private fun QuickLedgerCard(
    type: String,
    onTypeChange: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    rebateOf: String,
    onRebateOfChange: (String) -> Unit,
    categories: List<SettingsRepository.CategoryDef>,
    onSave: (String) -> Unit
) {
    var selectedCat by remember { mutableStateOf(categories.firstOrNull()?.name ?: "") }

    // 类型切换时重置选中分类
    LaunchedEffect(type) {
        selectedCat = categories.firstOrNull()?.name ?: ""
    }

    LifeCard {
        SegmentedButton(
            options = listOf("支出", "收入", "返利"),
            selected = type,
            onSelect = onTypeChange
        )
        Spacer(Modifier.height(12.dp))
        // 分类胶囊
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { cat ->
                PillTag(
                    text = cat.name,
                    color = Color(android.graphics.Color.parseColor(cat.color)),
                    selected = selectedCat == cat.name,
                    onClick = { selectedCat = cat.name }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        AmountStepper(value = amount, onValueChange = onAmountChange)
        // 返利类型：显示"对应消费"输入框
        if (type == "返利") {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = rebateOf,
                onValueChange = onRebateOfChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("对应消费金额（可选）") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Clay,
                    unfocusedBorderColor = Line
                )
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("备注（可选）") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Clay,
                unfocusedBorderColor = Line
            )
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { onSave(selectedCat) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Clay)
        ) {
            Text("记一笔", color = PaperCard)
        }
    }
}
