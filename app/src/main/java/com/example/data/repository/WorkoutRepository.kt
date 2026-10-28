package com.example.data.repository

import com.example.data.dao.ExerciseLogDao
import com.example.data.dao.ExerciseMaxRecord
import com.example.data.dao.WorkoutDao
import com.example.data.entity.ExerciseLog
import com.example.data.entity.Workout
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val exerciseLogDao: ExerciseLogDao
) {
    val allWorkouts: Flow<List<Workout>> = workoutDao.getAllWorkouts()
    val routineTemplates: Flow<List<Workout>> = workoutDao.getRoutineTemplates()
    val completedWorkouts: Flow<List<Workout>> = workoutDao.getCompletedWorkouts()
    val allPersonalBests: Flow<List<ExerciseMaxRecord>> = exerciseLogDao.getAllPersonalBests()

    fun getWorkoutById(id: Long): Flow<Workout?> = workoutDao.getWorkoutById(id)
    suspend fun getWorkoutByIdOnce(id: Long): Workout? = workoutDao.getWorkoutByIdOnce(id)
    suspend fun getRoutineTemplatesOnce(): List<Workout> = workoutDao.getRoutineTemplatesOnce()
    fun getExercisesForWorkout(workoutId: Long): Flow<List<ExerciseLog>> = exerciseLogDao.getExercisesForWorkout(workoutId)
    suspend fun getExercisesForWorkoutOnce(workoutId: Long): List<ExerciseLog> = exerciseLogDao.getExercisesForWorkoutOnce(workoutId)
    fun getPersonalBestForExercise(exerciseName: String): Flow<Double?> = exerciseLogDao.getPersonalBestForExercise(exerciseName)

    suspend fun saveRoutineWithExercises(workout: Workout, exercises: List<ExerciseLog>) {
        if (workout.id == 0L) {
            val newId = workoutDao.insertWorkout(workout)
            val mappedExercises = exercises.map { it.copy(id = 0, workoutId = newId) }
            exerciseLogDao.insertExerciseLogs(mappedExercises)
        } else {
            workoutDao.updateWorkout(workout)
            exerciseLogDao.deleteLogsForWorkout(workout.id)
            val mappedExercises = exercises.map { it.copy(id = 0, workoutId = workout.id) }
            exerciseLogDao.insertExerciseLogs(mappedExercises)
        }
    }

    suspend fun insertWorkout(workout: Workout): Long = workoutDao.insertWorkout(workout)
    suspend fun updateWorkout(workout: Workout) = workoutDao.updateWorkout(workout)
    suspend fun deleteWorkout(workout: Workout) {
        exerciseLogDao.deleteLogsForWorkout(workout.id)
        workoutDao.deleteWorkout(workout)
    }
    suspend fun deleteWorkoutById(id: Long) {
        exerciseLogDao.deleteLogsForWorkout(id)
        workoutDao.deleteWorkoutById(id)
    }

    suspend fun insertExerciseLog(log: ExerciseLog): Long = exerciseLogDao.insertExerciseLog(log)
    suspend fun updateExerciseLog(log: ExerciseLog) = exerciseLogDao.updateExerciseLog(log)
    suspend fun deleteExerciseLog(log: ExerciseLog) = exerciseLogDao.deleteExerciseLog(log)
    suspend fun deleteExerciseLogById(id: Long) = exerciseLogDao.deleteExerciseLogById(id)

    /**
     * Clones a template workout into a new active completed workout session.
     */
    suspend fun startWorkoutFromTemplate(template: Workout): Pair<Long, List<Long>> {
        val newWorkout = Workout(
            name = template.name,
            dayOfWeek = template.dayOfWeek,
            targetMuscles = template.targetMuscles,
            createdAt = System.currentTimeMillis(),
            lastPerformedAt = null,
            isRoutineTemplate = false,
            durationMinutes = 0
        )
        val workoutId = workoutDao.insertWorkout(newWorkout)

        // Clone template exercises for this active session
        val templateExercises = exerciseLogDao.getExercisesForWorkoutOnce(template.id)
        val clonedExercises = templateExercises.map { ex ->
            ex.copy(
                id = 0,
                workoutId = workoutId,
                timestamp = System.currentTimeMillis()
            )
        }
        val exerciseIds = exerciseLogDao.insertExerciseLogs(clonedExercises)

        // Update template's last performed timestamp
        workoutDao.updateWorkout(template.copy(lastPerformedAt = System.currentTimeMillis()))

        return Pair(workoutId, exerciseIds)
    }
}
