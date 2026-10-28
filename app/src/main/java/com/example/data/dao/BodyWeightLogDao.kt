package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.BodyWeightLog
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyWeightLogDao {
    @Query("SELECT * FROM bodyweight_logs ORDER BY dateEpochDay DESC, timestamp DESC")
    fun getAllBodyWeightLogs(): Flow<List<BodyWeightLog>>

    @Query("SELECT * FROM bodyweight_logs ORDER BY dateEpochDay DESC, timestamp DESC")
    suspend fun getAllBodyWeightLogsOnce(): List<BodyWeightLog>

    @Query("SELECT * FROM bodyweight_logs ORDER BY dateEpochDay DESC, timestamp DESC LIMIT 1")
    fun getLatestBodyWeightLog(): Flow<BodyWeightLog?>

    @Query("SELECT * FROM bodyweight_logs WHERE dateEpochDay >= :startEpochDay ORDER BY dateEpochDay ASC")
    fun getLogsSince(startEpochDay: Long): Flow<List<BodyWeightLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyWeightLog(log: BodyWeightLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyWeightLogs(logs: List<BodyWeightLog>): List<Long>

    @Update
    suspend fun updateBodyWeightLog(log: BodyWeightLog)

    @Delete
    suspend fun deleteBodyWeightLog(log: BodyWeightLog)

    @Query("DELETE FROM bodyweight_logs WHERE id = :id")
    suspend fun deleteBodyWeightLogById(id: Long)

    @Query("DELETE FROM bodyweight_logs")
    suspend fun deleteAllBodyWeightLogs()
}
