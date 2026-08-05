package com.lifehub

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.lifehub.ui.navigation.LifeHubBottomBar
import com.lifehub.ui.navigation.LifeHubNavGraph
import com.lifehub.ui.theme.LifeHubTheme
import com.lifehub.util.JsonBackupUtil
import kotlinx.coroutines.launch

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
