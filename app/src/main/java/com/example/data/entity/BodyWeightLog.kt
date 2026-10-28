package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bodyweight_logs",
    indices = [Index(value = ["dateEpochDay"])]
)
data class BodyWeightLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val weightKg: Double,
    val dateEpochDay: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)
