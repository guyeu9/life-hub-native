package com.lifehub.ui.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.layout.Layout
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifehub.LifeHubApplication
import com.lifehub.charts.RingProgress
import com.lifehub.data.SettingsRepository
import com.lifehub.data.entity.LedgerEntity
import com.lifehub.ui.components.*
import com.lifehub.ui.theme.*
import com.lifehub.util.money0
import com.lifehub.util.money2
import com.lifehub.util.vibrateLight
import com.lifehub.util.vibrateSuccess
import com.lifehub.viewmodel.HomeItem
import com.lifehub.ui.settings.QuickAmountsDialog
import com.lifehub.viewmodel.HomeUiState
import com.lifehub.viewmodel.HomeViewModel
import com.lifehub.viewmodel.HomeViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as LifeHubApplication
    val vm: HomeViewModel = viewModel(factory = HomeViewModelFactory(app))
    val state by vm.uiState.collectAsState()
    val fields by app.container.settings.fields.collectAsState(initial = SettingsRepository.FieldTable())
    val quickAmounts by app.container.settings.quickAmounts.collectAsState(initial = SettingsRepository.QuickAmounts())
    val scope = rememberCoroutineScope()

    // 随手记一笔 UI 状态
    var qlType by remember { mutableStateOf("支出") }
    var qlAmount by remember { mutableStateOf("") }
    var qlNote by remember { mutableStateOf("") }
    var qlRebateOf by remember { mutableStateOf("") }
    var showQuickAmts by remember { mutableStateOf(false) }
    var confettiKey by remember { mutableIntStateOf(0) }

    val typeCode = when (qlType) { "支出" -> "expense"; "收入" -> "income"; else -> "rebate" }
    val typeColor = when (qlType) {
        "支出" -> Color(0xFFB0433A)
        "收入" -> Color(0xFF4C7554)
        else -> Color(0xFFA8842F)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 18.dp, bottom = 28.dp)
        ) {
            item { LifeIndexCard(state, vm) }

            // 随手记一笔（原网页在生活指数之后、今天要处理之前）
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "随手记一笔",
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink
                    )
                    Text(
                        text = "点类型 · 点分类 · 记完自动存",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSoft
                    )
                }
            }
            item {
                QuickLedgerCard(
                    type = qlType,
                    typeColor = typeColor,
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
                    quickAmounts = when (qlType) {
                        "支出" -> quickAmounts.expense
                        "收入" -> quickAmounts.income
                        else -> quickAmounts.rebate
                    },
                    onQuickAmount = { qlAmount = it },
                    onEditQuickAmounts = { showQuickAmts = true },
                    onSave = { cat ->
                        val amt = qlAmount.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            scope.launch {
                                app.container.ledger.insert(
                                    LedgerEntity(
                                        type = typeCode,
                                        category = cat,
                                        amount = amt,
                                        note = qlNote,
                                        rebateOf = qlRebateOf.toDoubleOrNull() ?: 0.0,
                                        date = System.currentTimeMillis()
                                    )
                                )
                                qlAmount = ""
                                qlNote = ""
                                qlRebateOf = ""
                                confettiKey++
                                Toast.makeText(app, "记好了", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            // 今天要处理（带卡片边框，对齐网页 .today-block）
            item {
                LifeCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Clay,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "今天要处理",
                                style = MaterialTheme.typography.titleMedium,
                                color = Ink
                            )
                        }
                        Text(
                            text = buildString {
                                append("${state.todayItems.size} 项")
                                if (state.overdueCount > 0) append(" · 含 ${state.overdueCount} 项逾期")
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = InkSoft
                        )
                    }

                    if (state.todayItems.isEmpty()) {
                        EmptyState("今天该做的都做完了，去休息吧。", icon = Icons.Default.Check)
                    } else {
                        state.todayItems.forEachIndexed { index, item ->
                            TodayRow(
                                item = item,
                                index = index,
                                onAction = {
                                    scope.launch {
                                        when (item.type) {
                                            "schedule" -> vm.completeSchedule(item.id)
                                            "habit" -> vm.quickHabit(item)
                                            "wish" -> vm.buyDone(item.id)
                                            "ledger_tip" -> onNavigate("ledger")
                                            "fitness_tip" -> onNavigate("fitness")
                                        }
                                    }
                                },
                                onNavigate = onNavigate
                            )
                        }
                    }
                }
            }

            // 今日速览
            item {
                Text(
                    text = "今日速览",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink
                )
            }
            item { TodayMetrics(state) }
        }
        ConfettiOverlay(trigger = confettiKey)
    }

    if (showQuickAmts) {
        QuickAmountsDialog(
            current = quickAmounts,
            onDismiss = { showQuickAmts = false },
            onSave = { q ->
                context.vibrateSuccess()
                scope.launch { app.container.settings.setQuickAmounts(q) }
                showQuickAmts = false
            }
        )
    }
}

@Composable
private fun LifeIndexCard(state: HomeUiState, vm: HomeViewModel) {
    val todoPct = vm.todoPct(state)
    val habitPct = vm.habitPct(state)
    val overall = vm.overallPct(state)
    val progress = overall / 100f

    val levelText = when {
        overall >= 85 -> "从容"
        overall >= 70 -> "在线"
        overall >= 50 -> "散漫"
        else -> "待启动"
    }
    val sayText = when {
        overall >= 85 -> "状态很好。保持这个节奏，不用额外加码。"
        overall >= 70 -> "整体在线。挑一两件最要紧的收个尾，今天就算赢。"
        overall >= 50 -> "有点松。先把标红的处理掉，剩下的慢慢来。"
        else -> "今天还没启动。从最上面那条开始，做完一件就好很多。"
    }
    val ringColor = when {
        overall >= 80 -> Color(0xFF5D7561)
        overall >= 60 -> Color(0xFFA8842F)
        else -> Color(0xFFB0433A)
    }

    LifeCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RingProgress(
                progress = progress,
                color = ringColor,
                trackColor = Color(0xFFE9E3D6),
                stroke = 14f,
                modifier = Modifier.size(110.dp),
                centerLabel = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$overall", style = MaterialTheme.typography.headlineLarge, color = Ink)
                        Text("INDEX", style = MaterialTheme.typography.labelSmall, color = InkSoft, letterSpacing = 2.sp)
                    }
                }
            )
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = levelText,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Ink,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = sayText,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                )
                Spacer(Modifier.height(4.dp))
                DimRow(
                    label = "待办",
                    value = if (state.todoTotal == 0) "未设置" else "$todoPct%",
                    progress = if (state.todoTotal == 0) 0f else todoPct / 100f,
                    color = if (state.todoTotal == 0) Line else Color(0xFF4A6478),
                    configured = state.todoTotal > 0
                )
                DimRow(
                    label = "习惯",
                    value = if (state.habitTotal == 0) "未设置" else "$habitPct%",
                    progress = if (state.habitTotal == 0) 0f else habitPct / 100f,
                    color = if (state.habitTotal == 0) Line else Color(0xFF5D7561),
                    configured = state.habitTotal > 0
                )
                DimMoney(
                    label = "记账",
                    value = if (!state.ledgerConfigured) "未设置"
                    else "收 ¥${money2(state.ledgerIncome)} · 支 ¥${money2(state.ledgerExpense)} · 记 ${state.ledgerCount} 笔 · 今日净支出 ¥${money2(state.ledgerNet)}",
                    configured = state.ledgerConfigured
                )
                DimMoney(
                    label = "身体",
                    value = if (state.lastWeight != null) {
                        if (state.weighedToday) {
                            "今日 ${state.todayWeight?.let { if (it % 1.0 == 0.0) it.toInt().toString() else "%.1f".format(it) } ?: "—"}kg" + (state.todayBodyFat?.let { " · ${kotlin.math.round(it).toInt()}% 脂" } ?: "")
                        } else "今日未称重"
                    } else "未设置",
                    configured = state.lastWeight != null
                )
            }
        }
    }
}

@Composable
private fun DimRow(label: String, value: String, progress: Float, color: Color, configured: Boolean = true) {
    Column(Modifier.fillMaxWidth().then(if (configured) Modifier else Modifier.graphicsLayer { alpha = 0.45f })) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = InkSoft)
            Text(value, style = MaterialTheme.typography.labelMedium, color = Ink)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = Line
        )
    }
}

@Composable
private fun DimMoney(label: String, value: String, configured: Boolean = true) {
    Row(Modifier.fillMaxWidth().then(if (configured) Modifier else Modifier.graphicsLayer { alpha = 0.45f }), Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = InkSoft)
        Text(value, style = MaterialTheme.typography.labelMedium, color = Ink)
    }
}

@Composable
private fun TodayRow(
    item: HomeItem,
    index: Int,
    onAction: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val ctx = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateItemSlide(index)
            .hapticClick {
                if (item.type == "ledger_tip") onNavigate("ledger")
                else if (item.type == "fitness_tip") onNavigate("fitness")
            },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(8.dp).clip(CircleShape)
                    .background(if (item.overdue) Danger else Color(0xFFB8B2A6))
            )
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.overdue) Danger else Ink,
                    fontWeight = if (item.overdue) androidx.compose.ui.text.font.FontWeight.SemiBold else null
                )
                Text(item.sub, style = MaterialTheme.typography.labelSmall, color = if (item.overdue) Danger else InkSoft)
            }
            if (item.action.isNotBlank()) {
                SmallButton(
                    text = item.action,
                    highlighted = item.overdue,
                    onClick = {
                        ctx.vibrateSuccess()
                        onAction()
                    }
                )
            }
        }
    }
}

@Composable
private fun SmallButton(text: String, highlighted: Boolean, onClick: () -> Unit) {
    val bg = if (highlighted) Clay else PaperCard
    val txt = if (highlighted) PaperCard else Ink
    val borderColor = if (highlighted) Clay else Color(0xFFD8D2C6)
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .pressScale(scale = 0.94f, onClick = onClick),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = text,
            color = txt,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun QuickLedgerCard(
    type: String,
    typeColor: Color,
    onTypeChange: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    rebateOf: String,
    onRebateOfChange: (String) -> Unit,
    categories: List<SettingsRepository.CategoryDef>,
    quickAmounts: List<Double>,
    onQuickAmount: (String) -> Unit,
    onEditQuickAmounts: () -> Unit,
    onSave: (String) -> Unit
) {
    val ctx = LocalContext.current
    var selectedCat by remember { mutableStateOf("") }

    LaunchedEffect(type) {
        selectedCat = ""
    }

    LifeCard(borderColor = typeColor) {
        // 类型选择（彩色胶囊，对齐网页）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf("支出" to Color(0xFFB0433A), "收入" to Color(0xFF4C7554), "返利" to Color(0xFFA8842F))
                .forEach { (label, color) ->
                    val selected = type == label
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .pressScale(scale = 0.97f) {
                                ctx.vibrateLight()
                                onTypeChange(label)
                            },
                        color = if (selected) color else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, color)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(9.dp).clip(CircleShape)
                                    .background(if (selected) PaperCard else color)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                label,
                                color = if (selected) PaperCard else color,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
        }

        Spacer(Modifier.height(14.dp))

        // 金额输入
        AmountStepper(value = amount, onValueChange = onAmountChange, accentColor = typeColor)

        Spacer(Modifier.height(12.dp))

        // 常用金额
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("常用金额", style = MaterialTheme.typography.labelMedium, color = InkSoft)
            TextButton(onClick = onEditQuickAmounts) {
                Text("自定义", style = MaterialTheme.typography.labelMedium, color = Clay)
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickAmounts.forEach { amt ->
                QuickAmountChip(
                    amount = amt,
                    color = typeColor,
                    onClick = {
                        ctx.vibrateLight()
                        onQuickAmount(amt.toString())
                    }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // 分类
        Text("分类", style = MaterialTheme.typography.labelMedium, color = InkSoft)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                val catColor = try {
                    Color(android.graphics.Color.parseColor(cat.color))
                } catch (_: Exception) {
                    Clay
                }
                val selected = selectedCat == cat.name
                val catBorder = if (selected) catColor else Color(0xFFD8D2C6)
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .pressScale(scale = 0.96f) {
                            ctx.vibrateLight()
                            selectedCat = cat.name
                        },
                    color = PaperCard,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, catBorder)
                ) {
                    Text(
                        cat.name,
                        color = if (selected) catColor else InkSoft,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // 返利：对应消费
        if (type == "返利") {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = rebateOf,
                onValueChange = onRebateOfChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("对应消费（原价，选填，用于算返利率）") },
                placeholder = { Text("如 34.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = typeColor,
                    unfocusedBorderColor = Color(0xFFD8D2C6)
                )
            )
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("备注（选填）") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = typeColor,
                unfocusedBorderColor = Color(0xFFD8D2C6)
            )
        )
        Spacer(Modifier.height(14.dp))
        SuccessButton(
            text = "记下这笔${type}",
            onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                if (amt <= 0) {
                    Toast.makeText(ctx, "先填个金额", Toast.LENGTH_SHORT).show()
                    return@SuccessButton
                }
                if (selectedCat.isBlank()) {
                    Toast.makeText(ctx, "先点选一个分类", Toast.LENGTH_SHORT).show()
                    return@SuccessButton
                }
                onSave(selectedCat)
                selectedCat = ""
            },
            modifier = Modifier.fillMaxWidth(),
            containerColor = typeColor
        )
    }
}

@Composable
private fun QuickAmountChip(amount: Double, color: Color, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .pressScale(scale = 0.94f) {
                ctx.vibrateLight()
                onClick()
            },
        color = PaperCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD8D2C6))
    ) {
        Text(
            text = amount.toString(),
            color = Ink,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun TodayMetrics(state: HomeUiState) {
    val todayExp = state.ledgerExpense
    val todayReb = state.ledgerRebate
    val left = state.budget - state.budgetUsed
    val metrics = listOf(
        MetricData("今日支出", money0(todayExp), "元", if (todayExp > 0) "已记 ${state.ledgerCount} 笔" else "还没记账", Danger),
        MetricData("今日返利", money2(todayReb), "元", if (todayExp > 0) "相当于省了 ${if (todayExp > 0) (todayReb / todayExp * 100).toInt() else 0}%" else "吃饭记得领返利", Gold),
        MetricData("本月预算剩余", money0(left), "元", "预算 ${money0(state.budget)}${if (state.netRebate) " · 已抵返利" else ""}", if (left < 0) Danger else Sage),
        MetricData("最长连续打卡", state.maxStreak.toString(), "天", "${state.habitTotal} 个习惯在跟", Clay),
        MetricData("最近体重", state.lastWeight?.let { if (it % 1.0 == 0.0) it.toInt().toString() else "%.1f".format(it) } ?: "—", "kg", state.lastWeightDate.let { if (it.isNotBlank()) "$it 记录" else "还没有数据" }, Ink)
    )

    // 网页 .metrics：带边框的卡片组，移动端两列（最后一项占满）
    val cardModifier = Modifier.fillMaxWidth()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PaperCard)
            .border(0.5.dp, Line)
    ) {
        metrics.chunked(2).forEachIndexed { rowIndex, pair ->
            Row(modifier = Modifier.fillMaxWidth()) {
                if (pair.size > 1) {
                    MetricCard(
                        metric = pair[0],
                        modifier = cardModifier.weight(1f)
                    )
                    VerticalDivider(thickness = 0.5.dp, color = Line)
                    MetricCard(
                        metric = pair[1],
                        modifier = cardModifier.weight(1f)
                    )
                } else {
                    MetricCard(
                        metric = pair[0],
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (rowIndex < metrics.size / 2) {
                HorizontalDivider(thickness = 0.5.dp, color = Line)
            }
        }
    }
}

private data class MetricData(
    val label: String,
    val value: String,
    val unit: String,
    val desc: String,
    val valueColor: Color
)

@Composable
private fun MetricCard(metric: MetricData, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PaperCard)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            metric.label,
            style = MaterialTheme.typography.labelSmall,
            color = InkSoft,
            letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                metric.value,
                style = MaterialTheme.typography.headlineMedium,
                color = metric.valueColor,
                fontWeight = FontWeight.SemiBold,
                lineHeight = MaterialTheme.typography.headlineMedium.lineHeight
            )
            Spacer(Modifier.width(3.dp))
            Text(
                metric.unit,
                style = MaterialTheme.typography.labelMedium,
                color = InkSoft,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            metric.desc,
            style = MaterialTheme.typography.labelSmall,
            color = InkSoft
        )
    }
}

@Composable
private fun AmountStepper(
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color = Clay,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier
                .size(width = 46.dp, height = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .pressScale(scale = 0.92f) {
                    val cur = value.toDoubleOrNull()?.toInt() ?: 0
                    onValueChange(maxOf(0, cur - 1).toString())
                },
            color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Line)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("−", style = MaterialTheme.typography.headlineMedium, color = accentColor)
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = { raw ->
                // 仅保留数字和单个小数点，去掉前导0和多余小数点
                val cleaned = raw.filter { c -> c.isDigit() || c == '.' }.let { s ->
                    val firstDot = s.indexOf('.')
                    if (firstDot < 0) s
                    else s.substring(0, firstDot + 1) + s.substring(firstDot + 1).filter { it != '.' }
                }
                onValueChange(if (cleaned.isEmpty() || cleaned == ".") cleaned else cleaned.toDoubleOrNull()?.toString() ?: value)
            },
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center),
            prefix = { Text("¥", color = InkSoft) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Line
            )
        )
        Surface(
            modifier = Modifier
                .size(width = 46.dp, height = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .pressScale(scale = 0.92f) {
                    val cur = value.toDoubleOrNull()?.toInt() ?: 0
                    onValueChange((cur + 1).toString())
                },
            color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Line)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("+", style = MaterialTheme.typography.headlineMedium, color = accentColor)
            }
        }
    }
}

@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val hGapPx = 8.dp.roundToPx()
        val vGapPx = 8.dp.roundToPx()
        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        val rowWidths = mutableListOf<Int>()
        val rowHeights = mutableListOf<Int>()

        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentWidth = 0
        var currentHeight = 0

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0))
            if (currentRow.isNotEmpty() && currentWidth + hGapPx + placeable.width > constraints.maxWidth) {
                rows.add(currentRow)
                rowWidths.add(currentWidth)
                rowHeights.add(currentHeight)
                currentRow = mutableListOf()
                currentWidth = 0
                currentHeight = 0
            }
            currentRow.add(placeable)
            currentWidth += if (currentRow.size > 1) hGapPx + placeable.width else placeable.width
            currentHeight = maxOf(currentHeight, placeable.height)
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
            rowWidths.add(currentWidth)
            rowHeights.add(currentHeight)
        }

        val totalHeight = rowHeights.sum() + maxOf(0, (rows.size - 1) * vGapPx)
        layout(constraints.maxWidth, totalHeight) {
            var y = 0
            rows.forEachIndexed { i, row ->
                val rowWidth = rowWidths[i]
                var x = when (horizontalArrangement) {
                    Arrangement.End -> constraints.maxWidth - rowWidth
                    Arrangement.Center -> (constraints.maxWidth - rowWidth) / 2
                    else -> 0
                }
                row.forEachIndexed { j, placeable ->
                    if (j > 0) x += hGapPx
                    placeable.placeRelative(x, y + (rowHeights[i] - placeable.height) / 2)
                    x += placeable.width
                }
                y += rowHeights[i] + vGapPx
            }
        }
    }
}
