package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ExerciseSet
import com.example.model.MuscleGroup
import com.example.ui.components.RestTimerBanner
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GymOrange
import com.example.ui.theme.SkyBlueLight
import com.example.ui.theme.SkyBluePrimary
import com.example.viewmodel.ActiveExerciseUiState
import com.example.viewmodel.ActiveWorkoutViewModel
import com.example.viewmodel.WorkoutSummaryData
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    viewModel: ActiveWorkoutViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    todayRoutine: com.example.data.entity.Workout? = null,
    onStartTodayRoutine: () -> Unit = {},
    onQuickStart: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentWorkout by viewModel.currentWorkout.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsStateWithLifecycle()

    val restSecondsLeft by viewModel.restSecondsLeft.collectAsStateWithLifecycle()
    val restTotalDuration by viewModel.restTotalDuration.collectAsStateWithLifecycle()
    val isTimerRunning by viewModel.isTimerRunning.collectAsStateWithLifecycle()

    val finishedSummary by viewModel.workoutFinishedSummary.collectAsStateWithLifecycle()

    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }

    // Intercept back button to confirm exit
    BackHandler {
        if (currentWorkout != null) {
            showExitConfirmDialog = true
        } else {
            onNavigateBack()
        }
    }

    // Empty state when there is no active workout session
    if (currentWorkout == null) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Active Session", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("active_workout_empty_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                modifier = Modifier.size(42.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text(
                        text = "No active session",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "You are not currently in a workout session. Start today's planned routine or begin a quick open session.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (todayRoutine != null) {
                                onStartTodayRoutine()
                            } else {
                                onQuickStart()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_today_routine_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        val btnText = if (todayRoutine != null) {
                            "Start today's routine (${todayRoutine.name})"
                        } else {
                            "Start today's routine"
                        }
                        Text(btnText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    OutlinedButton(
                        onClick = onQuickStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("quick_start_active_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Quick Start", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
            }
        }
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentWorkout?.name ?: "Workout Session",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        // Elapsed stopwatch
                        val hours = elapsedSeconds / 3600
                        val minutes = (elapsedSeconds % 3600) / 60
                        val secs = elapsedSeconds % 60
                        val timeStr = if (hours > 0) {
                            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs)
                        } else {
                            String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
                        }
                        Text(
                            text = "Elapsed: $timeStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { showExitConfirmDialog = true },
                        modifier = Modifier.testTag("active_workout_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.finishWorkout {
                                // callback after finishing
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("finish_workout_button")
                    ) {
                        Text("Finish", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Built-in Countdown Rest Timer banner
            RestTimerBanner(
                secondsLeft = restSecondsLeft,
                totalDuration = restTotalDuration,
                isRunning = isTimerRunning,
                onPlayPause = {
                    if (isTimerRunning) viewModel.pauseTimer() else viewModel.resumeTimer(context)
                },
                onAdd15s = { viewModel.addRestTime(15) },
                onDismiss = { viewModel.dismissTimer() },
                onSelectPreset = { presetSec ->
                    viewModel.startRestTimer(context, presetSec)
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddExerciseDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_exercise_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Exercise")
            }
        }
    ) { paddingValues ->
        if (exercises.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "No Exercises in this Session",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap the '+' button below to add your first exercise and log sets, reps, and max weight lifted.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { showAddExerciseDialog = true },
                        modifier = Modifier.testTag("empty_add_exercise_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Exercise")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(exercises, key = { _, ex -> ex.exerciseLog.id }) { exIndex, item ->
                    ActiveExerciseCard(
                        exerciseState = item,
                        onAddSet = { viewModel.addSet(exIndex) },
                        onRemoveSet = { setIdx -> viewModel.removeSet(exIndex, setIdx) },
                        onUpdateSet = { setIdx, w, r -> viewModel.updateSet(exIndex, setIdx, w, r) },
                        onToggleDone = { setIdx -> viewModel.toggleSetCompleted(context, exIndex, setIdx) },
                        onRemoveExercise = { viewModel.removeExercise(exIndex) },
                        onStartTimerManually = {
                            viewModel.startRestTimer(context, item.exerciseLog.restTimeSeconds.takeIf { it > 0 } ?: 60)
                        },
                        onUpdateMaxKg = { newMax ->
                            viewModel.updateExerciseMaxKg(exIndex, newMax)
                        }
                    )
                }

                item {
                    OutlinedButton(
                        onClick = { showAddExerciseDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 48.dp)
                            .testTag("bottom_add_exercise_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Another Exercise")
                    }
                }
            }
        }
    }

    if (showAddExerciseDialog) {
        AddExerciseDialog(
            onDismiss = { showAddExerciseDialog = false },
            onConfirm = { name, muscle, restSec, defaultWeight ->
                viewModel.addExercise(name, muscle, restSec, defaultWeight)
                showAddExerciseDialog = false
            }
        )
    }

    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = { Text("Leave Workout?") },
            text = { Text("If no sets have been completed, this empty session will be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmDialog = false
                        viewModel.abandonWorkoutIfEmpty(
                            onDeleted = onNavigateBack,
                            onKept = onNavigateBack
                        )
                    }
                ) {
                    Text("Exit Session")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text("Continue Workout")
                }
            }
        )
    }

    // Workout Summary Dialog on Finish
    finishedSummary?.let { summary ->
        WorkoutFinishedDialog(
            summary = summary,
            onDismiss = {
                viewModel.dismissSummary()
                onNavigateBack()
            }
        )
    }
}

@Composable
fun ActiveExerciseCard(
    exerciseState: ActiveExerciseUiState,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onUpdateSet: (setIndex: Int, weight: Double, reps: Int) -> Unit,
    onToggleDone: (setIndex: Int) -> Unit,
    onRemoveExercise: () -> Unit,
    onStartTimerManually: () -> Unit,
    onUpdateMaxKg: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val log = exerciseState.exerciseLog
    val sets = exerciseState.sets
    var isEditingMaxKg by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("exercise_card_${log.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Muscle Tag, Title, Max Weight Badge & Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = log.targetMuscle,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Max Kg Lifted Badge (Personal Record, clickable to edit/update)
                        val maxDisplayVal = exerciseState.personalRecord ?: exerciseState.maxWeightKg.takeIf { it > 0.0 }
                        val maxDisplayText = if (maxDisplayVal != null && maxDisplayVal > 0.0) {
                            if (maxDisplayVal % 1.0 == 0.0) "Max: ${maxDisplayVal.toInt()} kg" else "Max: $maxDisplayVal kg"
                        } else {
                            "Max: -- kg"
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { isEditingMaxKg = true }
                                .testTag("max_kg_badge_${log.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = GymOrange
                                )
                                Text(
                                    text = maxDisplayText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = log.exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Best set of previous finished session
                    if (!exerciseState.lastSessionBestSet.isNullOrBlank()) {
                        Text(
                            text = "Last: ${exerciseState.lastSessionBestSet}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Target rep range / notes if specified
                    if (log.notes.isNotBlank()) {
                        val notesDisplay = if (log.notes.contains("rest", ignoreCase = true) || log.notes.contains("min", ignoreCase = true)) {
                            log.notes
                        } else {
                            "${log.notes} • rest ${log.restTimeSeconds}s"
                        }
                        Text(
                            text = notesDisplay,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Manual rest timer button for this exercise
                    IconButton(
                        onClick = onStartTimerManually,
                        modifier = Modifier.testTag("start_exercise_timer_${log.id}")
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = "Start Rest Timer",
                            tint = SkyBluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onRemoveExercise,
                        modifier = Modifier.testTag("delete_exercise_${log.id}")
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove Exercise",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Sets Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SET",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "WEIGHT (KG)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "REPS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "DONE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(48.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Sets Rows
            sets.forEachIndexed { setIdx, set ->
                SetRow(
                    set = set,
                    onUpdateWeight = { newW -> onUpdateSet(setIdx, newW, set.reps) },
                    onUpdateReps = { newR -> onUpdateSet(setIdx, set.weightKg, newR) },
                    onToggleDone = { onToggleDone(setIdx) },
                    onDeleteSet = { onRemoveSet(setIdx) },
                    canDelete = sets.size > 1
                )
            }

            // Add Set Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onAddSet,
                    modifier = Modifier.testTag("add_set_button_${log.id}")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Set", fontWeight = FontWeight.SemiBold)
                }

                Text(
                    text = "Rest: ${log.restTimeSeconds}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (isEditingMaxKg) {
        var tempMaxKg by remember { mutableStateOf(if (exerciseState.maxWeightKg > 0.0) exerciseState.maxWeightKg.toString() else "") }
        AlertDialog(
            onDismissRequest = { isEditingMaxKg = false },
            title = { Text("Update Max Weight (Kg)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Set your personal best / maximum lifted weight for ${log.exerciseName}:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = tempMaxKg,
                        onValueChange = { tempMaxKg = it },
                        label = { Text("Max Kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_max_kg_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = tempMaxKg.toDoubleOrNull() ?: 0.0
                        onUpdateMaxKg(parsed)
                        isEditingMaxKg = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditingMaxKg = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SetRow(
    set: ExerciseSet,
    onUpdateWeight: (Double) -> Unit,
    onUpdateReps: (Int) -> Unit,
    onToggleDone: () -> Unit,
    onDeleteSet: () -> Unit,
    canDelete: Boolean
) {
    var isEditingWeight by remember { mutableStateOf(false) }
    var isEditingReps by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (set.isCompleted) EmeraldSuccess.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .padding(vertical = 4.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set number badge
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (set.isCompleted) EmeraldSuccess
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${set.setNumber}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (set.isCompleted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Weight Stepper (Kg)
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = { onUpdateWeight((set.weightKg - 2.5).coerceAtLeast(0.0)) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "-2.5kg", modifier = Modifier.size(16.dp))
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isEditingWeight = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${set.weightKg}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = { onUpdateWeight(set.weightKg + 2.5) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "+2.5kg", modifier = Modifier.size(16.dp))
            }
        }

        // Reps Stepper
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = { onUpdateReps((set.reps - 1).coerceAtLeast(1)) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "-1 rep", modifier = Modifier.size(16.dp))
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isEditingReps = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${set.reps}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = { onUpdateReps(set.reps + 1) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "+1 rep", modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Checkbox button for set completion
        IconButton(
            onClick = onToggleDone,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (set.isCompleted) EmeraldSuccess
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .testTag("set_checkbox_${set.setNumber}")
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = if (set.isCompleted) "Completed" else "Mark Complete",
                tint = if (set.isCompleted) Color.White else MaterialTheme.colorScheme.outline
            )
        }
    }

    // Weight direct input dialog
    if (isEditingWeight) {
        var tempWeight by remember { mutableStateOf(set.weightKg.toString()) }
        AlertDialog(
            onDismissRequest = { isEditingWeight = false },
            title = { Text("Enter Weight (kg)") },
            text = {
                OutlinedTextField(
                    value = tempWeight,
                    onValueChange = { tempWeight = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = tempWeight.toDoubleOrNull() ?: set.weightKg
                        onUpdateWeight(parsed)
                        isEditingWeight = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditingWeight = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reps direct input dialog
    if (isEditingReps) {
        var tempReps by remember { mutableStateOf(set.reps.toString()) }
        AlertDialog(
            onDismissRequest = { isEditingReps = false },
            title = { Text("Enter Reps") },
            text = {
                OutlinedTextField(
                    value = tempReps,
                    onValueChange = { tempReps = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = tempReps.toIntOrNull() ?: set.reps
                        onUpdateReps(parsed)
                        isEditingReps = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditingReps = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, muscle: MuscleGroup, restSec: Int, defaultWeight: Double) -> Unit
) {
    var exerciseName by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf(MuscleGroup.CHEST) }
    var restSeconds by remember { mutableIntStateOf(60) }
    var startingWeight by remember { mutableDoubleStateOf(40.0) }
    var expandedMuscle by remember { mutableStateOf(false) }

    val commonPresets = when (selectedMuscle) {
        MuscleGroup.CHEST -> listOf("Barbell Bench Press", "Incline Dumbbell Press", "Chest Cable Flyes", "Push-ups")
        MuscleGroup.BACK -> listOf("Barbell Deadlift", "Barbell Row", "Lat Pulldown", "Pull-ups", "Seated Cable Row")
        MuscleGroup.LEGS -> listOf("Barbell Back Squat", "Leg Press", "Romanian Deadlift", "Leg Extension", "Calf Raise")
        MuscleGroup.SHOULDERS -> listOf("Overhead Press", "Dumbbell Lateral Raise", "Face Pulls", "Arnold Press")
        MuscleGroup.ARMS -> listOf("Barbell Bicep Curl", "Triceps Skull Crushers", "Hammer Curls", "Tricep Pushdown")
        MuscleGroup.CORE -> listOf("Cable Woodchopper", "Hanging Leg Raise", "Ab Wheel Rollout", "Plank")
        else -> listOf("Clean and Press", "Kettlebell Swing", "Burpees", "Thruster")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Exercise to Session", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Muscle Group Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedMuscle,
                    onExpandedChange = { expandedMuscle = it }
                ) {
                    OutlinedTextField(
                        value = selectedMuscle.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Muscle") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMuscle) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMuscle,
                        onDismissRequest = { expandedMuscle = false }
                    ) {
                        MuscleGroup.entries.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.displayName) },
                                onClick = {
                                    selectedMuscle = group
                                    expandedMuscle = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = exerciseName,
                    onValueChange = { exerciseName = it },
                    label = { Text("Exercise Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exercise_name_input")
                )

                // Quick suggestions
                Text(
                    text = "Suggestions:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    commonPresets.take(2).forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { exerciseName = preset }
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Rest Time Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rest Timer: ${restSeconds}s",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(45, 60, 90, 120).forEach { s ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (restSeconds == s) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (restSeconds == s) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { restSeconds = s }
                            ) {
                                Text(
                                    text = "${s}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (exerciseName.isNotBlank()) {
                        onConfirm(exerciseName.trim(), selectedMuscle, restSeconds, startingWeight)
                    }
                },
                enabled = exerciseName.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_exercise_button")
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun WorkoutFinishedDialog(
    summary: WorkoutSummaryData,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = GymOrange,
                    modifier = Modifier.size(28.dp)
                )
                Text("Workout Complete!", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Awesome work! Here is your session summary:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Duration:", fontWeight = FontWeight.Medium)
                            Text("${summary.durationMinutes} minutes", fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Volume Lifted:", fontWeight = FontWeight.Medium)
                            Text(String.format(Locale.getDefault(), "%,.1f kg", summary.totalVolumeKg), fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Completed Sets:", fontWeight = FontWeight.Medium)
                            Text("${summary.totalSetsCompleted} sets", fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Exercises Trained:", fontWeight = FontWeight.Medium)
                            Text("${summary.exercisesCompleted}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("summary_done_button")
            ) {
                Text("Great, Close")
            }
        }
    )
}
