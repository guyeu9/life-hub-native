package com.lifehub

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lifehub.ui.navigation.Destination
import com.lifehub.ui.navigation.LifeHubBottomBar
import com.lifehub.ui.navigation.LifeHubNavGraph
import com.lifehub.ui.theme.*
import com.lifehub.util.JsonBackupUtil
import com.lifehub.util.vibrateLight
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            LifeHubTheme {
                val navController = rememberNavController()
                val app = application as LifeHubApplication
                val scope = rememberCoroutineScope()

                val exportLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/json")
                ) { uri ->
                    uri ?: return@rememberLauncherForActivityResult
                    scope.launch {
                        val ok = JsonBackupUtil.exportBackup(this@MainActivity, app.container, uri)
                        Toast.makeText(this@MainActivity, if (ok) "备份成功" else "备份失败", Toast.LENGTH_SHORT).show()
                    }
                }

                val importLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri ->
                    uri ?: return@rememberLauncherForActivityResult
                    scope.launch {
                        val ok = JsonBackupUtil.importBackup(this@MainActivity, app.container, uri)
                        Toast.makeText(this@MainActivity, if (ok) "导入成功" else "导入失败：文件无效", Toast.LENGTH_SHORT).show()
                    }
                }

                val onExport = { exportLauncher.launch("life-hub-backup-${System.currentTimeMillis() / 1000}.json") }
                val onImport = { importLauncher.launch(arrayOf("application/json")) }
                val onOpenSettings = { navController.navigate(Destination.Settings.route) }

                Scaffold(
                    topBar = {
                        LifeHubTopBar(
                            onExport = onExport,
                            onImport = onImport,
                            onOpenSettings = onOpenSettings
                        )
                    },
                    bottomBar = { LifeHubBottomBar(navController) }
                ) { padding ->
                    LifeHubNavGraph(
                        navController = navController,
                        onExport = onExport,
                        onImport = onImport,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LifeHubTopBar(
    onExport: () -> Unit,
    onImport: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val ctx = LocalContext.current
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Clay)
                )
                Spacer(Modifier.width(10.dp))
                // 品牌名：serif 字体，对齐网页 .brand h1
                Text(
                    "生活台",
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 21.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                )
                Spacer(Modifier.width(8.dp))
                // 副标题：全大写 + 宽字距，对齐网页 .brand .sub
                Text(
                    "LIFE DESK",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft,
                    letterSpacing = 1.6.sp
                )
                Spacer(Modifier.width(14.dp))
                // 日期：M月D日 加粗 + 周X，对齐网页 .topdate b
                val cal = Calendar.getInstance()
                val week = "日一二三四五六"
                val dateStr = SimpleDateFormat("M月d日", Locale.CHINA).format(cal.time)
                val wk = week[cal.get(Calendar.DAY_OF_WEEK) - 1]
                Row {
                    Text(dateStr, style = MaterialTheme.typography.labelMedium, color = Ink, fontWeight = FontWeight.SemiBold)
                    Text(" · 周$wk", style = MaterialTheme.typography.labelMedium, color = InkSoft)
                }
            }
        },
        actions = {
            TextButton(onClick = {
                ctx.vibrateLight()
                onExport()
            }) {
                Icon(Icons.Default.Download, contentDescription = null, tint = Clay, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("导出备份", color = Clay, style = MaterialTheme.typography.labelMedium)
            }
            TextButton(onClick = {
                ctx.vibrateLight()
                onImport()
            }) {
                Icon(Icons.Default.Upload, contentDescription = null, tint = Clay, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("导入恢复", color = Clay, style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = {
                ctx.vibrateLight()
                onOpenSettings()
            }) {
                Icon(Icons.Default.Settings, contentDescription = "设置", tint = Ink)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            // 半透毛玻璃感：对齐网页 .topbar rgba(244,242,237,.92)
            containerColor = PaperBg.copy(alpha = 0.92f),
            scrolledContainerColor = PaperBg.copy(alpha = 0.92f)
        )
    )
}
