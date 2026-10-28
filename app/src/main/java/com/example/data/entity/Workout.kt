package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dayOfWeek: String = "",
    val targetMuscles: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastPerformedAt: Long? = null,
    val isRoutineTemplate: Boolean = false,
    val durationMinutes: Int = 0
)
