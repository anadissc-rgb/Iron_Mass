package com.example.data.database

import androidx.room.*
import com.example.data.model.WorkoutLog
import com.example.data.model.MacroLog
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    // Workout logs
    @Query("SELECT * FROM workout_logs ORDER BY timestamp DESC")
    fun getAllWorkoutLogs(): Flow<List<WorkoutLog>>

    @Query("SELECT * FROM workout_logs WHERE exerciseName = :exerciseName ORDER BY timestamp DESC")
    fun getLogsForExercise(exerciseName: String): Flow<List<WorkoutLog>>

    @Query("SELECT * FROM workout_logs WHERE exerciseName = :exerciseName ORDER BY timestamp DESC LIMIT 10")
    suspend fun getRecentLogsForExerciseDirect(exerciseName: String): List<WorkoutLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutLog(log: WorkoutLog)

    @Query("DELETE FROM workout_logs WHERE id = :id")
    suspend fun deleteWorkoutLogById(id: Long)

    @Query("DELETE FROM workout_logs WHERE exerciseName = :exerciseName AND weekNumber = :week AND dayNumber = :day AND cycleNumber = :cycle")
    suspend fun clearLogForExerciseDay(exerciseName: String, week: Int, day: Int, cycle: Int)

    // Macros
    @Query("SELECT * FROM macro_logs WHERE dateString = :dateString")
    fun getMacroLog(dateString: String): Flow<MacroLog?>

    @Query("SELECT * FROM macro_logs WHERE dateString = :dateString")
    suspend fun getMacroLogDirect(dateString: String): MacroLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMacroLog(log: MacroLog)

    // Profile
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getUserProfileDirect(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)
}
