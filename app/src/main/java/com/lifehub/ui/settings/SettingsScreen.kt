@file:OptIn(ExperimentalMaterial3Api::class)

package com.lifehub.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifehub.LifeHubApplication
import com.lifehub.ui.components.AnimatedHeader
import com.lifehub.ui.components.LifeCard
import com.lifehub.ui.theme.*
import com.lifehub.util.vibrateLight
import com.lifehub.util.vibrateMedium
import com.lifehub.util.vibrateSuccess
import com.lifehub.viewmodel.SettingsViewModel
import com.lifehub.viewmodel.SettingsViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onExport: () -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as LifeHubApplication
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(app))
    val state by vm.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var showQuickAmts by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showDemoConfirm by remember { mutableStateOf(false) }
    var showFields by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置与数据", color = Ink) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PaperCard)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 18.dp, bottom = 24.dp)
            ) {
                item {
                    AnimatedHeader(
                        title = "设置与数据",
                        subtitle = "所有数据只存在这台设备里，不会上传到任何服务器。"
                    )
                }

                item {
                    LifeCard {
                        Text("存储统计", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Spacer(Modifier.height(12.dp))
                        StatLine("当前占用本地存储约", "${String.format("%.1f", state.storageSizeKb)} KB")
                        Divider(Modifier.padding(vertical = 10.dp), color = Line)
                        StatLine("账目", state.ledgerCount, "条")
                        StatLine("日程", state.scheduleCount, "条")
                        StatLine("待买", state.wishCount, "条")
                        StatLine("习惯", state.habitCount, "个")
                        StatLine("身体记录", state.fitnessCount, "天")
                        StatLine("收藏", state.mediaCount, "条")
                        Divider(Modifier.padding(vertical = 10.dp), color = Line)
                        Text(
                            "自上次备份新增：${state.pendingCount} 条",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSoft
                        )
                    }
                }

                item {
                    LifeCard {
                        Text("数据管理", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Spacer(Modifier.height(8.dp))
                        SettingsButton("导出 JSON 完整备份") {
                            context.vibrateLight()
                            onExport()
                        }
                        SettingsButton("从 JSON 备份导入 / 恢复") {
                            context.vibrateLight()
                            onImport()
                        }
                        SettingsButton("字段维护（分类 / 标签 / 优先级）") {
                            context.vibrateLight()
                            showFields = true
                        }
                        SettingsButton("自定义首页常用金额") {
                            context.vibrateLight()
                            showQuickAmts = true
                        }
                        SettingsButton("重新载入演示数据") {
                            context.vibrateLight()
                            showDemoConfirm = true
                        }
                        SettingsButton("清空全部数据（含演示）", color = Danger) {
                            context.vibrateMedium()
                            showClearConfirm = true
                        }
                    }
                }

                item {
                    Text(
                        "所有数据只存在这台设备的应用存储里，换设备或清除应用数据后不会保留。请定期导出备份。",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                    )
                }
            }

            // 提示
            state.toastMessage?.let { msg ->
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(2300)
                    vm.clearToast()
                }
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    containerColor = Ink,
                    contentColor = PaperCard,
                    shape = RoundedCornerShape(8.dp)
                ) { Text(msg) }
            }
        }
    }

    if (showQuickAmts) {
        QuickAmountsDialog(
            current = state.quickAmounts,
            onDismiss = { showQuickAmts = false },
            onSave = { q ->
                context.vibrateSuccess()
                scope.launch { vm.saveQuickAmounts(q) }
                showQuickAmts = false
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空全部数据") },
            text = { Text("确定要删除所有账目、日程、待买、习惯、身体记录和收藏吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    context.vibrateMedium()
                    scope.launch { vm.clearAll() }
                    showClearConfirm = false
                }) { Text("清空", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("取消") }
            }
        )
    }

    if (showDemoConfirm) {
        AlertDialog(
            onDismissRequest = { showDemoConfirm = false },
            title = { Text("重新载入演示数据") },
            text = { Text("这会先清空当前数据，然后写入网页同款的演示数据。继续吗？") },
            confirmButton = {
                TextButton(onClick = {
                    context.vibrateSuccess()
                    scope.launch { vm.loadDemoData() }
                    showDemoConfirm = false
                }) { Text("载入", color = Clay) }
            },
            dismissButton = {
                TextButton(onClick = { showDemoConfirm = false }) { Text("取消") }
            }
        )
    }

    if (showFields) {
        FieldsMaintenanceSheet(
            vm = vm,
            onDismiss = { showFields = false }
        )
    }
}

@Composable
private fun StatLine(label: String, count: Int, unit: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = InkSoft)
        Text("$count $unit", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Ink)
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = InkSoft)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Ink)
    }
}

@Composable
private fun SettingsButton(
    text: String,
    color: Color = Ink,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text("›", style = MaterialTheme.typography.bodyLarge, color = InkSoft)
        }
    }
}
