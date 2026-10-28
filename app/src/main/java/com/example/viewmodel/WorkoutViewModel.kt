package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.entity.Workout
import com.example.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorkoutViewModel(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _selectedDay = MutableStateFlow(getCurrentDayOfWeek())
    val selectedDay: StateFlow<String> = _selectedDay.asStateFlow()

    val routineTemplates: StateFlow<List<Workout>> = workoutRepository.routineTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedWorkouts: StateFlow<List<Workout>> = workoutRepository.completedWorkouts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todaysRoutine: StateFlow<Workout?> = combine(
        routineTemplates,
        selectedDay
    ) { routines, day ->
        routines.firstOrNull { it.dayOfWeek.equals(day, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setSelectedDay(day: String) {
        _selectedDay.value = day
    }

    fun createRoutine(
        name: String,
        dayOfWeek: String,
        targetMuscles: String
    ) {
        viewModelScope.launch {
            val workout = Workout(
                name = name,
                dayOfWeek = dayOfWeek,
                targetMuscles = targetMuscles,
                createdAt = System.currentTimeMillis(),
                isRoutineTemplate = true
            )
            workoutRepository.insertWorkout(workout)
        }
    }

    fun deleteWorkout(workout: Workout) {
        viewModelScope.launch {
            workoutRepository.deleteWorkout(workout)
        }
    }

    suspend fun getExercisesForRoutine(workoutId: Long) =
        workoutRepository.getExercisesForWorkoutOnce(workoutId)

    fun saveRoutineWithExercises(workout: Workout, exercises: List<com.example.data.entity.ExerciseLog>) {
        viewModelScope.launch {
            workoutRepository.saveRoutineWithExercises(workout, exercises)
        }
    }

    companion object {
        fun getCurrentDayOfWeek(): String {
            return SimpleDateFormat("EEEE", Locale.US).format(Date())
        }

        val DAYS_OF_WEEK = listOf(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
        )
    }
}

class WorkoutViewModelFactory(
    private val workoutRepository: WorkoutRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WorkoutViewModel(workoutRepository) as T
    }
}
