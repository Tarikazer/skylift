package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.AppDatabase
import com.example.data.repository.BackupRepository
import com.example.data.repository.BodyWeightRepository
import com.example.data.repository.WorkoutRepository
import com.example.ui.navigation.Screen
import com.example.ui.screens.ActiveWorkoutScreen
import com.example.ui.screens.BodyWeightScreen
import com.example.ui.screens.DailyWorkoutScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.SkyLiftTheme
import com.example.viewmodel.ActiveWorkoutViewModel
import com.example.viewmodel.ActiveWorkoutViewModelFactory
import com.example.viewmodel.BodyWeightViewModel
import com.example.viewmodel.BodyWeightViewModelFactory
import com.example.viewmodel.SettingsViewModel
import com.example.viewmodel.SettingsViewModelFactory
import com.example.viewmodel.WorkoutViewModel
import com.example.viewmodel.WorkoutViewModelFactory

class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getInstance(applicationContext) }
    private val workoutRepository by lazy {
        WorkoutRepository(database.workoutDao(), database.exerciseLogDao())
    }
    private val bodyWeightRepository by lazy {
        BodyWeightRepository(database.bodyWeightLogDao())
    }
    private val backupRepository by lazy {
        BackupRepository(
            applicationContext,
            database.workoutDao(),
            database.exerciseLogDao(),
            database.bodyWeightLogDao()
        )
    }

    private val workoutViewModel: WorkoutViewModel by viewModels {
        WorkoutViewModelFactory(workoutRepository)
    }
    private val activeWorkoutViewModel: ActiveWorkoutViewModel by viewModels {
        ActiveWorkoutViewModelFactory(workoutRepository)
    }
    private val bodyWeightViewModel: BodyWeightViewModel by viewModels {
        BodyWeightViewModelFactory(bodyWeightRepository)
    }
    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(backupRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            SkyLiftTheme {
                SkyLiftApp(
                    workoutViewModel = workoutViewModel,
                    activeWorkoutViewModel = activeWorkoutViewModel,
                    bodyWeightViewModel = bodyWeightViewModel,
                    settingsViewModel = settingsViewModel,
                    backupRepository = backupRepository
                )
            }
        }
    }
}

@Composable
fun SkyLiftApp(
    workoutViewModel: WorkoutViewModel,
    activeWorkoutViewModel: ActiveWorkoutViewModel,
    bodyWeightViewModel: BodyWeightViewModel,
    settingsViewModel: SettingsViewModel,
    backupRepository: BackupRepository
) {
    var currentScreenRoute by rememberSaveable { mutableStateOf(Screen.DailyWorkout.route) }

    val activeWorkout by activeWorkoutViewModel.currentWorkout.collectAsStateWithLifecycle()
    val isSessionActive = activeWorkout != null
    val todaysRoutine by workoutViewModel.todaysRoutine.collectAsStateWithLifecycle()

    val navItems = listOf(
        Screen.DailyWorkout,
        Screen.ActiveWorkout,
        Screen.BodyWeight,
        Screen.Settings
    )

    // Handle back button when not on Daily Workout home screen
    if (currentScreenRoute != Screen.DailyWorkout.route && currentScreenRoute != Screen.ActiveWorkout.route) {
        BackHandler {
            currentScreenRoute = Screen.DailyWorkout.route
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Keep navigation bar visible across all screens
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("main_bottom_nav_bar")
            ) {
                navItems.forEach { screen ->
                    val isSelected = currentScreenRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            // Do not auto-start a quick workout when tapping the tab
                            currentScreenRoute = screen.route
                        },
                        icon = {
                            if (screen == Screen.ActiveWorkout && isSessionActive) {
                                BadgedBox(badge = { Badge { Text("●") } }) {
                                    Icon(screen.icon, contentDescription = screen.title)
                                }
                            } else {
                                Icon(screen.icon, contentDescription = screen.title)
                            }
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreenRoute) {
                Screen.DailyWorkout.route -> {
                    DailyWorkoutScreen(
                        viewModel = workoutViewModel,
                        onStartWorkout = { template ->
                            activeWorkoutViewModel.startFromTemplate(template)
                            currentScreenRoute = Screen.ActiveWorkout.route
                        },
                        onQuickStart = {
                            activeWorkoutViewModel.startNewQuickWorkout()
                            currentScreenRoute = Screen.ActiveWorkout.route
                        },
                        onLoadUserProgram = {
                            backupRepository.loadCustomUserProgram()
                        }
                    )
                }

                Screen.ActiveWorkout.route -> {
                    ActiveWorkoutScreen(
                        viewModel = activeWorkoutViewModel,
                        todayRoutine = todaysRoutine,
                        onStartTodayRoutine = {
                            val routine = todaysRoutine
                            if (routine != null) {
                                activeWorkoutViewModel.startFromTemplate(routine)
                            }
                        },
                        onQuickStart = {
                            activeWorkoutViewModel.startNewQuickWorkout()
                        },
                        onNavigateBack = {
                            currentScreenRoute = Screen.DailyWorkout.route
                        }
                    )
                }

                Screen.BodyWeight.route -> {
                    BodyWeightScreen(viewModel = bodyWeightViewModel)
                }

                Screen.Settings.route -> {
                    SettingsScreen(viewModel = settingsViewModel)
                }
            }
        }
    }
}
