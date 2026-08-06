@file:OptIn(ExperimentalMaterial3Api::class)

package com.lifehub.ui.settings

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lifehub.data.SettingsRepository
import com.lifehub.ui.theme.*
import com.lifehub.util.vibrateLight
import com.lifehub.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

enum class FieldTab(val label: String, val key: String) {
    EXPENSE("支出分类", "expenseCats"),
    INCOME("收入分类", "incomeCats"),
    REBATE("返利分类", "rebateCats"),
    TAG("日程标签", "planTags"),
    PRIORITY("优先级", "pris"),
    MEDIA("书影音类型", "mediaTypes")
}

private fun tipForTab(tab: FieldTab): String = when (tab) {
    FieldTab.EXPENSE -> "改名后，历史账目里用到这个分类的记录会同步更新，不会丢数据。"
    FieldTab.INCOME -> "改名后，历史账目里用到这个分类的记录会同步更新，不会丢数据。"
    FieldTab.REBATE -> "返利分类专门用来区分返利来源，比如外卖返利、信用卡返现。"
    FieldTab.TAG -> "日程添加时可以直接选这些标签。"
    FieldTab.PRIORITY -> "列表顺序就是优先级高低，最上面的最紧急。"
    FieldTab.MEDIA -> "可以加「播客」「展览」「话剧」这类你自己的分类。"
}

@Composable
fun FieldsMaintenanceSheet(
    vm: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val state by vm.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(FieldTab.EXPENSE) }
    var newName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    var pendingDelete by remember { mutableStateOf<PendingDelete?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val items = rememberItemsForTab(state.fields, selectedTab)
    val onUpdate: (List<SettingsRepository.CategoryDef>) -> Unit = { list ->
        scope.launch { vm.setFields(updateFields(state.fields, selectedTab, list)) }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PaperCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("字段维护", style = MaterialTheme.typography.headlineSmall, color = Ink)
            Text(tipForTab(selectedTab), style = MaterialTheme.typography.bodySmall, color = InkSoft)
            Spacer(Modifier.height(12.dp))

            // Tab bar
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = PaperCard,
                contentColor = Clay,
                edgePadding = 0.dp
            ) {
                FieldTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = {
                            context.vibrateLight()
                            selectedTab = tab
                        },
                        text = { Text(tab.label) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(items, key = { _, it -> it.name + selectedTab.name }) { i, item ->
                    FieldRow(
                        item = item,
                        canUp = i > 0,
                        canDown = i < items.size - 1,
                        vm = vm,
                        tab = selectedTab,
                        onNameChange = { name ->
                            val oldName = items[i].name
                            if (oldName != name) {
                                scope.launch { vm.renameField(selectedTab, oldName, name) }
                            }
                            val updated = items.toMutableList()
                            updated[i] = item.copy(name = name)
                            onUpdate(updated)
                        },
                        onColorChange = { color ->
                            val updated = items.toMutableList()
                            updated[i] = item.copy(color = color)
                            onUpdate(updated)
                        },
                        onMoveUp = {
                            val updated = items.toMutableList()
                            updated[i] = items[i - 1]
                            updated[i - 1] = items[i]
                            onUpdate(updated)
                        },
                        onMoveDown = {
                            val updated = items.toMutableList()
                            updated[i] = items[i + 1]
                            updated[i + 1] = items[i]
                            onUpdate(updated)
                        },
                        onDelete = {
                            if (items.size <= 1) {
                                scope.launch { vm.toast("至少要保留一项") }
                                return@FieldRow
                            }
                            scope.launch {
                                val usage = vm.fieldUsage(selectedTab, item.name)
                                if (usage == 0) {
                                    context.vibrateLight()
                                    val updated = items.toMutableList()
                                    updated.removeAt(i)
                                    onUpdate(updated)
                                } else if (selectedTab == FieldTab.MEDIA) {
                                    val fallback = items.filterIndexed { idx, _ -> idx != i }.firstOrNull()
                                    if (fallback == null) {
                                        vm.toast("至少要保留一项")
                                    } else {
                                        pendingDelete = PendingDelete(
                                            index = i,
                                            item = item,
                                            usage = usage,
                                            others = listOf(fallback.name),
                                            isMedia = true
                                        )
                                    }
                                } else {
                                    val others = items.filterIndexed { idx, _ -> idx != i }.map { it.name }
                                    pendingDelete = PendingDelete(
                                        index = i,
                                        item = item,
                                        usage = usage,
                                        others = others,
                                        isMedia = false
                                    )
                                }
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("新增一项") },
                    placeholder = { Text("输入名称后回车或点添加") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Clay, unfocusedBorderColor = Line)
                )
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            context.vibrateLight()
                            val updated = items.toMutableList()
                            updated.add(SettingsRepository.CategoryDef(newName.trim(), randomColor()))
                            onUpdate(updated)
                            newName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Clay)
                ) { Text("添加") }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    context.vibrateLight()
                    showResetConfirm = true
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("恢复这一组的出厂设置") }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("恢复出厂设置", color = Ink) },
            text = { Text("会把「${selectedTab.label}」这一组恢复成默认内容。已有记录不会被删除，但可能引用到已不存在的名称。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        context.vibrateLight()
                        scope.launch { vm.setFields(resetTab(selectedTab, state.fields)) }
                        showResetConfirm = false
                    }
                ) { Text("恢复", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("取消") }
            }
        )
    }

    pendingDelete?.let { pd ->
        if (pd.isMedia) {
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("删除类型") },
                text = { Text("有 ${pd.usage} 条收藏正在用「${pd.item.name}」。删除后这些收藏会归到「${pd.others.first()}」下。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                vm.reassignField(selectedTab, pd.item.name, pd.others.first())
                                val updated = items.toMutableList()
                                updated.removeAt(pd.index)
                                onUpdate(updated)
                                pendingDelete = null
                            }
                        }
                    ) { Text("仍然删除", color = Danger) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) { Text("取消") }
                }
            )
        } else {
            var target by remember(pd) { mutableStateOf(pd.others.firstOrNull() ?: "") }
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("删除「${pd.item.name}」") },
                text = {
                    Column {
                        Text("有 ${pd.usage} 条记录正在使用它。删掉之后，这些记录要归到哪一项？")
                        Spacer(Modifier.height(12.dp))
                        pd.others.forEach { opt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { target = opt }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = target == opt, onClick = { target = opt })
                                Spacer(Modifier.width(8.dp))
                                Text(opt)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("记录本身不会被删除，只是换个归属。", style = MaterialTheme.typography.bodySmall, color = InkSoft)
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                vm.reassignField(selectedTab, pd.item.name, target)
                                val updated = items.toMutableList()
                                updated.removeAt(pd.index)
                                onUpdate(updated)
                                pendingDelete = null
                            }
                        }
                    ) { Text("删除并迁移", color = Danger) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) { Text("取消") }
                }
            )
        }
    }
}

private data class PendingDelete(
    val index: Int,
    val item: SettingsRepository.CategoryDef,
    val usage: Int,
    val others: List<String>,
    val isMedia: Boolean
)

@Composable
private fun rememberItemsForTab(fields: SettingsRepository.FieldTable, tab: FieldTab): List<SettingsRepository.CategoryDef> {
    return remember(fields, tab) {
        when (tab) {
            FieldTab.EXPENSE -> fields.expenseCats
            FieldTab.INCOME -> fields.incomeCats
            FieldTab.REBATE -> fields.rebateCats
            FieldTab.TAG -> fields.planTags.map { SettingsRepository.CategoryDef(it, "#918C81") }
            FieldTab.PRIORITY -> fields.priorities.map { SettingsRepository.CategoryDef(it, "#918C81") }
            FieldTab.MEDIA -> fields.mediaTypes
        }
    }
}

private fun updateFields(
    fields: SettingsRepository.FieldTable,
    tab: FieldTab,
    list: List<SettingsRepository.CategoryDef>
): SettingsRepository.FieldTable {
    return when (tab) {
        FieldTab.EXPENSE -> fields.copy(expenseCats = list)
        FieldTab.INCOME -> fields.copy(incomeCats = list)
        FieldTab.REBATE -> fields.copy(rebateCats = list)
        FieldTab.TAG -> fields.copy(planTags = list.map { it.name })
        FieldTab.PRIORITY -> fields.copy(priorities = list.map { it.name })
        FieldTab.MEDIA -> fields.copy(mediaTypes = list)
    }
}

private fun resetTab(tab: FieldTab, current: SettingsRepository.FieldTable): SettingsRepository.FieldTable {
    val defaults = SettingsRepository.FieldTable()
    return when (tab) {
        FieldTab.EXPENSE -> current.copy(expenseCats = defaults.expenseCats)
        FieldTab.INCOME -> current.copy(incomeCats = defaults.incomeCats)
        FieldTab.REBATE -> current.copy(rebateCats = defaults.rebateCats)
        FieldTab.TAG -> current.copy(planTags = defaults.planTags)
        FieldTab.PRIORITY -> current.copy(priorities = defaults.priorities)
        FieldTab.MEDIA -> current.copy(mediaTypes = defaults.mediaTypes)
    }
}

@Composable
private fun FieldRow(
    item: SettingsRepository.CategoryDef,
    canUp: Boolean,
    canDown: Boolean,
    vm: SettingsViewModel,
    tab: FieldTab,
    onNameChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    var name by remember(item.name) { mutableStateOf(item.name) }
    var usage by remember { mutableStateOf(0) }
    LaunchedEffect(item.name, tab) {
        usage = vm.fieldUsage(tab, item.name)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (tab == FieldTab.EXPENSE || tab == FieldTab.INCOME || tab == FieldTab.REBATE) {
            ColorSwatch(item.color) { onColorChange(it) }
        } else {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(InkSoft.copy(alpha = 0.4f))
            )
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && name != item.name) {
                        onNameChange(name)
                    }
                },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Clay, unfocusedBorderColor = Line)
        )
        Text(
            if (usage > 0) "$usage 条在用" else "未使用",
            style = MaterialTheme.typography.bodySmall,
            color = InkSoft,
            modifier = Modifier.widthIn(min = 52.dp)
        )
        IconButton(onClick = onMoveUp, enabled = canUp) {
            Icon(Icons.Default.ArrowUpward, contentDescription = "上移", tint = if (canUp) InkSoft else Line)
        }
        IconButton(onClick = onMoveDown, enabled = canDown) {
            Icon(Icons.Default.ArrowDownward, contentDescription = "下移", tint = if (canDown) InkSoft else Line)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Close, contentDescription = "删除", tint = Danger)
        }
    }
}

@Composable
private fun ColorSwatch(color: String, onColorChange: (String) -> Unit) {
    val parsed = try {
        Color(AndroidColor.parseColor(color))
    } catch (_: Exception) { Color.Gray }

    var showPicker by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(parsed)
            .border(1.dp, Line, CircleShape)
            .clickable { showPicker = true }
    )
    if (showPicker) {
        SimpleColorPicker(
            current = color,
            onDismiss = { showPicker = false },
            onSelect = {
                onColorChange(it)
                showPicker = false
            }
        )
    }
}

@Composable
private fun SimpleColorPicker(
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val colors = listOf(
        "#A2543C", "#5D7561", "#A8842F", "#4A6478", "#B0433A",
        "#8D6B52", "#6B6A94", "#C2755A", "#4C7554", "#918C81"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择颜色", color = Ink) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colors.forEach { c ->
                    val parsed = try { Color(AndroidColor.parseColor(c)) } catch (_: Exception) { Color.Gray }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(parsed)
                            .border(if (c.equals(current, ignoreCase = true)) 2.dp else 1.dp, if (c.equals(current, ignoreCase = true)) Ink else Line, CircleShape)
                            .clickable { onSelect(c) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun randomColor(): String {
    val palette = listOf("#A2543C", "#5D7561", "#A8842F", "#4A6478", "#B0433A", "#8D6B52", "#6B6A94", "#C2755A")
    return palette.random()
}
