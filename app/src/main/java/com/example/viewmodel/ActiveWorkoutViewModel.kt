package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.entity.ExerciseLog
import com.example.data.entity.Workout
import com.example.data.repository.WorkoutRepository
import com.example.model.ExerciseSet
import com.example.model.MuscleGroup
import com.example.ui.components.TimerFeedbackHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActiveExerciseUiState(
    val exerciseLog: ExerciseLog,
    val sets: List<ExerciseSet>,
    val maxWeightKg: Double,
    val personalRecord: Double? = null
)

data class WorkoutSummaryData(
    val workoutName: String,
    val durationMinutes: Int,
    val totalVolumeKg: Double,
    val totalSetsCompleted: Int,
    val exercisesCompleted: Int
)

class ActiveWorkoutViewModel(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _currentWorkout = MutableStateFlow<Workout?>(null)
    val currentWorkout: StateFlow<Workout?> = _currentWorkout.asStateFlow()

    private val _exercises = MutableStateFlow<List<ActiveExerciseUiState>>(emptyList())
    val exercises: StateFlow<List<ActiveExerciseUiState>> = _exercises.asStateFlow()

    private val _workoutStartTime = MutableStateFlow(System.currentTimeMillis())
    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    // Countdown Rest Timer state
    private val _restSecondsLeft = MutableStateFlow(0)
    val restSecondsLeft: StateFlow<Int> = _restSecondsLeft.asStateFlow()

    private val _restTotalDuration = MutableStateFlow(60)
    val restTotalDuration: StateFlow<Int> = _restTotalDuration.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _autoStartTimerOnCheck = MutableStateFlow(true)
    val autoStartTimerOnCheck: StateFlow<Boolean> = _autoStartTimerOnCheck.asStateFlow()

    private val _workoutFinishedSummary = MutableStateFlow<WorkoutSummaryData?>(null)
    val workoutFinishedSummary: StateFlow<WorkoutSummaryData?> = _workoutFinishedSummary.asStateFlow()

    private var timerJob: Job? = null
    private var workoutTimerJob: Job? = null

    init {
        startSessionStopwatch()
    }

    private fun startSessionStopwatch() {
        workoutTimerJob?.cancel()
        workoutTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val diff = (System.currentTimeMillis() - _workoutStartTime.value) / 1000
                _elapsedSeconds.value = diff.toInt()
            }
        }
    }

    fun loadWorkout(workoutId: Long) {
        viewModelScope.launch {
            val workout = workoutRepository.getWorkoutByIdOnce(workoutId)
            _currentWorkout.value = workout
            _workoutStartTime.value = System.currentTimeMillis()
            loadExercisesForWorkout(workoutId)
        }
    }

    fun startNewQuickWorkout(name: String = "Quick Workout", targetMuscles: String = "Full Body") {
        viewModelScope.launch {
            val workout = Workout(
                name = name,
                dayOfWeek = WorkoutViewModel.getCurrentDayOfWeek(),
                targetMuscles = targetMuscles,
                createdAt = System.currentTimeMillis(),
                isRoutineTemplate = false
            )
            val id = workoutRepository.insertWorkout(workout)
            loadWorkout(id)
        }
    }

    fun startFromTemplate(template: Workout) {
        viewModelScope.launch {
            val (newWorkoutId, _) = workoutRepository.startWorkoutFromTemplate(template)
            loadWorkout(newWorkoutId)
        }
    }

    private suspend fun loadExercisesForWorkout(workoutId: Long) {
        val rawLogs = workoutRepository.getExercisesForWorkoutOnce(workoutId)
        val uiStates = rawLogs.map { log ->
            val sets = ExerciseSet.listFromJsonString(log.setsDataJson).ifEmpty {
                // Initialize default 3 sets if empty
                listOf(
                    ExerciseSet(1, log.maxKg.coerceAtLeast(20.0), log.reps.coerceAtLeast(10), false),
                    ExerciseSet(2, log.maxKg.coerceAtLeast(20.0), log.reps.coerceAtLeast(10), false),
                    ExerciseSet(3, log.maxKg.coerceAtLeast(20.0), log.reps.coerceAtLeast(10), false)
                )
            }
            val maxKg = sets.filter { it.isCompleted }.maxOfOrNull { it.weightKg } ?: log.maxKg
            ActiveExerciseUiState(
                exerciseLog = log,
                sets = sets,
                maxWeightKg = maxKg
            )
        }
        _exercises.value = uiStates
    }

    fun addExercise(name: String, muscle: MuscleGroup, defaultRestSeconds: Int = 60, defaultWeight: Double = 40.0) {
        val workout = _currentWorkout.value ?: return
        viewModelScope.launch {
            val initialSets = listOf(
                ExerciseSet(1, defaultWeight, 10, false),
                ExerciseSet(2, defaultWeight, 10, false),
                ExerciseSet(3, defaultWeight, 10, false)
            )
            val log = ExerciseLog(
                workoutId = workout.id,
                exerciseName = name,
                targetMuscle = muscle.displayName,
                setsCount = initialSets.size,
                reps = 10,
                maxKg = defaultWeight,
                restTimeSeconds = defaultRestSeconds,
                setsDataJson = ExerciseSet.listToJsonString(initialSets),
                notes = ""
            )
            val newId = workoutRepository.insertExerciseLog(log)
            loadExercisesForWorkout(workout.id)
        }
    }

    fun updateSet(exerciseIndex: Int, setIndex: Int, newWeight: Double, newReps: Int) {
        val currentList = _exercises.value.toMutableList()
        if (exerciseIndex !in currentList.indices) return

        val exercise = currentList[exerciseIndex]
        val setList = exercise.sets.toMutableList()
        if (setIndex !in setList.indices) return

        val oldSet = setList[setIndex]
        setList[setIndex] = oldSet.copy(
            weightKg = newWeight.coerceAtLeast(0.0),
            reps = newReps.coerceAtLeast(1)
        )

        val updatedMaxKg = setList.filter { it.isCompleted }.maxOfOrNull { it.weightKg }
            ?: setList.maxOfOrNull { it.weightKg } ?: 0.0

        val updatedLog = exercise.exerciseLog.copy(
            setsDataJson = ExerciseSet.listToJsonString(setList),
            maxKg = updatedMaxKg,
            setsCount = setList.size
        )

        currentList[exerciseIndex] = exercise.copy(
            exerciseLog = updatedLog,
            sets = setList,
            maxWeightKg = updatedMaxKg
        )
        _exercises.value = currentList

        viewModelScope.launch {
            workoutRepository.updateExerciseLog(updatedLog)
        }
    }

    fun updateExerciseMaxKg(exerciseIndex: Int, newMaxKg: Double) {
        val currentList = _exercises.value.toMutableList()
        if (exerciseIndex !in currentList.indices) return

        val exercise = currentList[exerciseIndex]
        val updatedLog = exercise.exerciseLog.copy(maxKg = newMaxKg.coerceAtLeast(0.0))
        currentList[exerciseIndex] = exercise.copy(
            exerciseLog = updatedLog,
            maxWeightKg = newMaxKg.coerceAtLeast(0.0)
        )
        _exercises.value = currentList

        viewModelScope.launch {
            workoutRepository.updateExerciseLog(updatedLog)
        }
    }

    fun toggleSetCompleted(context: Context, exerciseIndex: Int, setIndex: Int) {
        val currentList = _exercises.value.toMutableList()
        if (exerciseIndex !in currentList.indices) return

        val exercise = currentList[exerciseIndex]
        val setList = exercise.sets.toMutableList()
        if (setIndex !in setList.indices) return

        val oldSet = setList[setIndex]
        val newlyCompleted = !oldSet.isCompleted
        setList[setIndex] = oldSet.copy(isCompleted = newlyCompleted)

        val updatedMaxKg = setList.filter { it.isCompleted }.maxOfOrNull { it.weightKg }
            ?: setList.maxOfOrNull { it.weightKg } ?: 0.0

        val updatedLog = exercise.exerciseLog.copy(
            setsDataJson = ExerciseSet.listToJsonString(setList),
            maxKg = updatedMaxKg
        )

        currentList[exerciseIndex] = exercise.copy(
            exerciseLog = updatedLog,
            sets = setList,
            maxWeightKg = updatedMaxKg
        )
        _exercises.value = currentList

        viewModelScope.launch {
            workoutRepository.updateExerciseLog(updatedLog)
        }

        // Trigger countdown rest timer automatically if set was completed!
        if (newlyCompleted && _autoStartTimerOnCheck.value) {
            val restTime = exercise.exerciseLog.restTimeSeconds.takeIf { it > 0 } ?: 60
            startRestTimer(context, restTime)
        }
    }

    fun addSet(exerciseIndex: Int) {
        val currentList = _exercises.value.toMutableList()
        if (exerciseIndex !in currentList.indices) return

        val exercise = currentList[exerciseIndex]
        val setList = exercise.sets.toMutableList()
        val lastSet = setList.lastOrNull()
        val nextWeight = lastSet?.weightKg ?: 20.0
        val nextReps = lastSet?.reps ?: 10

        setList.add(ExerciseSet(setList.size + 1, nextWeight, nextReps, false))

        val updatedLog = exercise.exerciseLog.copy(
            setsCount = setList.size,
            setsDataJson = ExerciseSet.listToJsonString(setList)
        )

        currentList[exerciseIndex] = exercise.copy(
            exerciseLog = updatedLog,
            sets = setList
        )
        _exercises.value = currentList

        viewModelScope.launch {
            workoutRepository.updateExerciseLog(updatedLog)
        }
    }

    fun removeSet(exerciseIndex: Int, setIndex: Int) {
        val currentList = _exercises.value.toMutableList()
        if (exerciseIndex !in currentList.indices) return

        val exercise = currentList[exerciseIndex]
        val setList = exercise.sets.toMutableList()
        if (setList.size <= 1) return // Keep at least 1 set
        if (setIndex !in setList.indices) return

        setList.removeAt(setIndex)
        // Renumber sets
        val renumbered = setList.mapIndexed { idx, s -> s.copy(setNumber = idx + 1) }

        val updatedMaxKg = renumbered.filter { it.isCompleted }.maxOfOrNull { it.weightKg }
            ?: renumbered.maxOfOrNull { it.weightKg } ?: 0.0

        val updatedLog = exercise.exerciseLog.copy(
            setsCount = renumbered.size,
            maxKg = updatedMaxKg,
            setsDataJson = ExerciseSet.listToJsonString(renumbered)
        )

        currentList[exerciseIndex] = exercise.copy(
            exerciseLog = updatedLog,
            sets = renumbered,
            maxWeightKg = updatedMaxKg
        )
        _exercises.value = currentList

        viewModelScope.launch {
            workoutRepository.updateExerciseLog(updatedLog)
        }
    }

    fun removeExercise(exerciseIndex: Int) {
        val currentList = _exercises.value.toMutableList()
        if (exerciseIndex !in currentList.indices) return
        val target = currentList.removeAt(exerciseIndex)
        _exercises.value = currentList
        viewModelScope.launch {
            workoutRepository.deleteExerciseLog(target.exerciseLog)
        }
    }

    // --- Rest Timer Functions ---

    fun startRestTimer(context: Context, seconds: Int) {
        timerJob?.cancel()
        _restTotalDuration.value = seconds
        _restSecondsLeft.value = seconds
        _isTimerRunning.value = true

        timerJob = viewModelScope.launch {
            while (_restSecondsLeft.value > 0 && _isTimerRunning.value) {
                delay(1000)
                _restSecondsLeft.value = _restSecondsLeft.value - 1
            }
            if (_restSecondsLeft.value == 0) {
                _isTimerRunning.value = false
                TimerFeedbackHelper.notifyTimerCompleted(context)
            }
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resumeTimer(context: Context) {
        if (_restSecondsLeft.value <= 0) return
        _isTimerRunning.value = true
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_restSecondsLeft.value > 0 && _isTimerRunning.value) {
                delay(1000)
                _restSecondsLeft.value = _restSecondsLeft.value - 1
            }
            if (_restSecondsLeft.value == 0) {
                _isTimerRunning.value = false
                TimerFeedbackHelper.notifyTimerCompleted(context)
            }
        }
    }

    fun addRestTime(seconds: Int) {
        _restSecondsLeft.value = _restSecondsLeft.value + seconds
        if (_restSecondsLeft.value > _restTotalDuration.value) {
            _restTotalDuration.value = _restSecondsLeft.value
        }
    }

    fun dismissTimer() {
        _isTimerRunning.value = false
        _restSecondsLeft.value = 0
        timerJob?.cancel()
    }

    fun setAutoStartTimer(enabled: Boolean) {
        _autoStartTimerOnCheck.value = enabled
    }

    // --- Finish Workout ---

    fun finishWorkout(onCompleted: () -> Unit) {
        val workout = _currentWorkout.value ?: return
        workoutTimerJob?.cancel()
        timerJob?.cancel()

        val elapsedMin = (_elapsedSeconds.value / 60).coerceAtLeast(1)
        var totalVolume = 0.0
        var totalSets = 0
        var exercisesCount = 0

        _exercises.value.forEach { ex ->
            var hasCompletedSet = false
            ex.sets.forEach { s ->
                if (s.isCompleted) {
                    totalVolume += s.weightKg * s.reps
                    totalSets++
                    hasCompletedSet = true
                }
            }
            if (hasCompletedSet) exercisesCount++
        }

        val updatedWorkout = workout.copy(
            durationMinutes = elapsedMin,
            lastPerformedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            workoutRepository.updateWorkout(updatedWorkout)
            _workoutFinishedSummary.value = WorkoutSummaryData(
                workoutName = workout.name,
                durationMinutes = elapsedMin,
                totalVolumeKg = totalVolume,
                totalSetsCompleted = totalSets,
                exercisesCompleted = exercisesCount
            )
            onCompleted()
        }
    }

    fun dismissSummary() {
        _workoutFinishedSummary.value = null
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        workoutTimerJob?.cancel()
    }
}

class ActiveWorkoutViewModelFactory(
    private val workoutRepository: WorkoutRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ActiveWorkoutViewModel(workoutRepository) as T
    }
}
