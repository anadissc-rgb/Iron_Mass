package com.example.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.WorkoutProgram
import com.example.data.model.MacroLog
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutLog
import com.example.data.repository.WorkoutRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@kotlin.OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class IronMassViewModel(private val repository: WorkoutRepository) : ViewModel() {

    // Current app state
    var selectedTab by mutableStateOf(0) // 0: Workout/Gym, 1: Macros, 2: Roadmap/Tips, 3: Profile/History

    // Selected Week and Day for tracker
    var currentWeek by mutableStateOf(1)  // 1 to 4
    var currentDay by mutableStateOf(1)   // 1 to 7
    var currentCycle by mutableStateOf(1) // 1 to 3 (shows cycle progress)

    // Log input state for active exercise
    var inputWeight by mutableStateOf("")
    var inputReps by mutableStateOf("")
    var activeExerciseName by mutableStateOf("")

    // Notification reminder toggle
    var remindersEnabled by mutableStateOf(true)

    // Active rest timer state
    var timerRunning by mutableStateOf(false)
    var timerRemainingSeconds by mutableStateOf(0)
    var timerTotalSeconds by mutableStateOf(0)
    var customRestSecondsOverwrite by mutableStateOf(0) // custom configured duration overwrite (0 = use default)
    var showRestCompletedAlert by mutableStateOf(false) // alerts user when timer finishes
    private var timerJob: Job? = null

    // Macro date
    var macroSelectedDateString by mutableStateOf(getTodayDateString())

    // Streams from DB
    val allLogs: StateFlow<List<WorkoutLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .map { it ?: UserProfile() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    // Flow for current date's macro
    val currentMacroLog: StateFlow<MacroLog> = snapshotFlow { macroSelectedDateString }
        .flatMapLatest { dateStr ->
            repository.getMacroLog(dateStr).map { it ?: MacroLog(dateString = dateStr) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MacroLog(dateString = getTodayDateString()))

    // Store previous performance per exercise to show progressive overload tip
    private val _exerciseHistory = MutableStateFlow<Map<String, List<WorkoutLog>>>(emptyMap())
    val exerciseHistory = _exerciseHistory.asStateFlow()

    init {
        // Load initial values or set up profile
        viewModelScope.launch {
            val prof = repository.getUserProfileDirect() ?: UserProfile().also {
                repository.insertUserProfile(it)
            }
            remindersEnabled = prof.remindersEnabled
            
            // Calculate and set automatic current week, day, and cycle on launch
            val (w, d, c) = calculateWorkoutProgress(prof.programStartDate)
            currentWeek = w
            currentDay = d
            currentCycle = c
            
            refreshExerciseHistory()
        }

        // Keep local profile values up-to-date with any outside changes
        viewModelScope.launch {
            repository.userProfile.collect { prof ->
                prof?.let {
                    remindersEnabled = it.remindersEnabled
                }
            }
        }
    }

    /**
     * Calculates the automatic current workout Day, Week and Cycle
     * based on the program's start date using exact calendar normalization.
     */
    fun calculateWorkoutProgress(startDateMillis: Long): Triple<Int, Int, Int> {
        val startCal = Calendar.getInstance().apply { timeInMillis = startDateMillis }
        val currentCal = Calendar.getInstance()
        
        // Normalize calendars to midnight of the start day to avoid intra-day hour mismatches
        val startMidnight = Calendar.getInstance().apply {
            timeZone = startCal.timeZone
            set(Calendar.YEAR, startCal.get(Calendar.YEAR))
            set(Calendar.MONTH, startCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, startCal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val currentMidnight = Calendar.getInstance().apply {
            timeZone = currentCal.timeZone
            set(Calendar.YEAR, currentCal.get(Calendar.YEAR))
            set(Calendar.MONTH, currentCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, currentCal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val diffMillis = currentMidnight.timeInMillis - startMidnight.timeInMillis
        if (diffMillis < 0) {
            return Triple(1, 1, 1)
        }
        
        val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
        val dayNumber = (diffDays % 7) + 1
        val totalWeeksElapsed = diffDays / 7
        val weekNumber = (totalWeeksElapsed % 4) + 1
        val cycleNumber = (totalWeeksElapsed / 4) + 1
        
        return Triple(weekNumber, dayNumber, cycleNumber)
    }

    fun syncToToday() {
        viewModelScope.launch {
            val prof = repository.getUserProfileDirect() ?: return@launch
            val (w, d, c) = calculateWorkoutProgress(prof.programStartDate)
            currentWeek = w
            currentDay = d
            currentCycle = c
        }
    }

    fun updateProgramStartDate(timestamp: Long) {
        viewModelScope.launch {
            val current = repository.getUserProfileDirect() ?: UserProfile()
            val updated = current.copy(programStartDate = timestamp)
            repository.insertUserProfile(updated)
            // Immediately sync to the new start date
            val (w, d, c) = calculateWorkoutProgress(timestamp)
            currentWeek = w
            currentDay = d
            currentCycle = c
        }
    }

    private fun refreshExerciseHistory() {
        viewModelScope.launch {
            allLogs.collect { logs ->
                val grouped = logs.groupBy { it.exerciseName }
                _exerciseHistory.value = grouped
            }
        }
    }

    fun selectWorkout(week: Int, day: Int) {
        currentWeek = week
        currentDay = day
        // Clear active inputs when switching workouts
        inputWeight = ""
        inputReps = ""
        activeExerciseName = ""
    }

    fun previousWorkoutDay() {
        if (currentDay > 1) {
            currentDay--
        } else if (currentWeek > 1) {
            currentWeek--
            currentDay = 7
        }
    }

    fun nextWorkoutDay() {
        if (currentDay < 7) {
            currentDay++
        } else if (currentWeek < 4) {
            currentWeek++
            currentDay = 1
        } else {
            // cycle repeats or loops
            currentWeek = 1
            currentDay = 1
            if (currentCycle < 3) currentCycle++ else currentCycle = 1
        }
    }

    // Timer functions
    fun startRestTimer(seconds: Int) {
        timerJob?.cancel()
        // Determine final duration from default or user custom override configuration
        val finalSeconds = if (customRestSecondsOverwrite > 0) customRestSecondsOverwrite else seconds
        timerTotalSeconds = finalSeconds
        timerRemainingSeconds = finalSeconds
        timerRunning = true
        timerJob = viewModelScope.launch {
            while (timerRemainingSeconds > 0) {
                delay(1000)
                timerRemainingSeconds--
            }
            if (timerRunning) {
                timerRunning = false
                showRestCompletedAlert = true
                triggerAlertDevice()
            }
        }
    }

    private fun triggerAlertDevice() {
        try {
            // Sound play using standard Android ToneGenerator for audio alert
            val toneG = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
            toneG.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 600) // distinct beep tone
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelRestTimer() {
        timerJob?.cancel()
        timerRunning = false
        timerRemainingSeconds = 0
    }

    // Saving workout logs
    fun logSet(exerciseName: String, setNumber: Int, weight: Double, reps: Int) {
        viewModelScope.launch {
            val log = WorkoutLog(
                weekNumber = currentWeek,
                dayNumber = currentDay,
                cycleNumber = currentCycle,
                exerciseName = exerciseName,
                setNumber = setNumber,
                weightUsed = weight,
                repsCompleted = reps,
                timestamp = System.currentTimeMillis()
            )
            repository.insertWorkoutLog(log)
        }
    }

    fun deleteLog(id: Long) {
        viewModelScope.launch {
            repository.deleteWorkoutLogById(id)
        }
    }

    fun clearLogForExercise(exerciseName: String) {
        viewModelScope.launch {
            repository.clearLogForExerciseDay(
                exerciseName = exerciseName,
                week = currentWeek,
                day = currentDay,
                cycle = currentCycle
            )
        }
    }

    // Macros checking
    fun toggleMacro(mealType: String) {
        viewModelScope.launch {
            val current = currentMacroLog.value
            val updated = when (mealType) {
                "breakfast" -> current.copy(breakfastChecked = !current.breakfastChecked)
                "lunch" -> current.copy(lunchChecked = !current.lunchChecked)
                "dinner" -> current.copy(dinnerChecked = !current.dinnerChecked)
                "preWorkout" -> current.copy(preWorkoutChecked = !current.preWorkoutChecked)
                "postWorkout" -> current.copy(postWorkoutChecked = !current.postWorkoutChecked)
                else -> current
            }
            repository.insertMacroLog(updated)
        }
    }

    fun updateMealMacros(mealType: String, proteinG: Double, carbsG: Double, fatsG: Double) {
        viewModelScope.launch {
            val current = currentMacroLog.value
            val updated = when (mealType) {
                "breakfast" -> current.copy(breakfastProteinG = proteinG, breakfastCarbsG = carbsG, breakfastFatsG = fatsG)
                "lunch" -> current.copy(lunchProteinG = proteinG, lunchCarbsG = carbsG, lunchFatsG = fatsG)
                "dinner" -> current.copy(dinnerProteinG = proteinG, dinnerCarbsG = carbsG, dinnerFatsG = fatsG)
                "preWorkout" -> current.copy(preWorkoutProteinG = proteinG, preWorkoutCarbsG = carbsG, preWorkoutFatsG = fatsG)
                "postWorkout" -> current.copy(postWorkoutProteinG = proteinG, postWorkoutCarbsG = carbsG, postWorkoutFatsG = fatsG)
                else -> current
            }
            repository.insertMacroLog(updated)
        }
    }

    fun adjustWater(amount: Int) {
        viewModelScope.launch {
            val current = currentMacroLog.value
            val newWater = (current.waterGlasses + amount).coerceAtLeast(0)
            repository.insertMacroLog(current.copy(waterGlasses = newWater))
        }
    }

    fun changeMacroDate(daysOffset: Int) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(macroSelectedDateString) ?: Date()
        val cal = Calendar.getInstance()
        cal.time = date
        cal.add(Calendar.DAY_OF_YEAR, daysOffset)
        macroSelectedDateString = sdf.format(cal.time)
    }

    fun updateProfileWeight(weight: Double) {
        viewModelScope.launch {
            val current = repository.getUserProfileDirect() ?: UserProfile()
            repository.insertUserProfile(current.copy(weightKg = weight))
        }
    }

    fun updateFullProfile(
        name: String,
        age: Int,
        heightCm: Double,
        weightKg: Double,
        chestSizeCm: Double,
        bicepSizeCm: Double,
        waistSizeCm: Double,
        thighSizeCm: Double,
        calfSizeCm: Double,
        emergencyContact: String
    ) {
        viewModelScope.launch {
            val current = repository.getUserProfileDirect() ?: UserProfile()
            val updated = current.copy(
                name = name,
                age = age,
                heightCm = heightCm,
                weightKg = weightKg,
                chestSizeCm = chestSizeCm,
                bicepSizeCm = bicepSizeCm,
                waistSizeCm = waistSizeCm,
                thighSizeCm = thighSizeCm,
                calfSizeCm = calfSizeCm,
                emergencyContact = emergencyContact
            )
            repository.insertUserProfile(updated)
        }
    }

    fun updateMacrosChecklist(
        breakfastTitle: String, breakfastProtein: String, breakfastCarbs: String, breakfastFats: String, breakfastSuggestions: String,
        lunchTitle: String, lunchProtein: String, lunchCarbs: String, lunchFats: String, lunchSuggestions: String,
        preWorkoutTitle: String, preWorkoutProtein: String, preWorkoutCarbs: String, preWorkoutFats: String, preWorkoutSuggestions: String,
        postWorkoutTitle: String, postWorkoutProtein: String, postWorkoutCarbs: String, postWorkoutFats: String, postWorkoutSuggestions: String,
        dinnerTitle: String, dinnerProtein: String, dinnerCarbs: String, dinnerFats: String, dinnerSuggestions: String
    ) {
        viewModelScope.launch {
            val current = repository.getUserProfileDirect() ?: UserProfile()
            val updated = current.copy(
                breakfastTitle = breakfastTitle,
                breakfastProtein = breakfastProtein,
                breakfastCarbs = breakfastCarbs,
                breakfastFats = breakfastFats,
                breakfastSuggestions = breakfastSuggestions,
                lunchTitle = lunchTitle,
                lunchProtein = lunchProtein,
                lunchCarbs = lunchCarbs,
                lunchFats = lunchFats,
                lunchSuggestions = lunchSuggestions,
                preWorkoutTitle = preWorkoutTitle,
                preWorkoutProtein = preWorkoutProtein,
                preWorkoutCarbs = preWorkoutCarbs,
                preWorkoutFats = preWorkoutFats,
                preWorkoutSuggestions = preWorkoutSuggestions,
                postWorkoutTitle = postWorkoutTitle,
                postWorkoutProtein = postWorkoutProtein,
                postWorkoutCarbs = postWorkoutCarbs,
                postWorkoutFats = postWorkoutFats,
                postWorkoutSuggestions = postWorkoutSuggestions,
                dinnerTitle = dinnerTitle,
                dinnerProtein = dinnerProtein,
                dinnerCarbs = dinnerCarbs,
                dinnerFats = dinnerFats,
                dinnerSuggestions = dinnerSuggestions
            )
            repository.insertUserProfile(updated)
        }
    }

    fun toggleReminders() {
        viewModelScope.launch {
            val current = repository.getUserProfileDirect() ?: UserProfile()
            val newEnable = !current.remindersEnabled
            remindersEnabled = newEnable
            repository.insertUserProfile(current.copy(remindersEnabled = newEnable))
        }
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}

class IronMassViewModelFactory(private val repository: WorkoutRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IronMassViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return IronMassViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
