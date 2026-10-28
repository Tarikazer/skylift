package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.Workout
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY createdAt DESC")
    fun getAllWorkouts(): Flow<List<Workout>>

    @Query("SELECT * FROM workouts WHERE isRoutineTemplate = 1 ORDER BY id ASC")
    fun getRoutineTemplates(): Flow<List<Workout>>

    @Query("SELECT * FROM workouts WHERE isRoutineTemplate = 1 ORDER BY id ASC")
    suspend fun getRoutineTemplatesOnce(): List<Workout>

    @Query("SELECT * FROM workouts WHERE isRoutineTemplate = 0 AND lastPerformedAt IS NOT NULL ORDER BY lastPerformedAt DESC")
    fun getCompletedWorkouts(): Flow<List<Workout>>

    @Query("SELECT * FROM workouts WHERE id = :id LIMIT 1")
    fun getWorkoutById(id: Long): Flow<Workout?>

    @Query("SELECT * FROM workouts WHERE id = :id LIMIT 1")
    suspend fun getWorkoutByIdOnce(id: Long): Workout?

    @Query("SELECT * FROM workouts WHERE isRoutineTemplate = 1 AND dayOfWeek = :dayOfWeek LIMIT 1")
    fun getRoutineForDay(dayOfWeek: String): Flow<Workout?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: Workout): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkouts(workouts: List<Workout>): List<Long>

    @Update
    suspend fun updateWorkout(workout: Workout)

    @Delete
    suspend fun deleteWorkout(workout: Workout)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkoutById(id: Long)

    @Query("DELETE FROM workouts")
    suspend fun deleteAllWorkouts()
}
