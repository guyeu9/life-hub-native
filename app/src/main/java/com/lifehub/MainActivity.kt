package com.lifehub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.lifehub.ui.navigation.LifeHubBottomBar
import com.lifehub.ui.navigation.LifeHubNavGraph
import com.lifehub.ui.theme.LifeHubTheme
import com.lifehub.util.JsonBackupUtil

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 沉浸式状态栏（状态栏透明，内容延伸到顶部）
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            LifeHubTheme {
                val navController = rememberNavController()
                val app = application as LifeHubApplication

                // 导出：保存文件选择器
                val exportLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/json")
                ) { uri ->
                    uri ?: return@rememberLauncherForActivityResult
                    JsonBackupUtil.exportBackup(this, app.container, uri)
                }

                // 导入：打开文件选择器
                val importLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri ->
                    uri ?: return@rememberLauncherForActivityResult
                    JsonBackupUtil.importBackup(this, app.container, uri) {
                        // 导入完成后回调（后续可加 Toast）
                    }
                }

                Scaffold(
                    bottomBar = { LifeHubBottomBar(navController) }
                ) { padding ->
                    LifeHubNavGraph(
                        navController = navController,
                        onExport = { exportLauncher.launch("life-hub-backup-${System.currentTimeMillis() / 1000}.json") },
                        onImport = { importLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}
