package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_logs",
    indices = [
        Index(value = ["workoutId"]),
        Index(value = ["exerciseName"])
    ]
)
data class ExerciseLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutId: Long,
    val exerciseName: String,
    val targetMuscle: String,
    val setsCount: Int,
    val reps: Int,
    val maxKg: Double,
    val restTimeSeconds: Int = 60,
    val setsDataJson: String = "",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
