@file:OptIn(ExperimentalMaterial3Api::class)

package com.lifehub.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lifehub.data.SettingsRepository
import com.lifehub.ui.theme.Clay
import com.lifehub.ui.theme.Ink
import com.lifehub.ui.theme.InkSoft
import com.lifehub.ui.theme.Line

@Composable
fun QuickAmountsDialog(
    current: SettingsRepository.QuickAmounts,
    onDismiss: () -> Unit,
    onSave: (SettingsRepository.QuickAmounts) -> Unit
) {
    var expense by remember { mutableStateOf(current.expense.joinToString(",")) }
    var income by remember { mutableStateOf(current.income.joinToString(",")) }
    var rebate by remember { mutableStateOf(current.rebate.joinToString(",")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义常用金额", color = Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("用逗号分隔，最多 8 个，留空则用出厂值", style = MaterialTheme.typography.bodySmall, color = InkSoft)
                AmtField("支出", expense) { expense = it }
                AmtField("收入", income) { income = it }
                AmtField("返利", rebate) { rebate = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    SettingsRepository.QuickAmounts(
                        expense = parseAmts(expense, SettingsRepository.QuickAmounts().expense),
                        income = parseAmts(income, SettingsRepository.QuickAmounts().income),
                        rebate = parseAmts(rebate, SettingsRepository.QuickAmounts().rebate)
                    )
                )
            }) { Text("保存", color = Clay) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun AmtField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, color = InkSoft) },
        placeholder = { Text("如 10,20,50") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Clay,
            unfocusedBorderColor = Line,
            focusedLabelColor = Clay
        )
    )
}

private fun parseAmts(raw: String, defaults: List<Double>): List<Double> {
    val cleaned = raw.split(",")
        .map { it.trim() }
        .mapNotNull { it.toFloatOrNull()?.let { n -> kotlin.math.round(n).toInt() } }
        .filter { it > 0 }
        .take(8)
    return if (cleaned.isEmpty()) defaults else cleaned.map { it.toDouble() }
}
