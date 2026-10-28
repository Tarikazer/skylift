package com.example.viewmodel

import android.content.Context
import android.os.SystemClock
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
    val personalRecord: Double? = null,
    val lastSessionBestSet: String? = null
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
    private var timerTargetRealtime: Long = 0L
    private var timerRemainingMillisOnPause: Long = 0L

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
                _elapsedSeconds.value = diff.toInt().coerceAtLeast(0)
            }
        }
    }

    fun loadWorkout(workoutId: Long) {
        // Reset stopwatch so it starts fresh and works again after previous finishes
        workoutTimerJob?.cancel()
        _elapsedSeconds.value = 0
        _workoutStartTime.value = System.currentTimeMillis()
        startSessionStopwatch()

        viewModelScope.launch {
            val workout = workoutRepository.getWorkoutByIdOnce(workoutId)
            _currentWorkout.value = workout
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
                lastPerformedAt = null,
                isRoutineTemplate = false,
                durationMinutes = 0
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
                listOf(
                    ExerciseSet(1, log.maxKg.coerceAtLeast(20.0), log.reps.coerceAtLeast(10), false),
                    ExerciseSet(2, log.maxKg.coerceAtLeast(20.0), log.reps.coerceAtLeast(10), false),
                    ExerciseSet(3, log.maxKg.coerceAtLeast(20.0), log.reps.coerceAtLeast(10), false)
                )
            }

            // Max weight from completed sets only; if none completed, keep stored log.maxKg
            val completedMax = sets.filter { it.isCompleted && it.weightKg > 0.0 }.maxOfOrNull { it.weightKg }
            val maxKg = completedMax ?: log.maxKg

            // Look up previous finished session log for best set display
            val lastFinishedLog = workoutRepository.getLatestFinishedExerciseLog(log.exerciseName)
            val lastBestSetText = if (lastFinishedLog != null && lastFinishedLog.setsDataJson.isNotBlank()) {
                val prevSets = ExerciseSet.listFromJsonString(lastFinishedLog.setsDataJson)
                val completedPrev = prevSets.filter { it.isCompleted && it.weightKg > 0.0 }
                val bestSet = completedPrev.maxWithOrNull(compareBy({ it.weightKg }, { it.reps }))
                    ?: prevSets.filter { it.weightKg > 0.0 }.maxWithOrNull(compareBy({ it.weightKg }, { it.reps }))
                if (bestSet != null && bestSet.weightKg > 0.0) {
                    val weightStr = if (bestSet.weightKg % 1.0 == 0.0) "${bestSet.weightKg.toInt()} kg" else "${bestSet.weightKg} kg"
                    "$weightStr × ${bestSet.reps}"
                } else null
            } else null

            // All-time PB from DAO
            val allTimePb = workoutRepository.getPersonalBestForExerciseOnce(log.exerciseName)
            val personalBest = allTimePb ?: (if (log.maxKg > 0.0) log.maxKg else null)

            ActiveExerciseUiState(
                exerciseLog = log,
                sets = sets,
                maxWeightKg = maxKg,
                personalRecord = personalBest,
                lastSessionBestSet = lastBestSetText
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
                maxKg = 0.0,
                restTimeSeconds = defaultRestSeconds,
                setsDataJson = ExerciseSet.listToJsonString(initialSets),
                notes = ""
            )
            workoutRepository.insertExerciseLog(log)
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

        // Compute maxKg from COMPLETED sets only. If none completed, keep previous stored value
        val completedMax = setList.filter { it.isCompleted && it.weightKg > 0.0 }.maxOfOrNull { it.weightKg }
        val updatedMaxKg = completedMax ?: exercise.exerciseLog.maxKg

        val updatedLog = exercise.exerciseLog.copy(
            setsDataJson = ExerciseSet.listToJsonString(setList),
            maxKg = updatedMaxKg,
            setsCount = setList.size
        )

        val updatedPb = if (completedMax != null) {
            maxOf(exercise.personalRecord ?: 0.0, completedMax)
        } else exercise.personalRecord

        currentList[exerciseIndex] = exercise.copy(
            exerciseLog = updatedLog,
            sets = setList,
            maxWeightKg = updatedMaxKg,
            personalRecord = updatedPb
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
        val clamped = newMaxKg.coerceAtLeast(0.0)
        val updatedLog = exercise.exerciseLog.copy(maxKg = clamped)
        val updatedPb = maxOf(exercise.personalRecord ?: 0.0, clamped)

        currentList[exerciseIndex] = exercise.copy(
            exerciseLog = updatedLog,
            maxWeightKg = clamped,
            personalRecord = updatedPb
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

        // When a set is marked done and the next set has weight 0, copy the weight from the completed set
        if (newlyCompleted && setIndex + 1 < setList.size) {
            val nextSet = setList[setIndex + 1]
            if (nextSet.weightKg == 0.0) {
                setList[setIndex + 1] = nextSet.copy(weightKg = oldSet.weightKg)
            }
        }

        // Compute maxKg from COMPLETED sets only. If none completed, keep previous stored value
        val completedMax = setList.filter { it.isCompleted && it.weightKg > 0.0 }.maxOfOrNull { it.weightKg }
        val updatedMaxKg = completedMax ?: exercise.exerciseLog.maxKg

        val updatedLog = exercise.exerciseLog.copy(
            setsDataJson = ExerciseSet.listToJsonString(setList),
            maxKg = updatedMaxKg
        )

        val updatedPb = if (completedMax != null) {
            maxOf(exercise.personalRecord ?: 0.0, completedMax)
        } else exercise.personalRecord

        currentList[exerciseIndex] = exercise.copy(
            exerciseLog = updatedLog,
            sets = setList,
            maxWeightKg = updatedMaxKg,
            personalRecord = updatedPb
        )
        _exercises.value = currentList

        viewModelScope.launch {
            workoutRepository.updateExerciseLog(updatedLog)
        }

        // Trigger countdown rest timer automatically if set was completed
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

        // Compute maxKg from completed sets only
        val completedMax = renumbered.filter { it.isCompleted && it.weightKg > 0.0 }.maxOfOrNull { it.weightKg }
        val updatedMaxKg = completedMax ?: exercise.exerciseLog.maxKg

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

    // --- Rest Timer Functions (Timestamp-based via SystemClock.elapsedRealtime) ---

    fun startRestTimer(context: Context, seconds: Int) {
        if (seconds <= 0) return
        _restTotalDuration.value = seconds
        _restSecondsLeft.value = seconds
        timerRemainingMillisOnPause = 0L
        timerTargetRealtime = SystemClock.elapsedRealtime() + (seconds * 1000L)
        runTimerLoop(context)
    }

    fun pauseTimer() {
        if (!_isTimerRunning.value) return
        _isTimerRunning.value = false
        timerJob?.cancel()
        val remaining = (timerTargetRealtime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        timerRemainingMillisOnPause = remaining
        _restSecondsLeft.value = ((remaining + 999) / 1000).toInt()
    }

    fun resumeTimer(context: Context) {
        if (_restSecondsLeft.value <= 0) return
        val remaining = if (timerRemainingMillisOnPause > 0L) {
            timerRemainingMillisOnPause
        } else {
            _restSecondsLeft.value * 1000L
        }
        timerTargetRealtime = SystemClock.elapsedRealtime() + remaining
        timerRemainingMillisOnPause = 0L
        runTimerLoop(context)
    }

    fun addRestTime(seconds: Int) {
        val addMillis = seconds * 1000L
        if (_isTimerRunning.value) {
            timerTargetRealtime += addMillis
            val remaining = (timerTargetRealtime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            val newSeconds = ((remaining + 999) / 1000).toInt()
            _restSecondsLeft.value = newSeconds
            if (newSeconds > _restTotalDuration.value) {
                _restTotalDuration.value = newSeconds
            }
        } else {
            timerRemainingMillisOnPause += addMillis
            val newSeconds = ((timerRemainingMillisOnPause + 999) / 1000).toInt()
            _restSecondsLeft.value = newSeconds
            if (newSeconds > _restTotalDuration.value) {
                _restTotalDuration.value = newSeconds
            }
        }
    }

    fun dismissTimer() {
        _isTimerRunning.value = false
        _restSecondsLeft.value = 0
        timerRemainingMillisOnPause = 0L
        timerJob?.cancel()
    }

    private fun runTimerLoop(context: Context) {
        timerJob?.cancel()
        _isTimerRunning.value = true
        timerJob = viewModelScope.launch {
            while (_isTimerRunning.value) {
                val now = SystemClock.elapsedRealtime()
                val remainingMillis = timerTargetRealtime - now
                if (remainingMillis <= 0L) {
                    _restSecondsLeft.value = 0
                    _isTimerRunning.value = false
                    TimerFeedbackHelper.notifyTimerCompleted(context)
                    break
                } else {
                    _restSecondsLeft.value = ((remainingMillis + 999) / 1000).toInt()
                    delay(250)
                }
            }
        }
    }

    fun setAutoStartTimer(enabled: Boolean) {
        _autoStartTimerOnCheck.value = enabled
    }

    // --- Session Lifecycle: Finish & Abandon ---

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
            // If there is a routine template with matching name, update its lastPerformedAt
            val templates = workoutRepository.getRoutineTemplatesOnce()
            val matchingTemplate = templates.firstOrNull { it.name.equals(workout.name, ignoreCase = true) }
            if (matchingTemplate != null) {
                workoutRepository.updateWorkout(matchingTemplate.copy(lastPerformedAt = System.currentTimeMillis()))
            }

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
        _currentWorkout.value = null
        _exercises.value = emptyList()
        _elapsedSeconds.value = 0
        workoutTimerJob?.cancel()
        timerJob?.cancel()
    }

    /**
     * If the session has 0 completed sets, deletes it from DB and clears state.
     * Otherwise leaves the session in DB and calls onKept().
     */
    fun abandonWorkoutIfEmpty(onDeleted: () -> Unit, onKept: () -> Unit = {}) {
        val workout = _currentWorkout.value
        if (workout == null) {
            onDeleted()
            return
        }
        val totalCompletedSets = _exercises.value.sumOf { ex -> ex.sets.count { it.isCompleted } }
        if (totalCompletedSets == 0) {
            viewModelScope.launch {
                workoutRepository.deleteWorkoutById(workout.id)
                _currentWorkout.value = null
                _exercises.value = emptyList()
                _elapsedSeconds.value = 0
                workoutTimerJob?.cancel()
                timerJob?.cancel()
                onDeleted()
            }
        } else {
            onKept()
        }
    }

    fun discardSession(onDiscarded: () -> Unit) {
        val workout = _currentWorkout.value
        viewModelScope.launch {
            if (workout != null) {
                workoutRepository.deleteWorkoutById(workout.id)
            }
            _currentWorkout.value = null
            _exercises.value = emptyList()
            _elapsedSeconds.value = 0
            workoutTimerJob?.cancel()
            timerJob?.cancel()
            onDiscarded()
        }
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
