package com.lifehub.ui.wishlist

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifehub.LifeHubApplication
import com.lifehub.data.SettingsRepository
import com.lifehub.data.entity.WishItemEntity
import com.lifehub.ui.components.AnimatedHeader
import com.lifehub.ui.components.AnimatedNumber
import com.lifehub.ui.components.ConfettiOverlay
import com.lifehub.ui.components.animateItemSlide
import com.lifehub.ui.components.EmptyState
import com.lifehub.ui.components.LifeCard
import com.lifehub.ui.components.SuccessButton
import com.lifehub.ui.components.toggleClick
import com.lifehub.ui.theme.*
import com.lifehub.util.money0
import com.lifehub.util.vibrateLight
import com.lifehub.util.vibrateSuccess
import com.lifehub.util.vibrateTick
import com.lifehub.viewmodel.BuyScope
import com.lifehub.viewmodel.WishViewModel
import com.lifehub.viewmodel.WishViewModelFactory

@Composable
fun WishlistScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as LifeHubApplication
    val vm: WishViewModel = viewModel(factory = WishViewModelFactory(app))
    val state by vm.uiState.collectAsState()
    val scope by vm.scope.collectAsState()
    val fields by app.container.settings.fields.collectAsState(initial = SettingsRepository.FieldTable())
    val rows = remember(state, scope) { vm.filtered(state) }

    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(fields.priorities.getOrNull(1) ?: "P1") }
    var note by remember { mutableStateOf("") }
    var confettiKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(fields) {
        if (priority !in fields.priorities) priority = fields.priorities.getOrNull(1) ?: fields.priorities.firstOrNull() ?: "P1"
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
        ) {
            item {
                AnimatedHeader(
                    eyebrow = "Wishlist",
                    title = "待买清单",
                    subtitle = "先记下来，过几天再看还想不想要。"
                )
            }
            item { WishMetrics(state) }

            item {
                LifeCard {
                    Text("加入清单", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("想买什么") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Clay, unfocusedBorderColor = Line)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = price, onValueChange = { price = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("预估价格") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Clay, unfocusedBorderColor = Line)
                        )
                        PrioritySelector(pris = fields.priorities, selected = priority) { priority = it }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = note, onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("备注（可选）") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Clay, unfocusedBorderColor = Line)
                    )
                    Spacer(Modifier.height(12.dp))
                    SuccessButton(
                        text = "+ 添加",
                        onClick = {
                            if (name.isNotBlank()) {
                                vm.add(name.trim(), price.toDoubleOrNull() ?: 0.0, priority, note.trim())
                                name = ""; price = ""; note = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = name.isNotBlank(),
                        containerColor = Ink
                    )
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("清单", style = MaterialTheme.typography.titleMedium, color = Ink)
                    ScopeSegmented(scope) { vm.setScope(it) }
                }
            }

            if (rows.isEmpty()) {
                item { EmptyState("清单是空的") }
            } else {
                itemsIndexed(rows) { index, item ->
                    WishRow(
                        item = item,
                        onToggle = {
                            if (!item.bought) {
                                context.vibrateSuccess()
                                confettiKey++
                            }
                            vm.toggle(item)
                        },
                        onDelete = { vm.delete(item) },
                        onBoughtLedger = {
                            context.vibrateSuccess()
                            vm.markBoughtAndLedger(item, fields)
                            Toast.makeText(context, "已标记买到，并记了一笔 ¥${money0(item.estPrice)}", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.animateItemSlide(index)
                    )
                }
            }
        }

        ConfettiOverlay(trigger = confettiKey, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun WishMetrics(s: com.lifehub.viewmodel.WishUiState) {
    val pct = if (s.budget > 0) (s.todoSum / s.budget * 100).toInt() else 0
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
        Metric("待买总额", money0(s.todoSum), "元", "${s.todoCount} 件在等", Clay)
        Metric("已买总额", money0(s.doneSum), "元", "${s.doneCount} 件已入手", Ink)
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
        Metric("高优先级", s.p0Count.toString(), "件", "真正需要的", Danger)
        Metric("占本月预算", pct.toString(), "%", "全买下来的话", Ink)
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
private fun PrioritySelector(pris: List<String>, selected: String, onSelect: (String) -> Unit) {
    val ctx = LocalContext.current
    val colors = pris.mapIndexed { i, _ ->
        when (i) {
            0 -> Danger
            1 -> Clay
            else -> InkSoft
        }
    }
    Row(Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, Line, RoundedCornerShape(8.dp))) {
        pris.forEachIndexed { i, p ->
            val on = p == selected
            Box(
                Modifier
                    .clickable {
                        ctx.vibrateTick()
                        onSelect(p)
                    }
                    .background(if (on) colors[i] else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(p, style = MaterialTheme.typography.labelMedium, color = if (on) PaperCard else InkSoft)
            }
        }
    }
}

@Composable
private fun ScopeSegmented(selected: BuyScope, onSelect: (BuyScope) -> Unit) {
    val ctx = LocalContext.current
    val opts = listOf(BuyScope.TODO to "待买", BuyScope.DONE to "已买", BuyScope.ALL to "全部")
    Row(Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, Line, RoundedCornerShape(8.dp))) {
        opts.forEach { (s, label) ->
            val on = s == selected
            Box(
                Modifier
                    .clickable {
                        ctx.vibrateLight()
                        onSelect(s)
                    }
                    .background(if (on) Ink else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = if (on) PaperCard else InkSoft)
            }
        }
    }
}

@Composable
private fun WishRow(
    item: WishItemEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onBoughtLedger: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priColor = when (item.priority) { "P0" -> Danger; "P1" -> Clay; else -> InkSoft }
    val cal = remember(item.id) {
        java.util.Calendar.getInstance().apply { timeInMillis = item.createdAt }
    }
    val joinText = "${cal.get(java.util.Calendar.MONTH) + 1}月${cal.get(java.util.Calendar.DAY_OF_MONTH)}日 加入"
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = PaperCard) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (item.bought) Sage else Color.White)
                    .border(1.6.dp, if (item.bought) Sage else InkFaint, CircleShape)
                    .toggleClick { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (item.bought) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.bought) InkFaint else Ink,
                    fontWeight = if (item.bought) FontWeight.Normal else FontWeight.Medium,
                    textDecoration = if (item.bought) TextDecoration.LineThrough else TextDecoration.None
                )
                Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(4.dp), color = priColor.copy(alpha = 0.15f)) {
                        Text(item.priority, style = MaterialTheme.typography.labelSmall, color = priColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                    }
                    Text(joinText, style = MaterialTheme.typography.labelSmall, color = InkSoft)
                    if (item.note.isNotBlank()) Text(item.note, style = MaterialTheme.typography.labelSmall, color = InkSoft)
                }
            }
            Text("¥${money0(item.estPrice)}", style = MaterialTheme.typography.titleMedium, color = Ink)
            Spacer(Modifier.width(8.dp))
            if (!item.bought) {
                val ctx = LocalContext.current
                OutlinedButton(
                    onClick = {
                        ctx.vibrateSuccess()
                        onBoughtLedger()
                    },
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text("已买并记账", style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = { onDelete() }) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = Danger)
            }
        }
    }
}
