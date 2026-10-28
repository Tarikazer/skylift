package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.data.entity.ExerciseLog
import com.example.model.ExerciseSet
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.Workout
import com.example.model.MuscleGroup
import com.example.ui.theme.SkyBlueLight
import com.example.ui.theme.SkyBluePrimary
import com.example.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyWorkoutScreen(
    viewModel: WorkoutViewModel,
    onStartWorkout: (Workout) -> Unit,
    onQuickStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val routineTemplates by viewModel.routineTemplates.collectAsStateWithLifecycle()
    val completedWorkouts by viewModel.completedWorkouts.collectAsStateWithLifecycle()
    val todaysRoutine by viewModel.todaysRoutine.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var routineToEdit by remember { mutableStateOf<Workout?>(null) }
    var exercisesForRoutineToEdit by remember { mutableStateOf<List<ExerciseLog>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_routine_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Routine")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val todayDate = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
                        Text(
                            text = todayDate,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Workout Schedule",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    OutlinedButton(
                        onClick = onQuickStart,
                        modifier = Modifier.testTag("quick_start_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Quick Start", fontSize = 12.sp)
                    }
                }
            }

            // Weekly Schedule Strip
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Weekly Split",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WorkoutViewModel.DAYS_OF_WEEK.forEach { day ->
                            val isSelected = day.equals(selectedDay, ignoreCase = true)
                            val routineForDay = routineTemplates.firstOrNull { it.dayOfWeek.equals(day, ignoreCase = true) }
                            val shortDay = day.take(3)

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { viewModel.setSelectedDay(day) }
                                    .testTag("day_chip_$day")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = shortDay,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (routineForDay != null) {
                                                    if (isSelected) Color.White else MaterialTheme.colorScheme.primary
                                                } else Color.Transparent
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Scheduled Routine Card for Selected Day
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("today_routine_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "$selectedDay Focus",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            if (todaysRoutine != null) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (todaysRoutine != null) {
                            val r = todaysRoutine!!
                            Text(
                                text = r.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (r.targetMuscles.isNotBlank()) {
                                Text(
                                    text = "Target Muscles: ${r.targetMuscles}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { onStartWorkout(r) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("start_scheduled_workout_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Start Routine", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            exercisesForRoutineToEdit = viewModel.getExercisesForRoutine(r.id)
                                            routineToEdit = r
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("edit_today_routine_button")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Routine")
                                }
                            }
                        } else {
                            Text(
                                text = "Rest Day or No Scheduled Routine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Take time to recover, or create a workout routine for $selectedDay.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Button(
                                onClick = { showCreateDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("schedule_routine_for_day_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Schedule Routine for $selectedDay")
                            }
                        }
                    }
                }
            }

            // All Routine Templates
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Workout Routines (${routineTemplates.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (routineTemplates.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "No Workout Routines Yet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Create your custom split (Push, Pull, Legs) or tap Settings to load starter routines!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(routineTemplates, key = { it.id }) { template ->
                    RoutineTemplateCard(
                        workout = template,
                        onStart = { onStartWorkout(template) },
                        onEdit = {
                            coroutineScope.launch {
                                exercisesForRoutineToEdit = viewModel.getExercisesForRoutine(template.id)
                                routineToEdit = template
                            }
                        },
                        onDelete = { viewModel.deleteWorkout(template) }
                    )
                }
            }

            // Workout History Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Recent Workout History (${completedWorkouts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (completedWorkouts.isEmpty()) {
                item {
                    Text(
                        text = "Completed workouts will show up here after you finish a session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(completedWorkouts.take(10), key = { it.id }) { historyItem ->
                    CompletedWorkoutHistoryCard(
                        workout = historyItem,
                        onDelete = { viewModel.deleteWorkout(historyItem) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateRoutineDialog(
            defaultDay = selectedDay,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, day, muscles ->
                viewModel.createRoutine(name, day, muscles)
                showCreateDialog = false
            }
        )
    }

    routineToEdit?.let { routine ->
        EditRoutineDialog(
            routine = routine,
            initialExercises = exercisesForRoutineToEdit,
            onDismiss = {
                routineToEdit = null
                exercisesForRoutineToEdit = emptyList()
            },
            onSave = { updatedWorkout, updatedExercises ->
                viewModel.saveRoutineWithExercises(updatedWorkout, updatedExercises)
                routineToEdit = null
                exercisesForRoutineToEdit = emptyList()
            }
        )
    }
}

@Composable
fun RoutineTemplateCard(
    workout: Workout,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("routine_card_${workout.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = workout.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (workout.dayOfWeek.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = workout.dayOfWeek,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (workout.targetMuscles.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = workout.targetMuscles,
                        style = MaterialTheme.typography.bodySmall,
                        color = SkyBluePrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (workout.lastPerformedAt != null && workout.lastPerformedAt > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    val dateFormatted = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(workout.lastPerformedAt))
                    Text(
                        text = "Last trained: $dateFormatted",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_routine_${workout.id}")
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit routine",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_routine_${workout.id}")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete routine",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }

                Button(
                    onClick = onStart,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("start_routine_${workout.id}")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CompletedWorkoutHistoryCard(
    workout: Workout,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = workout.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    val dateStr = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(workout.createdAt))
                    Text(
                        text = "$dateStr • ${workout.durationMinutes} mins",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete workout",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoutineDialog(
    defaultDay: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, dayOfWeek: String, targetMuscles: String) -> Unit
) {
    var routineName by remember { mutableStateOf("") }
    var selectedDay by remember { mutableStateOf(defaultDay) }
    var targetMuscles by remember { mutableStateOf("") }

    val presetMuscles = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Full Body")
    val selectedMuscles = remember { mutableStateOf(setOf<String>()) }

    var expandedDayDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Workout Routine", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = routineName,
                    onValueChange = { routineName = it },
                    label = { Text("Routine Name (e.g. Push Power)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("routine_name_input")
                )

                // Day of Week Picker
                ExposedDropdownMenuBox(
                    expanded = expandedDayDropdown,
                    onExpandedChange = { expandedDayDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedDay,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Scheduled Day") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDayDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDayDropdown,
                        onDismissRequest = { expandedDayDropdown = false }
                    ) {
                        WorkoutViewModel.DAYS_OF_WEEK.forEach { day ->
                            DropdownMenuItem(
                                text = { Text(day) },
                                onClick = {
                                    selectedDay = day
                                    expandedDayDropdown = false
                                }
                            )
                        }
                    }
                }

                // Target Muscle group chips
                Text(
                    text = "Target Muscles",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetMuscles.forEach { muscle ->
                        val isChecked = selectedMuscles.value.contains(muscle)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isChecked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val newSet = selectedMuscles.value.toMutableSet()
                                    if (isChecked) newSet.remove(muscle) else newSet.add(muscle)
                                    selectedMuscles.value = newSet
                                    targetMuscles = newSet.joinToString(", ")
                                }
                        ) {
                            Text(
                                text = muscle,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (routineName.isNotBlank()) {
                        val finalMuscles = if (targetMuscles.isNotBlank()) targetMuscles else selectedMuscles.value.joinToString(", ")
                        onConfirm(routineName.trim(), selectedDay, finalMuscles)
                    }
                },
                enabled = routineName.isNotBlank(),
                modifier = Modifier.testTag("save_routine_button")
            ) {
                Text("Save Routine")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

data class EditableExerciseItem(
    val id: Long = 0,
    val name: String = "",
    val targetMuscle: String = "Legs",
    val setsCount: Int = 3,
    val repsRange: String = "8-12",
    val restTimeSeconds: Int = 60,
    val maxKgText: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRoutineDialog(
    routine: Workout,
    initialExercises: List<ExerciseLog>,
    onDismiss: () -> Unit,
    onSave: (Workout, List<ExerciseLog>) -> Unit
) {
    var routineName by remember { mutableStateOf(routine.name) }
    var selectedDay by remember { mutableStateOf(routine.dayOfWeek.ifBlank { "None" }) }
    var targetMuscles by remember { mutableStateOf(routine.targetMuscles) }
    var expandedDayDropdown by remember { mutableStateOf(false) }

    val exerciseItems = remember {
        val list = if (initialExercises.isNotEmpty()) {
            initialExercises.map { ex ->
                EditableExerciseItem(
                    id = ex.id,
                    name = ex.exerciseName,
                    targetMuscle = ex.targetMuscle,
                    setsCount = ex.setsCount,
                    repsRange = ex.notes.ifBlank { "${ex.reps}" },
                    restTimeSeconds = ex.restTimeSeconds,
                    maxKgText = if (ex.maxKg > 0.0) "${ex.maxKg}" else ""
                )
            }.toMutableList()
        } else {
            mutableListOf(
                EditableExerciseItem(name = "Exercise 1", targetMuscle = "Legs", setsCount = 4, repsRange = "8-10", restTimeSeconds = 90, maxKgText = "")
            )
        }
        androidx.compose.runtime.mutableStateListOf(*list.toTypedArray())
    }

    val daysOptions = listOf("None") + WorkoutViewModel.DAYS_OF_WEEK

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit Routine", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = routineName,
                        onValueChange = { routineName = it },
                        label = { Text("Routine Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_routine_name_input")
                    )
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = expandedDayDropdown,
                        onExpandedChange = { expandedDayDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = if (selectedDay == "None") "None (Rest Day / Floating)" else selectedDay,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assigned Weekly Split Day") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDayDropdown) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDayDropdown,
                            onDismissRequest = { expandedDayDropdown = false }
                        ) {
                            daysOptions.forEach { day ->
                                DropdownMenuItem(
                                    text = { Text(if (day == "None") "None (Rest Day / Floating)" else day) },
                                    onClick = {
                                        selectedDay = day
                                        expandedDayDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = targetMuscles,
                        onValueChange = { targetMuscles = it },
                        label = { Text("Target Muscles (e.g. Legs, Core)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Exercises (${exerciseItems.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = {
                                exerciseItems.add(
                                    EditableExerciseItem(
                                        name = "",
                                        targetMuscle = "Legs",
                                        setsCount = 3,
                                        repsRange = "10-12",
                                        restTimeSeconds = 60,
                                        maxKgText = ""
                                    )
                                )
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Exercise")
                        }
                    }
                }

                itemsIndexed(exerciseItems) { index, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Header with reorder controls and delete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Move Up
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val prev = exerciseItems[index - 1]
                                                exerciseItems[index - 1] = item
                                                exerciseItems[index] = prev
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                                    }

                                    // Move Down
                                    IconButton(
                                        onClick = {
                                            if (index < exerciseItems.lastIndex) {
                                                val next = exerciseItems[index + 1]
                                                exerciseItems[index + 1] = item
                                                exerciseItems[index] = next
                                            }
                                        },
                                        enabled = index < exerciseItems.lastIndex,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                                    }

                                    // Delete
                                    IconButton(
                                        onClick = {
                                            if (exerciseItems.size > 1) {
                                                exerciseItems.removeAt(index)
                                            }
                                        },
                                        enabled = exerciseItems.size > 1,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            // Exercise Name
                            OutlinedTextField(
                                value = item.name,
                                onValueChange = { newName ->
                                    exerciseItems[index] = item.copy(name = newName)
                                },
                                label = { Text("Exercise Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Row: Target Muscle & Sets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = item.targetMuscle,
                                    onValueChange = { newMuscle ->
                                        exerciseItems[index] = item.copy(targetMuscle = newMuscle)
                                    },
                                    label = { Text("Muscle") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = "${item.setsCount}",
                                    onValueChange = { newSets ->
                                        val s = newSets.toIntOrNull() ?: item.setsCount
                                        exerciseItems[index] = item.copy(setsCount = s)
                                    },
                                    label = { Text("Sets") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(0.7f)
                                )
                            }

                            // Row: Reps Range & Rest Time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = item.repsRange,
                                    onValueChange = { newReps ->
                                        exerciseItems[index] = item.copy(repsRange = newReps)
                                    },
                                    label = { Text("Reps / Range (e.g. 6-10)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = "${item.restTimeSeconds}",
                                    onValueChange = { newRest ->
                                        val r = newRest.toIntOrNull() ?: item.restTimeSeconds
                                        exerciseItems[index] = item.copy(restTimeSeconds = r)
                                    },
                                    label = { Text("Rest (s)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(0.7f)
                                )
                            }

                            // Max Kg field (empty at first, fully editable)
                            OutlinedTextField(
                                value = item.maxKgText,
                                onValueChange = { newMax ->
                                    exerciseItems[index] = item.copy(maxKgText = newMax)
                                },
                                label = { Text("Max Kg (empty / PR)") },
                                placeholder = { Text("e.g. 100") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (routineName.isNotBlank() && exerciseItems.isNotEmpty()) {
                        val finalMuscles = targetMuscles.ifBlank {
                            exerciseItems.map { it.targetMuscle }.distinct().joinToString(", ")
                        }
                        val updatedRoutine = routine.copy(
                            name = routineName.trim(),
                            dayOfWeek = if (selectedDay == "None") "" else selectedDay,
                            targetMuscles = finalMuscles
                        )

                        val updatedExercises = exerciseItems.mapIndexed { idx, ee ->
                            val parsedMax = ee.maxKgText.toDoubleOrNull() ?: 0.0
                            val parsedReps = ee.repsRange.filter { it.isDigit() }.toIntOrNull() ?: 10
                            val sets = (1..ee.setsCount.coerceAtLeast(1)).map { sNum ->
                                ExerciseSet(
                                    setNumber = sNum,
                                    weightKg = parsedMax,
                                    reps = parsedReps,
                                    isCompleted = false
                                )
                            }
                            ExerciseLog(
                                id = ee.id,
                                workoutId = routine.id,
                                exerciseName = ee.name.ifBlank { "Exercise ${idx + 1}" },
                                targetMuscle = ee.targetMuscle.ifBlank { "Full Body" },
                                setsCount = ee.setsCount.coerceAtLeast(1),
                                reps = parsedReps,
                                maxKg = parsedMax,
                                restTimeSeconds = ee.restTimeSeconds,
                                setsDataJson = ExerciseSet.listToJsonString(sets),
                                notes = ee.repsRange
                            )
                        }

                        onSave(updatedRoutine, updatedExercises)
                    }
                },
                enabled = routineName.isNotBlank(),
                modifier = Modifier.testTag("save_edited_routine_button")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
