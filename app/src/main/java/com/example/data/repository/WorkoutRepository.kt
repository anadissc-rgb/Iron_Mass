package com.example.data.repository

import com.example.data.database.WorkoutDao
import com.example.data.model.WorkoutLog
import com.example.data.model.MacroLog
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(private val dao: WorkoutDao) {
    val allLogs: Flow<List<WorkoutLog>> = dao.getAllWorkoutLogs()
    val userProfile: Flow<UserProfile?> = dao.getUserProfile()

    fun getLogsForExercise(exerciseName: String): Flow<List<WorkoutLog>> = dao.getLogsForExercise(exerciseName)

    suspend fun getRecentLogsForExerciseDirect(exerciseName: String): List<WorkoutLog> =
        dao.getRecentLogsForExerciseDirect(exerciseName)

    suspend fun insertWorkoutLog(log: WorkoutLog) = dao.insertWorkoutLog(log)

    suspend fun deleteWorkoutLogById(id: Long) = dao.deleteWorkoutLogById(id)

    suspend fun clearLogForExerciseDay(exerciseName: String, week: Int, day: Int, cycle: Int) =
        dao.clearLogForExerciseDay(exerciseName, week, day, cycle)

    fun getMacroLog(dateString: String): Flow<MacroLog?> = dao.getMacroLog(dateString)

    suspend fun getMacroLogDirect(dateString: String): MacroLog? = dao.getMacroLogDirect(dateString)

    suspend fun insertMacroLog(log: MacroLog) = dao.insertMacroLog(log)

    suspend fun getUserProfileDirect(): UserProfile? = dao.getUserProfileDirect()

    suspend fun insertUserProfile(profile: UserProfile) = dao.insertUserProfile(profile)
}
