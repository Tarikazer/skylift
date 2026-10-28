package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.ExerciseLog
import kotlinx.coroutines.flow.Flow

data class ExerciseMaxRecord(
    val exerciseName: String,
    val maxWeight: Double
)

@Dao
interface ExerciseLogDao {
    @Query("SELECT * FROM exercise_logs WHERE workoutId = :workoutId ORDER BY id ASC")
    fun getExercisesForWorkout(workoutId: Long): Flow<List<ExerciseLog>>

    @Query("SELECT * FROM exercise_logs WHERE workoutId = :workoutId ORDER BY id ASC")
    suspend fun getExercisesForWorkoutOnce(workoutId: Long): List<ExerciseLog>

    @Query("SELECT * FROM exercise_logs ORDER BY timestamp DESC")
    fun getAllExerciseLogs(): Flow<List<ExerciseLog>>

    @Query("SELECT * FROM exercise_logs ORDER BY timestamp DESC")
    suspend fun getAllExerciseLogsOnce(): List<ExerciseLog>

    @Query("SELECT MAX(maxKg) FROM exercise_logs WHERE exerciseName = :exerciseName")
    fun getPersonalBestForExercise(exerciseName: String): Flow<Double?>

    @Query("SELECT MAX(maxKg) FROM exercise_logs WHERE exerciseName = :exerciseName AND maxKg > 0")
    suspend fun getPersonalBestForExerciseOnce(exerciseName: String): Double?

    @Query("""
        SELECT e.* FROM exercise_logs e 
        INNER JOIN workouts w ON e.workoutId = w.id 
        WHERE w.isRoutineTemplate = 0 AND w.lastPerformedAt IS NOT NULL AND e.exerciseName = :exerciseName 
        ORDER BY w.lastPerformedAt DESC, e.timestamp DESC LIMIT 1
    """)
    suspend fun getLatestFinishedExerciseLog(exerciseName: String): ExerciseLog?

    @Query("SELECT exerciseName, MAX(maxKg) as maxWeight FROM exercise_logs GROUP BY exerciseName")
    fun getAllPersonalBests(): Flow<List<ExerciseMaxRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseLog(log: ExerciseLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseLogs(logs: List<ExerciseLog>): List<Long>

    @Update
    suspend fun updateExerciseLog(log: ExerciseLog)

    @Delete
    suspend fun deleteExerciseLog(log: ExerciseLog)

    @Query("DELETE FROM exercise_logs WHERE workoutId = :workoutId")
    suspend fun deleteLogsForWorkout(workoutId: Long)

    @Query("DELETE FROM exercise_logs WHERE id = :id")
    suspend fun deleteExerciseLogById(id: Long)

    @Query("DELETE FROM exercise_logs")
    suspend fun deleteAllExerciseLogs()
}
