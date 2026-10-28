package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object DailyWorkout : Screen("daily_workout", "Workouts", Icons.Default.CalendarToday)
    data object ActiveWorkout : Screen("active_workout", "Active Session", Icons.Default.FitnessCenter)
    data object BodyWeight : Screen("body_weight", "Bodyweight", Icons.Default.MonitorWeight)
    data object Settings : Screen("settings", "Settings & Backup", Icons.Default.Settings)
}
