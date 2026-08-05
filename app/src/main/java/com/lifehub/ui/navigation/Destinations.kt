package com.lifehub.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.lifehub.ui.theme.Clay
import com.lifehub.ui.theme.InkSoft
import com.lifehub.ui.theme.PaperCard
import com.lifehub.util.vibrateLight

/**
 * 七个模块的导航定义
 */
sealed class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Home : Destination("home", "首页", Icons.Filled.Home)
    object Ledger : Destination("ledger", "记账", Icons.Filled.AccountBalanceWallet)
    object Habit : Destination("habit", "习惯", Icons.Filled.Loop)
    object Fitness : Destination("fitness", "健身", Icons.Filled.FitnessCenter)
    object Schedule : Destination("schedule", "日程", Icons.Filled.EventNote)
    object Wishlist : Destination("wishlist", "待买", Icons.Filled.ShoppingCart)
    object Media : Destination("media", "书影音", Icons.Filled.MenuBook)
    object Settings : Destination("settings", "设置", Icons.Filled.Settings)
}

val ALL_DESTINATIONS = listOf(
    Destination.Home, Destination.Ledger, Destination.Habit,
    Destination.Fitness, Destination.Schedule, Destination.Wishlist, Destination.Media,
    Destination.Settings
)

@Composable
fun LifeHubBottomBar(
    navController: NavHostController
) {
    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry.value?.destination?.route
    val ctx = LocalContext.current

    NavigationBar(
        containerColor = PaperCard,
        contentColor = InkSoft
    ) {
        ALL_DESTINATIONS.forEach { dest ->
            val selected = currentRoute == dest.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    ctx.vibrateLight()
                    navController.navigate(dest.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) },
                alwaysShowLabel = true
            )
        }
    }
}
