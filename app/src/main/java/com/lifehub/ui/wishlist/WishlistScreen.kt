package com.lifehub.ui.wishlist

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
import com.lifehub.data.SettingsRepository
import com.lifehub.data.entity.WishItemEntity
import com.lifehub.ui.components.EmptyState
import com.lifehub.ui.components.LifeCard
import com.lifehub.ui.theme.*
import com.lifehub.util.money0
import com.lifehub.viewmodel.BuyScope
import com.lifehub.viewmodel.WishViewModel
import com.lifehub.viewmodel.WishViewModelFactory

@Composable
fun WishlistScreen() {
    val app = LocalContext.current.applicationContext as LifeHubApplication
    val vm: WishViewModel = viewModel(factory = WishViewModelFactory(app))
    val state by vm.uiState.collectAsState()
    val scope by vm.scope.collectAsState()
    val fields by app.container.settings.fields.collectAsState(initial = SettingsRepository.FieldTable())
    val rows = remember(state, scope) { vm.filtered(state) }

    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("P1") }
    var note by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        item { Text("待买清单", style = MaterialTheme.typography.displayMedium, color = Ink) }
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
                    PrioritySelector(priority) { priority = it }
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
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            vm.add(name.trim(), price.toDoubleOrNull() ?: 0.0, priority, note.trim())
                            name = ""; price = ""; note = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Clay)
                ) { Text("+ 添加", color = PaperCard) }
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
            items(rows) { item ->
                WishRow(
                    item = item,
                    onToggle = { vm.toggle(item) },
                    onDelete = { vm.delete(item) },
                    onBoughtLedger = { vm.markBoughtAndLedger(item, fields) }
                )
            }
        }
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
    Row(Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, Line, RoundedCornerShape(8.dp))) {
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
private fun ScopeSegmented(selected: BuyScope, onSelect: (BuyScope) -> Unit) {
    val opts = listOf(BuyScope.TODO to "待买", BuyScope.DONE to "已买", BuyScope.ALL to "全部")
    Row(Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, Line, RoundedCornerShape(8.dp))) {
        opts.forEach { (s, label) ->
            val on = s == selected
            Box(
                Modifier
                    .clickable { onSelect(s) }
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
    onBoughtLedger: () -> Unit
) {
    val priColor = when (item.priority) { "P0" -> Danger; "P1" -> Clay; else -> Sage }
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = PaperCard) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.bought,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = Sage)
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.bought) InkSoft else Ink,
                    fontWeight = if (item.bought) FontWeight.Normal else FontWeight.Medium
                )
                Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(4.dp), color = priColor.copy(alpha = 0.15f)) {
                        Text(item.priority, style = MaterialTheme.typography.labelSmall, color = priColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                    }
                    if (item.note.isNotBlank()) Text(item.note, style = MaterialTheme.typography.labelSmall, color = InkSoft)
                }
            }
            Text("¥${money0(item.estPrice)}", style = MaterialTheme.typography.titleMedium, color = Ink)
            Spacer(Modifier.width(8.dp))
            if (!item.bought) {
                OutlinedButton(onClick = onBoughtLedger, modifier = Modifier.padding(end = 4.dp)) {
                    Text("已买并记账", style = MaterialTheme.typography.labelSmall)
                }
            }
            Text("🗑", modifier = Modifier.clickable { onDelete() }.padding(4.dp), style = MaterialTheme.typography.labelMedium)
        }
    }
}
