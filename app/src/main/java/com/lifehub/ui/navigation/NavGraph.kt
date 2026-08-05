package com.lifehub.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.lifehub.ui.home.HomeScreen
import com.lifehub.ui.ledger.LedgerScreen
import com.lifehub.ui.habit.HabitScreen
import com.lifehub.ui.fitness.FitnessScreen
import com.lifehub.ui.schedule.ScheduleScreen
import com.lifehub.ui.wishlist.WishlistScreen
import com.lifehub.ui.media.MediaScreen
import com.lifehub.ui.settings.SettingsScreen

@Composable
fun LifeHubNavGraph(
    navController: NavHostController,
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Home.route,
        modifier = modifier
    ) {
        composable(Destination.Home.route) {
            HomeScreen(
                onNavigate = { route -> navController.navigate(route) },
                onExport = onExport,
                onImport = onImport,
                onOpenSettings = { navController.navigate("settings") }
            )
        }
        composable(Destination.Ledger.route) { LedgerScreen() }
        composable(Destination.Habit.route) { HabitScreen() }
        composable(Destination.Fitness.route) { FitnessScreen() }
        composable(Destination.Schedule.route) { ScheduleScreen() }
        composable(Destination.Wishlist.route) { WishlistScreen() }
        composable(Destination.Media.route) { MediaScreen() }
        composable(Destination.Settings.route) {
            SettingsScreen(
                onExport = onExport,
                onImport = onImport,
                onBack = {
                    navController.popBackStack()
                    if (navController.currentBackStackEntry == null) {
                        navController.navigate(Destination.Home.route)
                    }
                }
            )
        }
    }
}
