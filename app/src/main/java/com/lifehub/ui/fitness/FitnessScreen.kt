package com.lifehub.ui.fitness

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifehub.LifeHubApplication
import com.lifehub.charts.LineChart
import com.lifehub.data.SettingsRepository
import com.lifehub.data.entity.FitnessEntity
import com.lifehub.data.entity.FitnessPlanEntity
import com.lifehub.ui.components.AnimatedHeader
import com.lifehub.ui.components.AnimatedNumber
import com.lifehub.ui.components.ConfettiOverlay
import com.lifehub.ui.components.EmptyState
import com.lifehub.ui.components.LifeCard
import com.lifehub.ui.components.SuccessButton
import com.lifehub.ui.components.animateItemSlide
import com.lifehub.ui.components.hapticClick
import com.lifehub.ui.components.toggleClick
import com.lifehub.ui.theme.*
import com.lifehub.util.cnDateKey
import com.lifehub.util.todayKey
import com.lifehub.util.vibrateLight
import com.lifehub.util.vibrateMedium
import com.lifehub.util.vibrateSuccess
import com.lifehub.util.vibrateTick
import com.lifehub.viewmodel.FitnessUiState
import com.lifehub.viewmodel.FitnessViewModel
import com.lifehub.viewmodel.FitnessViewModelFactory
import kotlin.math.max
import kotlin.math.min

@Composable
fun FitnessScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as LifeHubApplication
    val vm: FitnessViewModel = viewModel(factory = FitnessViewModelFactory(app))
    val state by vm.uiState.collectAsState()

    var showProfile by remember { mutableStateOf(false) }
    var editingPlan by remember { mutableStateOf<Int?>(null) }
    var confettiKey by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    AnimatedHeader("减脂健身")
                    OutlinedButton(
                        onClick = {
                            context.vibrateLight()
                            showProfile = true
                        }
                    ) { Text("身高/目标/基代") }
                }
            }

            item { FitnessMetrics(state) }
            item { GoalProgress(state) }

            if (state.logs.isNotEmpty()) {
                item {
                    LifeCard {
                        Text("体重曲线", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Spacer(Modifier.height(8.dp))
                        LineChart(points = state.logs.map { it.weight.toFloat() })
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(16.dp)) {
                            LegendDot(Slate, "每日实测")
                            LegendDot(Clay, "7 日均线")
                        }
                    }
                }
            }

            item {
                AddLogForm(
                    onSave = { w, f, i, b ->
                        context.vibrateSuccess()
                        confettiKey++
                        vm.saveLog(todayKey(), w, f, i, b, "")
                    }
                )
            }

            item {
                Text("本周训练计划", style = MaterialTheme.typography.titleMedium, color = Ink)
                Spacer(Modifier.height(8.dp))
            }
            item {
                WeekPlan(
                    plan = state.plan,
                    todayIdx = todayPlanIdx(),
                    onToggle = { idx, checked ->
                        if (checked) {
                            context.vibrateSuccess()
                            confettiKey++
                        } else {
                            context.vibrateTick()
                        }
                        vm.togglePlanDone(idx)
                    },
                    onEdit = { editingPlan = it }
                )
            }

            item {
                Text("历史记录", style = MaterialTheme.typography.titleMedium, color = Ink)
                Spacer(Modifier.height(8.dp))
            }
            if (state.logs.isEmpty()) {
                item { EmptyState("还没有身体数据，先在上面记一条") }
            } else {
                itemsIndexed(state.logs.asReversed().take(20)) { index, log ->
                    HistoryRow(
                        log = log,
                        tdee = state.profile.tdee,
                        onDelete = {
                            context.vibrateMedium()
                            vm.deleteLog(log)
                        },
                        modifier = Modifier.animateItemSlide(index)
                    )
                }
            }
        }

        ConfettiOverlay(trigger = confettiKey, modifier = Modifier.fillMaxSize())
    }

    if (showProfile) {
        ProfileSheet(
            state.profile,
            onDismiss = { showProfile = false },
            onSave = {
                context.vibrateSuccess()
                vm.updateProfile(it); showProfile = false
            }
        )
    }
    editingPlan?.let { idx ->
        val cur = state.plan.firstOrNull { it.dayIndex == idx }
        if (cur != null) {
            EditPlanSheet(
                cur,
                onDismiss = { editingPlan = null },
                onSave = { t, d ->
                    context.vibrateSuccess()
                    vm.editPlan(idx, t, d); editingPlan = null
                }
            )
        }
    }
}

@Composable
private fun FitnessMetrics(state: FitnessUiState) {
    val logs = state.logs
    val last = logs.lastOrNull()
    val pts = logs.map { it.weight }.filter { it > 0 }
    val ma7 = if (pts.size >= 7) pts.takeLast(7).average() else pts.lastOrNull() ?: 0.0
    val h = state.profile.height / 100.0
    val bmi = if (last != null && last.weight > 0) last.weight / (h * h) else 0.0
    val lv = bmiLevel(bmi)
    val tgt = state.profile.targetWeight
    val deficit = state.todayLog?.let { (state.profile.tdee + it.burn - it.intake).toInt() }
    val last7 = logs.takeLast(7).filter { it.intake > 0 }
    val avgDef = if (last7.isNotEmpty()) last7.average { (state.profile.tdee + it.burn - it.intake) }.toInt() else null

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricCell("当前体重", last?.weight?.toString() ?: "—", "kg", last?.dateKey?.let { cnDateKey(it) } ?: "待记录", color = Ink)
        MetricCell("7 日均线", if (ma7 > 0) "%.1f".format(ma7) else "—", "kg", if (pts.size >= 7) "近 7 日平均" else "攒够 7 天才有", color = Clay)
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricCell("BMI", if (bmi > 0) "%.1f".format(bmi) else "—", "", if (bmi > 0) "${lv.first} · 身高 ${state.profile.height.toInt()}cm" else "先填身高", color = lv.second)
        MetricCell("距目标", if (last != null && tgt > 0) "%.1f".format(kotlin.math.abs(last.weight - tgt)) else "—", "kg", "目标 ${tgt}kg", color = Sage)
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricCell(
            "今日热量缺口",
            deficit?.let { (if (it > 0) "+" else "") + it } ?: "—",
            "kcal",
            if (deficit == null) "今天还没记" else "基代${state.profile.tdee.toInt()} + 运动 − 摄入",
            color = if (deficit != null && deficit > 0) Success else if (deficit == null) InkSoft else Danger
        )
        MetricCell(
            "近 7 日均缺口",
            avgDef?.let { (if (it > 0) "+" else "") + it } ?: "—",
            "kcal",
            if (avgDef != null) "约每周 %.2f".format(avgDef * 7.0 / 7700) + "kg" else "数据不足",
            color = if (avgDef != null && avgDef > 0) Success else if (avgDef == null) InkSoft else Danger
        )
    }
}

@Composable
private fun RowScope.MetricCell(label: String, value: String, unit: String, desc: String, color: Color) {
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
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(3.dp))
                    Text(unit, style = MaterialTheme.typography.labelSmall, color = InkSoft)
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(desc, style = MaterialTheme.typography.labelSmall, color = InkSoft)
        }
    }
}

@Composable
private fun GoalProgress(state: FitnessUiState) {
    val last = state.logs.lastOrNull()
    val start = if (state.profile.startWeight > 0) state.profile.startWeight else state.logs.firstOrNull()?.weight ?: 0.0
    val tgt = state.profile.targetWeight
    val totalNeed = if (start > 0 && tgt > 0) kotlin.math.abs(start - tgt) else 1.0
    val doneKg = if (last != null && start > 0) kotlin.math.abs(start - last.weight) else 0.0
    val prog = (doneKg / totalNeed).coerceIn(0.0, 1.0)
    LifeCard {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text("起点 ${if (start > 0) "%.1f".format(start) else "—"} kg", style = MaterialTheme.typography.labelMedium, color = InkSoft)
            Text("已完成 %.1f kg（%d%%）".format(doneKg, (prog * 100).toInt()), style = MaterialTheme.typography.labelMedium, color = Sage, fontWeight = FontWeight.SemiBold)
            Text("目标 ${tgt}kg", style = MaterialTheme.typography.labelMedium, color = InkSoft)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = prog.toFloat(),
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = Clay,
            trackColor = Line
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLogForm(onSave: (weight: Double, fat: Double, intake: Double, burn: Double) -> Unit) {
    var weight by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var intake by remember { mutableStateOf("") }
    var burn by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    LifeCard {
        Text("记录今天的身体数据", style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.height(4.dp))
        Text("同一天重复记录会覆盖", style = MaterialTheme.typography.labelSmall, color = InkSoft)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            NumField("体重 kg", weight, { weight = it }, Modifier.weight(1f))
            NumField("体脂 %", fat, { fat = it }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            NumField("摄入 kcal", intake, { intake = it }, Modifier.weight(1f))
            NumField("运动 kcal", burn, { burn = it }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        SuccessButton(
            text = "保存",
            onClick = {
                val w = weight.toDoubleOrNull() ?: 0.0
                if (w > 0) {
                    onSave(w, fat.toDoubleOrNull() ?: 0.0, intake.toDoubleOrNull() ?: 0.0, burn.toDoubleOrNull() ?: 0.0)
                    weight = ""; fat = ""; intake = ""; burn = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun NumField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = InkSoft)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value, onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Clay, unfocusedBorderColor = Line)
        )
    }
}

@Composable
private fun WeekPlan(
    plan: List<FitnessPlanEntity>,
    todayIdx: Int,
    onToggle: (Int, Boolean) -> Unit,
    onEdit: (Int) -> Unit
) {
    val ctx = LocalContext.current
    val names = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        plan.forEachIndexed { i, d ->
            Surface(
                modifier = Modifier.fillMaxWidth().toggleClick { onToggle(i, !d.done) },
                shape = RoundedCornerShape(8.dp),
                color = if (i == todayIdx) ClayLight.copy(alpha = 0.18f) else PaperCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (i == todayIdx) Clay else Line)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = d.done,
                        onCheckedChange = null,
                        modifier = Modifier.toggleClick { onToggle(i, !d.done) },
                        colors = CheckboxDefaults.colors(checkedColor = Sage)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(names[i], style = MaterialTheme.typography.labelMedium, color = InkSoft)
                            if (i == todayIdx) { Spacer(Modifier.width(6.dp)); Text("· 今天", style = MaterialTheme.typography.labelSmall, color = Clay) }
                        }
                        Text(d.title.ifBlank { "待安排" }, style = MaterialTheme.typography.bodyMedium, color = Ink)
                        if (d.detail.isNotBlank()) Text(d.detail, style = MaterialTheme.typography.labelSmall, color = InkSoft)
                    }
                    OutlinedButton(
                        onClick = {
                            ctx.vibrateLight()
                            onEdit(i)
                        }
                    ) { Text("改") }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    log: FitnessEntity,
    tdee: Double,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val def = (tdee + log.burn - log.intake).toInt()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = PaperCard
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(cnDateKey(log.dateKey), style = MaterialTheme.typography.bodyMedium, color = Ink)
                    Spacer(Modifier.width(8.dp))
                    Text("%.1f".format(log.weight), style = MaterialTheme.typography.titleMedium, color = Ink)
                    Text(" kg", style = MaterialTheme.typography.labelSmall, color = InkSoft)
                    if (log.fat > 0) { Spacer(Modifier.width(8.dp)); Text("体脂 %.1f%%".format(log.fat), style = MaterialTheme.typography.labelSmall, color = InkSoft) }
                }
                if (log.intake > 0 || log.burn > 0) {
                    Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (log.intake > 0) Text("摄入 ${log.intake.toInt()}", style = MaterialTheme.typography.labelSmall, color = InkSoft)
                        if (log.burn > 0) Text("运动 ${log.burn.toInt()}", style = MaterialTheme.typography.labelSmall, color = InkSoft)
                        if (log.intake > 0) {
                            val color = if (def > 0) Sage else Danger
                            Text("缺口 ${if (def > 0) "+" else ""}$def", style = MaterialTheme.typography.labelSmall, color = color)
                        }
                    }
                }
            }
            Text(
                "🗑",
                modifier = Modifier.hapticClick { onDelete() }.padding(4.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = InkSoft)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSheet(profile: SettingsRepository.FitnessProfile, onDismiss: () -> Unit, onSave: (SettingsRepository.FitnessProfile) -> Unit) {
    var height by remember { mutableStateOf(profile.height.toString()) }
    var start by remember { mutableStateOf(if (profile.startWeight > 0) profile.startWeight.toString() else "") }
    var target by remember { mutableStateOf(profile.targetWeight.toString()) }
    var tdee by remember { mutableStateOf(profile.tdee.toString()) }
    val ctx = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("身高 / 目标 / 基代", style = MaterialTheme.typography.headlineMedium, color = Ink)
            NumField("身高 cm", height, { height = it })
            NumField("起点体重 kg（留空取首条）", start, { start = it })
            NumField("目标体重 kg", target, { target = it })
            NumField("基础代谢 TDEE kcal", tdee, { tdee = it })
            SuccessButton(
                text = "保存",
                onClick = {
                    onSave(
                        SettingsRepository.FitnessProfile(
                            height = height.toDoubleOrNull() ?: 170.0,
                            startWeight = start.toDoubleOrNull() ?: 0.0,
                            targetWeight = target.toDoubleOrNull() ?: 60.0,
                            tdee = tdee.toDoubleOrNull() ?: 1600.0
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPlanSheet(cur: FitnessPlanEntity, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember { mutableStateOf(cur.title) }
    var detail by remember { mutableStateOf(cur.detail) }
    val ctx = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("编辑训练计划", style = MaterialTheme.typography.headlineMedium, color = Ink)
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("训练标题，如：力量 · 上肢") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Clay, unfocusedBorderColor = Line)
            )
            OutlinedTextField(
                value = detail, onValueChange = { detail = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("训练内容") },
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Clay, unfocusedBorderColor = Line)
            )
            SuccessButton(
                text = "保存",
                onClick = { onSave(title, detail) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** 今天是周几的计划索引（0=周一 ... 6=周日） */
private fun todayPlanIdx(): Int {
    val cal = java.util.Calendar.getInstance()
    return (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
}

/** BMI 分级 → (描述, 颜色) */
private fun bmiLevel(bmi: Double): Pair<String, Color> = when {
    bmi <= 0 -> "—" to InkSoft
    bmi < 18.5 -> "偏瘦" to Slate
    bmi < 24 -> "正常" to Sage
    bmi < 28 -> "偏胖" to Amber
    else -> "肥胖" to Danger
}

private fun <T> List<T>.average(selector: (T) -> Double): Double = this.map(selector).average()
