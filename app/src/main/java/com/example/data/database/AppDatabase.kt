package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.BodyWeightLogDao
import com.example.data.dao.ExerciseLogDao
import com.example.data.dao.WorkoutDao
import com.example.data.entity.BodyWeightLog
import com.example.data.entity.ExerciseLog
import com.example.data.entity.Workout

@Database(
    entities = [
        Workout::class,
        ExerciseLog::class,
        BodyWeightLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseLogDao(): ExerciseLogDao
    abstract fun bodyWeightLogDao(): BodyWeightLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "skylift_database.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
