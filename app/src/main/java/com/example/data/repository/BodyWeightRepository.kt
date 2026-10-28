package com.example.data.repository

import com.example.data.dao.BodyWeightLogDao
import com.example.data.entity.BodyWeightLog
import kotlinx.coroutines.flow.Flow

class BodyWeightRepository(
    private val bodyWeightLogDao: BodyWeightLogDao
) {
    val allLogs: Flow<List<BodyWeightLog>> = bodyWeightLogDao.getAllBodyWeightLogs()
    val latestLog: Flow<BodyWeightLog?> = bodyWeightLogDao.getLatestBodyWeightLog()

    fun getLogsSince(startEpochDay: Long): Flow<List<BodyWeightLog>> =
        bodyWeightLogDao.getLogsSince(startEpochDay)

    suspend fun insertLog(log: BodyWeightLog): Long = bodyWeightLogDao.insertBodyWeightLog(log)

    suspend fun updateLog(log: BodyWeightLog) = bodyWeightLogDao.updateBodyWeightLog(log)

    suspend fun deleteLog(log: BodyWeightLog) = bodyWeightLogDao.deleteBodyWeightLog(log)

    suspend fun deleteLogById(id: Long) = bodyWeightLogDao.deleteBodyWeightLogById(id)
}
