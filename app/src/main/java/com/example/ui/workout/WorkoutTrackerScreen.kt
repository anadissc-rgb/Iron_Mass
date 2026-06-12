package com.example.ui.workout

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.WorkoutDay
import com.example.data.WorkoutProgram
import com.example.data.ExerciseInfo
import com.example.data.model.MacroLog
import com.example.data.model.UserProfile
import com.example.data.model.WorkoutLog
import com.example.ui.theme.*
import com.example.ui.viewmodel.IronMassViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WorkoutTrackerScreen(viewModel: IronMassViewModel) {
    val allLogs by viewModel.allLogs.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val macroLog by viewModel.currentMacroLog.collectAsStateWithLifecycle()
    val exerciseHistory by viewModel.exerciseHistory.collectAsStateWithLifecycle()

    if (viewModel.showRestCompletedAlert) {
        RestCompletedDialog(onDismiss = { viewModel.showRestCompletedAlert = false })
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = GymSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                listOf(
                    Triple(0, "Gym Mode", Icons.Default.PlayArrow),
                    Triple(1, "Macros", Icons.Default.CheckCircle),
                    Triple(2, "Roadmap", Icons.Default.Info),
                    Triple(3, "History", Icons.Default.List)
                ).forEach { (idx, label, icon) ->
                    val isSelected = viewModel.selectedTab == idx
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectedTab = idx },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) Color.Black else GymMuted
                            )
                        },
                        label = {
                            Text(
                                label,
                                color = if (isSelected) GymHighVizYellow else GymMuted,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = GymHighVizYellow
                        ),
                        modifier = Modifier.testTag("nav_tab_$idx")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GymBlack)
                .padding(innerPadding)
        ) {
            // Screen switcher with transition animation
            AnimatedContent(
                targetState = viewModel.selectedTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "screen_switch"
            ) { tab ->
                when (tab) {
                    0 -> GymModeScreen(viewModel, allLogs, exerciseHistory)
                    1 -> MacrosScreen(viewModel, macroLog, userProfile)
                    2 -> RoadmapScreen(viewModel, allLogs)
                    3 -> ProfileHistoryScreen(viewModel, allLogs, userProfile)
                }
            }

            // Floating countdown Timer bar
            if (viewModel.timerRunning) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                ) {
                    TimerFloatingCard(
                        remainingSeconds = viewModel.timerRemainingSeconds,
                        totalSeconds = viewModel.timerTotalSeconds,
                        onCancel = { viewModel.cancelRestTimer() },
                        onAddSeconds = { viewModel.startRestTimer(viewModel.timerRemainingSeconds + 15) }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 1. GYM MODE SCREEN
// -------------------------------------------------------------------------------------------------
@Composable
fun GymModeScreen(
    viewModel: IronMassViewModel,
    allLogs: List<WorkoutLog>,
    exerciseHistory: Map<String, List<WorkoutLog>>
) {
    val workoutDay = WorkoutProgram.getWorkout(viewModel.currentWeek, viewModel.currentDay)
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // App Identity Header
        GymAppHeader()

        // Quick Selector for Week/Day
        WeekDaySelector(
            currentWeek = viewModel.currentWeek,
            currentDay = viewModel.currentDay,
            currentCycle = viewModel.currentCycle,
            onSelect = { w, d -> viewModel.selectWorkout(w, d) }
        )

        val targetProgress = remember(userProfile.programStartDate) {
            viewModel.calculateWorkoutProgress(userProfile.programStartDate)
        }
        val isDifferent = viewModel.currentWeek != targetProgress.first ||
                viewModel.currentDay != targetProgress.second ||
                viewModel.currentCycle != targetProgress.third

        if (isDifferent) {
            Surface(
                color = GymSurfaceElevated,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, GymDivider),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Viewing alternative day/week split",
                            color = GymHighVizYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val splitLabel = if (targetProgress.first % 2 != 0) "Hybrid" else "PPL"
                        Text(
                            "Your scheduled split: Week ${targetProgress.first} ($splitLabel), Day ${targetProgress.second}",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                    Button(
                        onClick = { viewModel.syncToToday() },
                        colors = ButtonDefaults.buttonColors(containerColor = GymNeonCyan),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(28.dp).testTag("sync_to_today_button")
                    ) {
                        Text("Sync Today", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selected Workout Details
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workoutDay.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = workoutDay.subtitle,
                    color = GymNeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Quick shift buttons
            Row {
                IconButton(
                    onClick = { viewModel.previousWorkoutDay() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(GymSurfaceElevated, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Prev Day",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { viewModel.nextWorkoutDay() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(GymSurfaceElevated, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Day",
                        tint = GymHighVizYellow,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Rest Timer settings bar
        var showTimerConfigDialog by remember { mutableStateOf(false) }
        val restConfigLabel = if (viewModel.customRestSecondsOverwrite > 0) {
            "${viewModel.customRestSecondsOverwrite}s Overwrite"
        } else {
            "Programmed Defaults"
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(GymSurfaceElevated, RoundedCornerShape(8.dp))
                .border(2.dp, GymDivider, RoundedCornerShape(8.dp))
                .clickable { showTimerConfigDialog = true }
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("rest_timer_settings_bar"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Configurable Rest Timer",
                    tint = GymNeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Rest Period Intensity",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customize rest intervals between sets",
                        color = GymMuted,
                        fontSize = 10.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(GymHighVizYellow.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = restConfigLabel,
                        color = GymHighVizYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Edit Rest",
                        tint = GymHighVizYellow,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        if (showTimerConfigDialog) {
            RestTimerConfigDialog(
                currentOverwrite = viewModel.customRestSecondsOverwrite,
                onSave = { seconds ->
                    viewModel.customRestSecondsOverwrite = seconds
                    showTimerConfigDialog = false
                },
                onDismiss = { showTimerConfigDialog = false }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (workoutDay.isRestDay) {
            RestDayStateCard(onNext = { viewModel.nextWorkoutDay() })
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(workoutDay.exercises) { exercise ->
                    ExerciseLoggingCard(
                        exercise = exercise,
                        viewModel = viewModel,
                        logs = allLogs.filter {
                            it.exerciseName == exercise.name &&
                                    it.weekNumber == viewModel.currentWeek &&
                                    it.dayNumber == viewModel.currentDay &&
                                    it.cycleNumber == viewModel.currentCycle
                        },
                        history = exerciseHistory[exercise.name] ?: emptyList()
                    )
                }
            }
        }
    }
}

@Composable
fun RestDayStateCard(onNext: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = GymSurface),
        border = BorderStroke(1.dp, GymDivider)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(GymSurfaceElevated, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Rest",
                    tint = GymNeonCyan,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Active Rest & Recovery Day",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Make sure you get adequate rest in between training days. You’ll need to refuel, rehydrate and recover to maximize muscle protein synthesis and nervous system replenishment.",
                color = GymMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = GymSurfaceElevated,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "💡 Hydration Guideline (Tip #9):",
                        color = GymHighVizYellow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Drink 3-4 liters of water. Check out your Meal checklist on the Macros tab!",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = GymHighVizYellow),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Go to Next Workout Day", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ExerciseLoggingCard(
    exercise: ExerciseInfo,
    viewModel: IronMassViewModel,
    logs: List<WorkoutLog>,
    history: List<WorkoutLog>
) {
    var setsInputList = remember(logs) {
        val list = mutableStateListOf<Pair<String, String>>()
        repeat(exercise.numSets) { idx ->
            val existing = logs.find { it.setNumber == idx + 1 }
            if (existing != null) {
                list.add(Pair(existing.weightUsed.toString(), existing.repsCompleted.toString()))
            } else {
                list.add(Pair("", ""))
            }
        }
        list
    }

    // Previous performance for progressive overload (Tip #4)
    val latestHistorySet = remember(history) {
        // Find logs for this exercise that are not from this current active day
        val filtered = history.filter {
            !(it.weekNumber == viewModel.currentWeek &&
                    it.dayNumber == viewModel.currentDay &&
                    it.cycleNumber == viewModel.currentCycle)
        }
        filtered.firstOrNull()
    }

    // Max weight log for visual achievements
    val maxWeightLifted = remember(history) {
        history.maxOfOrNull { it.weightUsed } ?: 0.0
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exercise_card_${exercise.id}"),
        colors = CardDefaults.cardColors(containerColor = GymSurface),
        border = BorderStroke(1.dp, GymDivider)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Exercise Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1.5f)) {
                    Text(
                        text = exercise.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${exercise.targetMuscle} • ${exercise.repsAndSets} • ${exercise.restSeconds}s rest",
                        color = GymMuted,
                        fontSize = 12.sp
                    )
                }

                // If heavy load variation
                if (exercise.isHeavy) {
                    Box(
                        modifier = Modifier
                            .background(GymNeonCyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .border(1.dp, GymNeonCyan, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "HEAVY",
                            color = GymNeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Progressive Overload Info Badge
            if (latestHistorySet != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GymSurfaceElevated, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Previous Best",
                        tint = GymHighVizYellow,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Previous Week: ${latestHistorySet.weightUsed}kg x ${latestHistorySet.repsCompleted} (Set ${latestHistorySet.setNumber})",
                        color = GymHighVizYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Special Deadlift text (Tip #1 pyramid details)
            if (exercise.isPyramid) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GymSurfaceElevated.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        "📈 Pyramid Progression: Increase weight steadily as you decrease repetitions (Targets: 8, 6, 4, 2 reps).",
                        color = GymNeonCyan,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Logging Grid for each Set
            repeat(exercise.numSets) { setIdx ->
                val setNum = setIdx + 1
                val logForSet = logs.find { it.setNumber == setNum }
                val isSetLogged = logForSet != null

                // Inputs
                var wtInput by remember(logForSet) { mutableStateOf(logForSet?.weightUsed?.toString() ?: "") }
                var repInput by remember(logForSet) { mutableStateOf(logForSet?.repsCompleted?.toString() ?: "") }

                // Determine if progressive overload was hit! (compare to the previous same set number if available, otherwise any set)
                val isOverload = remember(logForSet, latestHistorySet) {
                    if (logForSet != null && latestHistorySet != null) {
                        logForSet.weightUsed > latestHistorySet.weightUsed
                    } else false
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Set $setNum",
                        color = if (isSetLogged) GymNeonCyan else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(55.dp)
                    )

                    // Weight input field
                    OutlinedTextField(
                        value = wtInput,
                        onValueChange = {
                            if (!isSetLogged) wtInput = it
                        },
                        placeholder = {
                            Text(
                                if (latestHistorySet != null) "${latestHistorySet.weightUsed}" else "kg",
                                color = GymMuted,
                                fontSize = 12.sp
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .width(75.dp)
                            .height(48.dp)
                            .testTag("exercise_${exercise.id}_weight"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = GymSurfaceElevated,
                            unfocusedContainerColor = GymSurfaceElevated,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            disabledContainerColor = GymSurfaceElevated.copy(alpha = 0.5f),
                            focusedIndicatorColor = GymHighVizYellow
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(6.dp),
                        enabled = !isSetLogged
                    )

                    Text("×", color = GymMuted, fontSize = 14.sp)

                    // Reps input field
                    OutlinedTextField(
                        value = repInput,
                        onValueChange = {
                            if (!isSetLogged) repInput = it
                        },
                        placeholder = {
                            Text(
                                if (latestHistorySet != null) "${latestHistorySet.repsCompleted}" else "reps",
                                color = GymMuted,
                                fontSize = 12.sp
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .width(75.dp)
                            .height(48.dp)
                            .testTag("exercise_${exercise.id}_reps"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = GymSurfaceElevated,
                            unfocusedContainerColor = GymSurfaceElevated,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            disabledContainerColor = GymSurfaceElevated.copy(alpha = 0.5f),
                            focusedIndicatorColor = GymHighVizYellow
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(6.dp),
                        enabled = !isSetLogged
                    )

                    // Action buttons (Log Check or Rest/Clear)
                    if (isSetLogged) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isOverload) {
                                Box(
                                    modifier = Modifier
                                        .background(GymHighVizYellow.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "🔥 Overload!",
                                        color = GymHighVizYellow,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Logged",
                                tint = GymNeonCyan,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        // Tap logged check to delete and re-enter
                                        viewModel.deleteLog(logForSet!!.id)
                                    }
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                val w = wtInput.toDoubleOrNull() ?: latestHistorySet?.weightUsed ?: 0.0
                                val r = repInput.toIntOrNull() ?: latestHistorySet?.repsCompleted ?: 10
                                if (w > 0) {
                                    viewModel.logSet(exercise.name, setNum, w, r)
                                    // Start the smart timer with the prescribed seconds
                                    viewModel.startRestTimer(exercise.restSeconds)
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(GymHighVizYellow, RoundedCornerShape(6.dp))
                                .testTag("log_button_${exercise.id}_$setNum")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Log Set",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Quick reset option if they want to clear everything in this card
            if (logs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        "Clear Exercise Logs",
                        color = GymSoftRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { viewModel.clearLogForExercise(exercise.name) }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WeekDaySelector(
    currentWeek: Int,
    currentDay: Int,
    currentCycle: Int,
    onSelect: (Int, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GymSurface)
            .padding(vertical = 12.dp)
    ) {
        // Selected Cycle & Split indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Cycle Progress: Week $currentWeek/4 (Cycle $currentCycle/3)",
                color = GymMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            val splitName = if (currentWeek % 2 != 0) "Hybrid Split" else "PPL Movement Split"
            Text(
                splitName,
                color = GymHighVizYellow,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Week selection row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items((1..4).toList()) { w ->
                val isSelected = w == currentWeek
                val isHybrid = w % 2 != 0
                val borderCol = if (isSelected) GymHighVizYellow else GymDivider
                val bgCol = if (isSelected) GymHighVizYellow.copy(alpha = 0.15f) else GymSurfaceElevated

                Surface(
                    onClick = { onSelect(w, currentDay) },
                    shape = RoundedCornerShape(8.dp),
                    color = bgCol,
                    border = BorderStroke(1.dp, borderCol),
                    modifier = Modifier.testTag("week_selector_$w")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = "WEEK $w",
                            color = if (isSelected) GymHighVizYellow else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (isHybrid) "Hybrid" else "PPL",
                            color = GymMuted,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Days of week row (1 to 7)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val daysNames = listOf("D1", "D2", "D3", "D4", "D5", "D6", "OFF")
            items((1..7).toList()) { d ->
                val isSelected = d == currentDay
                val label = daysNames[d - 1]
                val bg = if (isSelected) GymHighVizYellow else GymSurfaceElevated
                val textCol = if (isSelected) Color.Black else Color.White

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(bg)
                        .clickable { onSelect(currentWeek, d) }
                        .border(1.dp, if (isSelected) GymHighVizYellow else GymDivider, RoundedCornerShape(6.dp))
                        .testTag("day_selector_$d"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = textCol,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 1.5 TIMER FLOATING BAR
// -------------------------------------------------------------------------------------------------
@Composable
fun TimerFloatingCard(
    remainingSeconds: Int,
    totalSeconds: Int,
    onCancel: () -> Unit,
    onAddSeconds: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GymHighVizYellow, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = GymSurfaceElevated),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Ticking Circle Progress
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f },
                    color = GymHighVizYellow,
                    trackColor = GymDivider,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "$remainingSeconds",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Rest Timer Active (Tip #3)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Take rest as prescribed for optimal mass gains.",
                    color = GymMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Controls
            Row {
                TextButton(onClick = onAddSeconds) {
                    Text("+15s", color = GymNeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Timer",
                        tint = GymSoftRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 2. MACROS SCREEN (CHECKLIST WITH DYNAMIC PERSONAL WEIGHT)
// -------------------------------------------------------------------------------------------------
@Composable
fun MacrosScreen(
    viewModel: IronMassViewModel,
    macroLog: MacroLog,
    profile: UserProfile
) {
    val totalCaloriesMock = remember(macroLog) {
        // Simple visualization
        val checkedCount = listOf(
            macroLog.breakfastChecked,
            macroLog.lunchChecked,
            macroLog.dinnerChecked,
            macroLog.preWorkoutChecked,
            macroLog.postWorkoutChecked
        ).count { it }
        checkedCount * 550
    }

    // Post workout carbs: 1g per kg of bodyweight
    val postWorkoutCarbTarget = remember(profile.weightKg) {
        profile.weightKg.toInt()
    }

    var showMacroEditDialog by remember { mutableStateOf(false) }

    if (showMacroEditDialog) {
        EditMacrosChecklistDialog(
            profile = profile,
            onSave = { bTitle, bP, bC, bF, bS, lTitle, lP, lC, lF, lS, preTitle, preP, preC, preF, preS, postTitle, postP, postC, postF, postS, dTitle, dP, dC, dF, dS ->
                viewModel.updateMacrosChecklist(
                    bTitle, bP, bC, bF, bS,
                    lTitle, lP, lC, lF, lS,
                    preTitle, preP, preC, preF, preS,
                    postTitle, postP, postC, postF, postS,
                    dTitle, dP, dC, dF, dS
                )
                showMacroEditDialog = false
            },
            onDismiss = { showMacroEditDialog = false }
        )
    }

    // Dynamic Macro Target Computations & Aggregated Progress Tracker
    val calculatedPostCarbs = profile.weightKg
    val targetBProtein = remember(profile.breakfastProtein) { parseMacroTargetGrams(profile.breakfastProtein, 40.0) }
    val targetLProtein = remember(profile.lunchProtein) { parseMacroTargetGrams(profile.lunchProtein, 40.0) }
    val targetPreProtein = remember(profile.preWorkoutProtein) { parseMacroTargetGrams(profile.preWorkoutProtein, 40.0) }
    val targetPostProtein = remember(profile.postWorkoutProtein) { parseMacroTargetGrams(profile.postWorkoutProtein, 40.0) }
    val targetDProtein = remember(profile.dinnerProtein) { parseMacroTargetGrams(profile.dinnerProtein, 40.0) }
    val totalTargetProtein = targetBProtein + targetLProtein + targetPreProtein + targetPostProtein + targetDProtein

    val targetBCarbs = remember(profile.breakfastCarbs) { parseMacroTargetGrams(profile.breakfastCarbs, 60.0) }
    val targetLCarbs = remember(profile.lunchCarbs) { parseMacroTargetGrams(profile.lunchCarbs, 100.0) }
    val targetPreCarbs = remember(profile.preWorkoutCarbs) { parseMacroTargetGrams(profile.preWorkoutCarbs, 50.0) }
    val targetPostCarbs = remember(profile.postWorkoutCarbs) { parseMacroTargetGrams(profile.postWorkoutCarbs, calculatedPostCarbs) }
    val targetDCarbs = remember(profile.dinnerCarbs) { parseMacroTargetGrams(profile.dinnerCarbs, 50.0) }
    val totalTargetCarbs = targetBCarbs + targetLCarbs + targetPreCarbs + targetPostCarbs + targetDCarbs

    val targetBFats = remember(profile.breakfastFats) { parseMacroTargetGrams(profile.breakfastFats, 20.0) }
    val targetLFats = remember(profile.lunchFats) { parseMacroTargetGrams(profile.lunchFats, 20.0) }
    val targetPreFats = remember(profile.preWorkoutFats) { parseMacroTargetGrams(profile.preWorkoutFats, 0.0) }
    val targetPostFats = remember(profile.postWorkoutFats) { parseMacroTargetGrams(profile.postWorkoutFats, 0.0) }
    val targetDFats = remember(profile.dinnerFats) { parseMacroTargetGrams(profile.dinnerFats, 15.0) }
    val totalTargetFats = targetBFats + targetLFats + targetPreFats + targetPostFats + targetDFats

    val totalActualProtein = macroLog.breakfastProteinG + macroLog.lunchProteinG + macroLog.preWorkoutProteinG + macroLog.postWorkoutProteinG + macroLog.dinnerProteinG
    val totalActualCarbs = macroLog.breakfastCarbsG + macroLog.lunchCarbsG + macroLog.preWorkoutCarbsG + macroLog.postWorkoutCarbsG + macroLog.dinnerCarbsG
    val totalActualFats = macroLog.breakfastFatsG + macroLog.lunchFatsG + macroLog.preWorkoutFatsG + macroLog.postWorkoutFatsG + macroLog.dinnerFatsG

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            GymAppHeader()
            Spacer(modifier = Modifier.height(8.dp))

            // Macro Calendar Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GymSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, GymDivider, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.changeMacroDate(-1) }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Day", tint = Color.White)
                }
                Text(
                    text = "Macros Checklist for: \n${formatDateString(viewModel.macroSelectedDateString)}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = { viewModel.changeMacroDate(1) }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day", tint = Color.White)
                }
            }
        }

        // Daily Tracker stats
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GymSurfaceElevated),
                border = BorderStroke(1.dp, GymDivider)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Nutrient Intake Engine (Page 9 Rules)",
                        color = GymHighVizYellow,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Est. Calories", color = GymMuted, fontSize = 11.sp)
                            Text("$totalCaloriesMock kcal", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                        Column {
                            Text("Daily Water intake", color = GymMuted, fontSize = 11.sp)
                            Text("${macroLog.waterGlasses * 250} ml / 3L", color = GymNeonCyan, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                        Column {
                            Text("Current Bodyweight", color = GymMuted, fontSize = 11.sp)
                            Text("${profile.weightKg} kg", color = GymHighVizYellow, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        // Checklist for Meals (Breakfast, Lunch, Dinner, Pre, Post)
        // Grand Dynamic Target vs Actual Macros Progress Status
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("grand_macros_visual_progress_card"),
                colors = CardDefaults.cardColors(containerColor = GymSurfaceElevated),
                border = BorderStroke(1.dp, GymDivider)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Daily Macros Tracker",
                            color = GymHighVizYellow,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(GymHighVizYellow.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            val completionPct = if (totalTargetProtein + totalTargetCarbs + totalTargetFats > 0) {
                                (((totalActualProtein + totalActualCarbs + totalActualFats) / (totalTargetProtein + totalTargetCarbs + totalTargetFats)) * 100).toInt()
                            } else 0
                            Text(
                                "Intake Match: $completionPct%",
                                color = GymHighVizYellow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    MacroProgressBarRow(
                        label = "Total Protein Intake",
                        actual = totalActualProtein,
                        target = totalTargetProtein,
                        color = GymNeonCyan,
                        testTag = "total_protein_progress"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    MacroProgressBarRow(
                        label = "Total Carbohydrates Intake",
                        actual = totalActualCarbs,
                        target = totalTargetCarbs,
                        color = GymHighVizYellow,
                        testTag = "total_carbs_progress"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    MacroProgressBarRow(
                        label = "Total Fats Intake",
                        actual = totalActualFats,
                        target = totalTargetFats,
                        color = GymSoftRed,
                        testTag = "total_fats_progress"
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Meal Program Checklist",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { showMacroEditDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = GymHighVizYellow),
                    modifier = Modifier.testTag("modify_macros_checklist_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Macros Checklist",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Modify Checklist", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            MealCheckCard(
                title = profile.breakfastTitle,
                protein = profile.breakfastProtein,
                carbs = profile.breakfastCarbs,
                fats = profile.breakfastFats,
                suggestions = profile.breakfastSuggestions,
                actualProtein = macroLog.breakfastProteinG,
                actualCarbs = macroLog.breakfastCarbsG,
                actualFats = macroLog.breakfastFatsG,
                onUpdateIntake = { p, c, f -> viewModel.updateMealMacros("breakfast", p, c, f) },
                isChecked = macroLog.breakfastChecked,
                onCheckedChange = { viewModel.toggleMacro("breakfast") }
            )
        }

        item {
            MealCheckCard(
                title = profile.lunchTitle,
                protein = profile.lunchProtein,
                carbs = profile.lunchCarbs,
                fats = profile.lunchFats,
                suggestions = profile.lunchSuggestions,
                actualProtein = macroLog.lunchProteinG,
                actualCarbs = macroLog.lunchCarbsG,
                actualFats = macroLog.lunchFatsG,
                onUpdateIntake = { p, c, f -> viewModel.updateMealMacros("lunch", p, c, f) },
                isChecked = macroLog.lunchChecked,
                onCheckedChange = { viewModel.toggleMacro("lunch") }
            )
        }

        item {
            MealCheckCard(
                title = profile.preWorkoutTitle,
                protein = profile.preWorkoutProtein,
                carbs = profile.preWorkoutCarbs,
                fats = profile.preWorkoutFats,
                suggestions = profile.preWorkoutSuggestions,
                actualProtein = macroLog.preWorkoutProteinG,
                actualCarbs = macroLog.preWorkoutCarbsG,
                actualFats = macroLog.preWorkoutFatsG,
                onUpdateIntake = { p, c, f -> viewModel.updateMealMacros("preWorkout", p, c, f) },
                isChecked = macroLog.preWorkoutChecked,
                onCheckedChange = { viewModel.toggleMacro("preWorkout") }
            )
        }

        item {
            val carbsDisplay = if (profile.postWorkoutCarbs.lowercase().contains("calculated")) {
                "${postWorkoutCarbTarget}g (Calculated: 1g/kg based on weight)"
            } else {
                profile.postWorkoutCarbs
            }
            MealCheckCard(
                title = profile.postWorkoutTitle,
                protein = profile.postWorkoutProtein,
                carbs = carbsDisplay,
                fats = profile.postWorkoutFats,
                suggestions = profile.postWorkoutSuggestions,
                actualProtein = macroLog.postWorkoutProteinG,
                actualCarbs = macroLog.postWorkoutCarbsG,
                actualFats = macroLog.postWorkoutFatsG,
                onUpdateIntake = { p, c, f -> viewModel.updateMealMacros("postWorkout", p, c, f) },
                isChecked = macroLog.postWorkoutChecked,
                onCheckedChange = { viewModel.toggleMacro("postWorkout") },
                calculatedPostCarbs = postWorkoutCarbTarget.toDouble()
            )
        }

        item {
            MealCheckCard(
                title = profile.dinnerTitle,
                protein = profile.dinnerProtein,
                carbs = profile.dinnerCarbs,
                fats = profile.dinnerFats,
                suggestions = profile.dinnerSuggestions,
                actualProtein = macroLog.dinnerProteinG,
                actualCarbs = macroLog.dinnerCarbsG,
                actualFats = macroLog.dinnerFatsG,
                onUpdateIntake = { p, c, f -> viewModel.updateMealMacros("dinner", p, c, f) },
                isChecked = macroLog.dinnerChecked,
                onCheckedChange = { viewModel.toggleMacro("dinner") }
            )
        }

        // Hydration tracker (Tip #9 detail)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GymSurface),
                border = BorderStroke(1.dp, GymDivider)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Drink Filtered Water (Tip #9)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "8-10 glasses (approx 2.5-3.5L) is recommended.",
                            color = GymMuted,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Progress bar for water
                        LinearProgressIndicator(
                            progress = { (macroLog.waterGlasses.toFloat() / 10f).coerceIn(0f, 1f) },
                            color = GymNeonCyan,
                            trackColor = GymDivider,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.adjustWater(-1) },
                            modifier = Modifier
                                .size(32.dp)
                                .background(GymSurfaceElevated, CircleShape)
                        ) {
                            Text("-", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "${macroLog.waterGlasses}",
                            color = GymNeonCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(
                            onClick = { viewModel.adjustWater(1) },
                            modifier = Modifier
                                .size(32.dp)
                                .background(GymHighVizYellow, CircleShape)
                        ) {
                            Text("+", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Quick Food Sources Reference from Page 9
        item {
            Text(
                "Approved PDF Source Library",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GymSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🥚 Protein Sources:", color = GymHighVizYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Chicken Breast, Eggs, Whey Isolate, Soya Chunks, Oatmeal, Nuts butter, Legumes, Lentils, Salmon", color = Color.White, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("🥖 Carb Sources:", color = GymNeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Brown Bread, Oats, Rice, Roti, Banana, and sweet potatoes", color = Color.White, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("🥦 Micronutrients & Minerals:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Avocados, Spinach, Broccoli, Cucumber, Mushrooms, Fruits, and Other green veggies", color = GymMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun MealCheckCard(
    title: String,
    protein: String,
    carbs: String,
    fats: String,
    suggestions: String,
    actualProtein: Double,
    actualCarbs: Double,
    actualFats: Double,
    onUpdateIntake: (Double, Double, Double) -> Unit,
    isChecked: Boolean,
    onCheckedChange: () -> Unit,
    calculatedPostCarbs: Double = 80.0
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    // Parse target values
    val targetP = remember(protein) { parseMacroTargetGrams(protein, 40.0) }
    val targetC = remember(carbs) { parseMacroTargetGrams(carbs, calculatedPostCarbs) }
    val targetF = remember(fats) { parseMacroTargetGrams(fats, 15.0) }
    
    // Inputs
    var pInput by remember(actualProtein) { mutableStateOf(if (actualProtein > 0) actualProtein.toString() else "") }
    var cInput by remember(actualCarbs) { mutableStateOf(if (actualCarbs > 0) actualCarbs.toString() else "") }
    var fInput by remember(actualFats) { mutableStateOf(if (actualFats > 0) actualFats.toString() else "") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("meal_card_${title.replace(Regex("[^a-zA-Z0-9]"), "_").lowercase()}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) GymSurfaceElevated else GymSurface
        ),
        border = BorderStroke(1.dp, if (isChecked) GymHighVizYellow else GymDivider)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { onCheckedChange() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = GymHighVizYellow,
                        checkmarkColor = Color.Black
                    ),
                    modifier = Modifier.testTag("meal_checkbox_${title.replace(Regex("[^a-zA-Z0-9]"), "_").lowercase()}")
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = if (isChecked) GymHighVizYellow else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("P: $protein", color = GymNeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("C: $carbs", color = GymHighVizYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("F: $fats", color = GymMuted, fontSize = 11.sp)
                    }
                    if (actualProtein > 0 || actualCarbs > 0 || actualFats > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Logged ->", color = GymMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("P: ${actualProtein}g", color = GymNeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("C: ${actualCarbs}g", color = GymHighVizYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("F: ${GymSoftRed}g", color = GymSoftRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        suggestions,
                        color = GymMuted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand to edit intake",
                        tint = GymMuted
                    )
                }
            }
            
            // Progress indicators & Inputs section when expanded
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GymSurfaceElevated.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Track Actual Intake (g)", color = GymHighVizYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = pInput,
                            onValueChange = { pInput = it },
                            label = { Text("Actual Protein", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = GymSurface,
                                unfocusedContainerColor = GymSurface,
                                focusedIndicatorColor = GymNeonCyan
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("actual_protein_input_${title.replace(Regex("[^a-zA-Z0-9]"), "_").lowercase()}")
                        )
                        OutlinedTextField(
                            value = cInput,
                            onValueChange = { cInput = it },
                            label = { Text("Actual Carbs", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = GymSurface,
                                unfocusedContainerColor = GymSurface,
                                focusedIndicatorColor = GymHighVizYellow
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("actual_carbs_input_${title.replace(Regex("[^a-zA-Z0-9]"), "_").lowercase()}")
                        )
                        OutlinedTextField(
                            value = fInput,
                            onValueChange = { fInput = it },
                            label = { Text("Actual Fats", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = GymSurface,
                                unfocusedContainerColor = GymSurface,
                                focusedIndicatorColor = GymSoftRed
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("actual_fats_input_${title.replace(Regex("[^a-zA-Z0-9]"), "_").lowercase()}")
                        )
                    }

                    // Progress Bars
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val parsedPActual = pInput.toDoubleOrNull() ?: 0.0
                        val parsedCActual = cInput.toDoubleOrNull() ?: 0.0
                        val parsedFActual = fInput.toDoubleOrNull() ?: 0.0

                        MealProgressBarRow(
                            label = "Protein Progress",
                            actual = parsedPActual,
                            target = targetP,
                            color = GymNeonCyan
                        )
                        MealProgressBarRow(
                            label = "Carbs Progress",
                            actual = parsedCActual,
                            target = targetC,
                            color = GymHighVizYellow
                        )
                        MealProgressBarRow(
                            label = "Fats Progress",
                            actual = parsedFActual,
                            target = targetF,
                            color = GymSoftRed
                        )
                    }

                    Button(
                        onClick = {
                            val actP = pInput.toDoubleOrNull() ?: 0.0
                            val actC = cInput.toDoubleOrNull() ?: 0.0
                            val actF = fInput.toDoubleOrNull() ?: 0.0
                            onUpdateIntake(actP, actC, actF)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GymNeonCyan),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("save_intake_button_${title.replace(Regex("[^a-zA-Z0-9]"), "_").lowercase()}")
                    ) {
                        Text("Save Intake Logs", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Helper methods for target extraction and progress layout:
fun parseMacroTargetGrams(targetString: String, calculatedVal: Double): Double {
    if (targetString.lowercase().contains("calculated")) {
        return calculatedVal
    }
    val numbers = Regex("\\d+").findAll(targetString).map { it.value.toDoubleOrNull() ?: 0.0 }.toList()
    return when {
        numbers.isEmpty() -> 40.0
        numbers.size >= 2 -> numbers[1] // upper bound limit (e.g. "30-40" -> 40)
        else -> if (numbers[0] == 0.0) 1.0 else numbers[0]
    }
}

@Composable
fun MacroProgressBarRow(
    label: String,
    actual: Double,
    target: Double,
    color: Color,
    testTag: String
) {
    val progress = if (target > 0) (actual / target).toFloat().coerceIn(0f, 1f) else 0f
    val percent = if (target > 0) ((actual / target) * 100).toInt() else 0
    Column(modifier = Modifier.fillMaxWidth().testTag(testTag)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                "${String.format(Locale.getDefault(), "%.1f", actual)}g / ${String.format(Locale.getDefault(), "%.1f", target)}g ($percent%)",
                color = GymMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = GymDivider
        )
    }
}

@Composable
fun MealProgressBarRow(
    label: String,
    actual: Double,
    target: Double,
    color: Color
) {
    val progress = if (target > 0) (actual / target).toFloat().coerceIn(0f, 1f) else 0f
    val percent = if (target > 0) ((actual / target) * 100).toInt() else 0
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label ($percent%):", 
            color = GymMuted, 
            fontSize = 10.sp, 
            modifier = Modifier.width(100.dp)
        )
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = GymDivider
        )
        Text(
            text = "${String.format(Locale.getDefault(), "%.1f", actual)}g / ${String.format(Locale.getDefault(), "%.1f", target)}g", 
            color = Color.White, 
            fontSize = 10.sp, 
            fontWeight = FontWeight.Bold
        )
    }
}

// -------------------------------------------------------------------------------------------------
// 3. ROADMAP & 10 CRITICAL TIPS SCREEN
// -------------------------------------------------------------------------------------------------
@Composable
fun RoadmapScreen(viewModel: IronMassViewModel, allLogs: List<WorkoutLog>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            GymAppHeader()
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Program Structure Timeline",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                "The 4-Week Advanced Periodization (Cycle 1-3 to hit 12 Weeks)",
                color = GymMuted,
                fontSize = 11.sp
            )
        }

        // Week Maps with stats
        items((1..4).toList()) { w ->
            val isActiveWeek = w == viewModel.currentWeek
            val isHybrid = w % 2 != 0
            val weekTitle = if (isHybrid) "WEEK $w (Hybrid Split)" else "WEEK $w (PPL Split)"
            val bg = if (isActiveWeek) GymSurfaceElevated else GymSurface
            val borderCol = if (isActiveWeek) GymHighVizYellow else GymDivider

            val logsInWeek = allLogs.filter { it.weekNumber == w }
            val setsDone = logsInWeek.size

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectWorkout(w, 1) },
                colors = CardDefaults.cardColors(containerColor = bg),
                border = BorderStroke(1.dp, borderCol)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = weekTitle,
                            color = if (isActiveWeek) GymHighVizYellow else Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isActiveWeek) {
                            Box(
                                modifier = Modifier
                                    .background(GymNeonCyan, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "ACTIVE",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isHybrid) "Alternating muscle-focused hypertrophy (D1-3 High Reps) and power-building compound movements (D4-6 Heavy Sets)."
                        else "Movement-pattern focus (Pull, Push, Legs) to eliminate muscle overlap and load heavy compound aggregates.",
                        color = GymMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Sets Completed: $setsDone done",
                            color = GymNeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Tap to Track Week $w →",
                            color = GymHighVizYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // The 10 Important Tips from Page 9
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "10 Important Mastery Tips (Page 9 Rules)",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }

        val masteryTips = listOf(
            Pair("1. Progressive Adaptation", "By repeating movement patterns, your nervous system gets efficient. Over time, you build immense neurological output and lift heavier loads."),
            Pair("2. Maximum Volitional Output", "Lift as heavy as possible while strictly maintaining proper, safe form. The last few reps of each set must feel challenging but achievable!"),
            Pair("3. Recovery & Repair Day", "Take a rest after three consecutive training days instead of wait until the weekend. Muscle recovery is where synthesis takes place!"),
            Pair("4. Progressive Overload Goal", "Aim to increase total weights used each succeeding week. Even a 1kg increase is a success."),
            Pair("5. Support a Calorie Surplus", "You cannot gain substantial muscle on deficits. Maintain a slight calorie surplus with optimized macros as pre-loaded in tab 2."),
            Pair("6. Creatine Monohydrate", "Consider utilizing creatine monohydrate to maximize cellular ATP pool and cellular water retention/hydration."),
            Pair("7. Strict Routine Discipline", "You do not need to constantly change exercises to shock the muscle. Stick to these specific compound lifts for at least 12 weeks to gauge gains."),
            Pair("8. Progressive Target Effort", "Easy workouts don't trigger hypertrophy. Force the muscle to adapt by training close to biological failure (RPE 8-9)."),
            Pair("9. Drink Filtered Water", "Keep yourself hydrated throughout the entire day. Hydration prevents muscle cramping and promotes nutrient transport."),
            Pair("10. Macro Precision Balance", "If you undereat, you won't grow. If you overeat, you will gain excessive fat. Adhere to the pre-loaded macros to stay lean but grow mass.")
        )

        items(masteryTips) { (title, description) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = GymSurface),
                border = BorderStroke(1.dp, GymDivider)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        title,
                        color = GymHighVizYellow,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        description,
                        color = Color.White,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EditMacrosChecklistDialog(
    profile: UserProfile,
    onSave: (
        breakfastTitle: String, breakfastProtein: String, breakfastCarbs: String, breakfastFats: String, breakfastSuggestions: String,
        lunchTitle: String, lunchProtein: String, lunchCarbs: String, lunchFats: String, lunchSuggestions: String,
        preWorkoutTitle: String, preWorkoutProtein: String, preWorkoutCarbs: String, preWorkoutFats: String, preWorkoutSuggestions: String,
        postWorkoutTitle: String, postWorkoutProtein: String, postWorkoutCarbs: String, postWorkoutFats: String, postWorkoutSuggestions: String,
        dinnerTitle: String, dinnerProtein: String, dinnerCarbs: String, dinnerFats: String, dinnerSuggestions: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var bTitle by remember { mutableStateOf(profile.breakfastTitle) }
    var bProtein by remember { mutableStateOf(profile.breakfastProtein) }
    var bCarbs by remember { mutableStateOf(profile.breakfastCarbs) }
    var bFats by remember { mutableStateOf(profile.breakfastFats) }
    var bSugg by remember { mutableStateOf(profile.breakfastSuggestions) }

    var lTitle by remember { mutableStateOf(profile.lunchTitle) }
    var lProtein by remember { mutableStateOf(profile.lunchProtein) }
    var lCarbs by remember { mutableStateOf(profile.lunchCarbs) }
    var lFats by remember { mutableStateOf(profile.lunchFats) }
    var lSugg by remember { mutableStateOf(profile.lunchSuggestions) }

    var preTitle by remember { mutableStateOf(profile.preWorkoutTitle) }
    var preProtein by remember { mutableStateOf(profile.preWorkoutProtein) }
    var preCarbs by remember { mutableStateOf(profile.preWorkoutCarbs) }
    var preFats by remember { mutableStateOf(profile.preWorkoutFats) }
    var preSugg by remember { mutableStateOf(profile.preWorkoutSuggestions) }

    var postTitle by remember { mutableStateOf(profile.postWorkoutTitle) }
    var postProtein by remember { mutableStateOf(profile.postWorkoutProtein) }
    var postCarbs by remember { mutableStateOf(profile.postWorkoutCarbs) }
    var postFats by remember { mutableStateOf(profile.postWorkoutFats) }
    var postSugg by remember { mutableStateOf(profile.postWorkoutSuggestions) }

    var dTitle by remember { mutableStateOf(profile.dinnerTitle) }
    var dProtein by remember { mutableStateOf(profile.dinnerProtein) }
    var dCarbs by remember { mutableStateOf(profile.dinnerCarbs) }
    var dFats by remember { mutableStateOf(profile.dinnerFats) }
    var dSugg by remember { mutableStateOf(profile.dinnerSuggestions) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Modify Macros Checklist",
                color = GymHighVizYellow,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        containerColor = GymSurface,
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Group 1: Breakfast
                MealEditSection(
                    sectionTitle = "Breakfast Options",
                    title = bTitle, onTitleChange = { bTitle = it },
                    protein = bProtein, onProteinChange = { bProtein = it },
                    carbs = bCarbs, onCarbsChange = { bCarbs = it },
                    fats = bFats, onFatsChange = { bFats = it },
                    sugg = bSugg, onSuggChange = { bSugg = it }
                )

                HorizontalDivider(color = GymDivider, thickness = 1.dp)

                // Group 2: Lunch
                MealEditSection(
                    sectionTitle = "Lunch Options",
                    title = lTitle, onTitleChange = { lTitle = it },
                    protein = lProtein, onProteinChange = { lProtein = it },
                    carbs = lCarbs, onCarbsChange = { lCarbs = it },
                    fats = lFats, onFatsChange = { lFats = it },
                    sugg = lSugg, onSuggChange = { lSugg = it }
                )

                HorizontalDivider(color = GymDivider, thickness = 1.dp)

                // Group 3: Pre-Workout
                MealEditSection(
                    sectionTitle = "Pre-Workout Options",
                    title = preTitle, onTitleChange = { preTitle = it },
                    protein = preProtein, onProteinChange = { preProtein = it },
                    carbs = preCarbs, onCarbsChange = { preCarbs = it },
                    fats = preFats, onFatsChange = { preFats = it },
                    sugg = preSugg, onSuggChange = { preSugg = it }
                )

                HorizontalDivider(color = GymDivider, thickness = 1.dp)

                // Group 4: Post-Workout
                MealEditSection(
                    sectionTitle = "Post-Workout Options",
                    title = postTitle, onTitleChange = { postTitle = it },
                    protein = postProtein, onProteinChange = { postProtein = it },
                    carbs = postCarbs, onCarbsChange = { postCarbs = it },
                    fats = postFats, onFatsChange = { postFats = it },
                    sugg = postSugg, onSuggChange = { postSugg = it }
                )

                HorizontalDivider(color = GymDivider, thickness = 1.dp)

                // Group 5: Dinner
                MealEditSection(
                    sectionTitle = "Dinner Options",
                    title = dTitle, onTitleChange = { dTitle = it },
                    protein = dProtein, onProteinChange = { dProtein = it },
                    carbs = dCarbs, onCarbsChange = { dCarbs = it },
                    fats = dFats, onFatsChange = { dFats = it },
                    sugg = dSugg, onSuggChange = { dSugg = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        bTitle, bProtein, bCarbs, bFats, bSugg,
                        lTitle, lProtein, lCarbs, lFats, lSugg,
                        preTitle, preProtein, preCarbs, preFats, preSugg,
                        postTitle, postProtein, postCarbs, postFats, postSugg,
                        dTitle, dProtein, dCarbs, dFats, dSugg
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = GymHighVizYellow),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("apply_macros_checklist_modifications")
            ) {
                Text("Apply Modifications", color = Color.Black, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_macros_checklist_modifications")
            ) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}

@Composable
fun MealEditSection(
    sectionTitle: String,
    title: String, onTitleChange: (String) -> Unit,
    protein: String, onProteinChange: (String) -> Unit,
    carbs: String, onCarbsChange: (String) -> Unit,
    fats: String, onFatsChange: (String) -> Unit,
    sugg: String, onSuggChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(sectionTitle, color = GymNeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Meal Label", color = GymMuted) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = GymSurfaceElevated,
                unfocusedContainerColor = GymSurfaceElevated,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = GymNeonCyan
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = protein,
                onValueChange = onProteinChange,
                label = { Text("Protein (P)", color = GymMuted, fontSize = 11.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = GymSurfaceElevated,
                    unfocusedContainerColor = GymSurfaceElevated,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = GymNeonCyan
                ),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = carbs,
                onValueChange = onCarbsChange,
                label = { Text("Carbs (C)", color = GymMuted, fontSize = 11.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = GymSurfaceElevated,
                    unfocusedContainerColor = GymSurfaceElevated,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = GymNeonCyan
                ),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = fats,
                onValueChange = onFatsChange,
                label = { Text("Fats (F)", color = GymMuted, fontSize = 11.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = GymSurfaceElevated,
                    unfocusedContainerColor = GymSurfaceElevated,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = GymNeonCyan
                ),
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = sugg,
            onValueChange = onSuggChange,
            label = { Text("Suggestions", color = GymMuted) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = GymSurfaceElevated,
                unfocusedContainerColor = GymSurfaceElevated,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = GymNeonCyan
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun BodyMetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = GymMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun ProfileForm(
    profile: UserProfile,
    onSave: (
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
    ) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nameInput by remember { mutableStateOf(profile.name) }
    var ageInput by remember { mutableStateOf(profile.age.toString()) }
    var heightInput by remember { mutableStateOf(profile.heightCm.toString()) }
    var weightInput by remember { mutableStateOf(profile.weightKg.toString()) }
    var chestInput by remember { mutableStateOf(profile.chestSizeCm.toString()) }
    var bicepInput by remember { mutableStateOf(profile.bicepSizeCm.toString()) }
    var waistInput by remember { mutableStateOf(profile.waistSizeCm.toString()) }
    var thighInput by remember { mutableStateOf(profile.thighSizeCm.toString()) }
    var calfInput by remember { mutableStateOf(profile.calfSizeCm.toString()) }
    var emergencyContactInput by remember { mutableStateOf(profile.emergencyContact) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("ATHLETE IDENTITY", color = GymMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        
        OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            label = { Text("Name", color = GymMuted) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = GymSurfaceElevated,
                unfocusedContainerColor = GymSurfaceElevated,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = GymNeonCyan
            ),
            modifier = Modifier.fillMaxWidth().testTag("edit_name_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = ageInput,
                onValueChange = { ageInput = it },
                label = { Text("Age", color = GymMuted) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = GymSurfaceElevated,
                    unfocusedContainerColor = GymSurfaceElevated,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = GymNeonCyan
                ),
                modifier = Modifier.weight(1f).testTag("edit_age_input")
            )

            OutlinedTextField(
                value = heightInput,
                onValueChange = { heightInput = it },
                label = { Text("Height (Cm)", color = GymMuted) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = GymSurfaceElevated,
                    unfocusedContainerColor = GymSurfaceElevated,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = GymNeonCyan
                ),
                modifier = Modifier.weight(1f).testTag("edit_height_input")
            )
        }

        OutlinedTextField(
            value = weightInput,
            onValueChange = { weightInput = it },
            label = { Text("Weight (Kg)", color = GymMuted) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = GymSurfaceElevated,
                unfocusedContainerColor = GymSurfaceElevated,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = GymNeonCyan
            ),
            modifier = Modifier.fillMaxWidth().testTag("edit_weight_input")
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text("BODY SIZES (CM)", color = GymMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = chestInput,
                onValueChange = { chestInput = it },
                label = { Text("Chest", color = GymMuted) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = GymSurfaceElevated,
                    unfocusedContainerColor = GymSurfaceElevated,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = GymNeonCyan
                ),
                modifier = Modifier.weight(1f).testTag("edit_chest_input")
            )

            OutlinedTextField(
                value = bicepInput,
                onValueChange = { bicepInput = it },
                label = { Text("Biceps (Arms)", color = GymMuted) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = GymSurfaceElevated,
                    unfocusedContainerColor = GymSurfaceElevated,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = GymNeonCyan
                ),
                modifier = Modifier.weight(1f).testTag("edit_bicep_input")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = waistInput,
                onValueChange = { waistInput = it },
                label = { Text("Waist", color = GymMuted) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = GymSurfaceElevated,
                    unfocusedContainerColor = GymSurfaceElevated,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = GymNeonCyan
                ),
                modifier = Modifier.weight(1f).testTag("edit_waist_input")
            )

            OutlinedTextField(
                value = thighInput,
                onValueChange = { thighInput = it },
                label = { Text("Thighs", color = GymMuted) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = GymSurfaceElevated,
                    unfocusedContainerColor = GymSurfaceElevated,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = GymNeonCyan
                ),
                modifier = Modifier.weight(1f).testTag("edit_thigh_input")
            )
        }

        OutlinedTextField(
            value = calfInput,
            onValueChange = { calfInput = it },
            label = { Text("Calves (Cm)", color = GymMuted) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = GymSurfaceElevated,
                unfocusedContainerColor = GymSurfaceElevated,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = GymNeonCyan
            ),
            modifier = Modifier.fillMaxWidth().testTag("edit_calf_input")
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text("SAFETY INFORMATION", color = GymSoftRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

        OutlinedTextField(
            value = emergencyContactInput,
            onValueChange = { emergencyContactInput = it },
            label = { Text("Emergency Contact No", color = GymMuted) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = GymSurfaceElevated,
                unfocusedContainerColor = GymSurfaceElevated,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = GymNeonCyan
            ),
            modifier = Modifier.fillMaxWidth().testTag("edit_emergency_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).testTag("profile_form_cancel_button")
            ) {
                Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    val parsedAge = ageInput.toIntOrNull() ?: profile.age
                    val parsedHeight = heightInput.toDoubleOrNull() ?: profile.heightCm
                    val parsedWeight = weightInput.toDoubleOrNull() ?: profile.weightKg
                    val parsedChest = chestInput.toDoubleOrNull() ?: profile.chestSizeCm
                    val parsedBicep = bicepInput.toDoubleOrNull() ?: profile.bicepSizeCm
                    val parsedWaist = waistInput.toDoubleOrNull() ?: profile.waistSizeCm
                    val parsedThigh = thighInput.toDoubleOrNull() ?: profile.thighSizeCm
                    val parsedCalf = calfInput.toDoubleOrNull() ?: profile.calfSizeCm

                    onSave(
                        nameInput.trim().ifEmpty { profile.name },
                        parsedAge,
                        parsedHeight,
                        parsedWeight,
                        parsedChest,
                        parsedBicep,
                        parsedWaist,
                        parsedThigh,
                        parsedCalf,
                        emergencyContactInput.trim()
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = GymHighVizYellow),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1.5f).testTag("profile_form_save_button")
            ) {
                Text("Save Changes", color = Color.Black, fontWeight = FontWeight.Black)
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 4. HISTORY, STATS AND PROFILE SETTINGS SCREEN
// -------------------------------------------------------------------------------------------------
@Composable
fun ProfileHistoryScreen(
    viewModel: IronMassViewModel,
    allLogs: List<WorkoutLog>,
    profile: UserProfile
) {
    var showEditDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = GymSurface,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 12.dp,
            title = {
                Text(
                    "Edit Profile & Metrics",
                    color = GymHighVizYellow,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            },
            text = {
                ProfileForm(
                    profile = profile,
                    onSave = { name, age, height, weight, chest, bicep, waist, thigh, calf, emergencyContact ->
                        viewModel.updateFullProfile(
                            name = name,
                            age = age,
                            heightCm = height,
                            weightKg = weight,
                            chestSizeCm = chest,
                            bicepSizeCm = bicep,
                            waistSizeCm = waist,
                            thighSizeCm = thigh,
                            calfSizeCm = calf,
                            emergencyContact = emergencyContact
                        )
                        showEditDialog = false
                    },
                    onCancel = { showEditDialog = false }
                )
            },
            confirmButton = {}
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            GymAppHeader()
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Settings & Session History",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
        }

        // Profile metrics
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GymSurface),
                border = BorderStroke(1.dp, GymDivider),
                modifier = Modifier.fillMaxWidth().testTag("athlete_profile_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Profile Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Initials Avatar
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .border(2.dp, GymHighVizYellow, CircleShape)
                                .background(GymSurfaceElevated, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val initials = if (profile.name.length >= 2) {
                                profile.name.take(2).uppercase()
                            } else if (profile.name.isNotEmpty()) {
                                profile.name.take(1).uppercase()
                            } else {
                                "GW"
                            }
                            Text(
                                initials,
                                color = GymHighVizYellow,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                profile.name,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.padding(top = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Age: ${profile.age}", color = GymMuted, fontSize = 12.sp)
                                Text("|", color = GymDivider, fontSize = 12.sp)
                                Text("Height: ${profile.heightCm}cm", color = GymMuted, fontSize = 12.sp)
                                Text("|", color = GymDivider, fontSize = 12.sp)
                                Text("Weight: ${profile.weightKg}kg", color = GymMuted, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = GymDivider, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Body Measurements Layout
                    Text(
                        "Body Measurements",
                        color = GymHighVizYellow,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BodyMetricItem("Chest", "${profile.chestSizeCm} cm")
                        BodyMetricItem("Biceps", "${profile.bicepSizeCm} cm")
                        BodyMetricItem("Waist", "${profile.waistSizeCm} cm")
                        BodyMetricItem("Thighs", "${profile.thighSizeCm} cm")
                        BodyMetricItem("Calves", "${profile.calfSizeCm} cm")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = GymDivider, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Emergency Contacts styled warningly
                    Surface(
                        color = GymSoftRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, GymSoftRed.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Emergency Info",
                                tint = GymSoftRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "EMERGENCY CONTACT",
                                    color = GymSoftRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    if (profile.emergencyContact.isBlank()) "Not Set" else profile.emergencyContact,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Edit All Details interactive button
                    Button(
                        onClick = { showEditDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = GymNeonCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("edit_full_profile_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Edit Full Profile",
                                color = Color.Black,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tip #3 reminder scheduling
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Daily Workout Reminder", color = GymMuted, fontSize = 11.sp)
                            Text("Receive motivational triggers daily", color = Color.White, fontSize = 12.sp)
                        }

                        Switch(
                            checked = viewModel.remindersEnabled,
                            onCheckedChange = { viewModel.toggleReminders() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GymHighVizYellow,
                                checkedTrackColor = GymHighVizYellow.copy(alpha = 0.3f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = GymDivider, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        "Workout Program Timeline & Split",
                        color = GymHighVizYellow,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Start Date: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(profile.programStartDate))}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "The program cycles automatically through a 4-week structure.\nWeek 1 & 3 focus on a Hybrid split. Week 2 & 4 focus on PPL.",
                        color = GymMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Simulate start date to verify different periodization weeks:",
                        color = GymNeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = {
                                val currentStart = profile.programStartDate
                                val oneWeekAgo = currentStart - (7L * 24 * 60 * 60 * 1000)
                                viewModel.updateProgramStartDate(oneWeekAgo)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = GymSurfaceElevated,
                            border = BorderStroke(1.dp, GymDivider),
                            modifier = Modifier.weight(1f).testTag("simulate_back_1_week")
                        ) {
                            Text(
                                "Back 1 Wk",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }

                        Surface(
                            onClick = {
                                val currentStart = profile.programStartDate
                                val twoWeeksAgo = currentStart - (14L * 24 * 60 * 60 * 1000)
                                viewModel.updateProgramStartDate(twoWeeksAgo)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = GymSurfaceElevated,
                            border = BorderStroke(1.dp, GymDivider),
                            modifier = Modifier.weight(1f).testTag("simulate_back_2_weeks")
                        ) {
                            Text(
                                "Back 2 Wks",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }

                        Surface(
                            onClick = {
                                viewModel.updateProgramStartDate(System.currentTimeMillis())
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = GymHighVizYellow,
                            modifier = Modifier.weight(1f).testTag("simulate_reset_today")
                        ) {
                            Text(
                                "Reset Today",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        // External helpful WebView resources from Page 10 of PDF
        item {
            Text(
                "PDF Links & Resources (Page 10)",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        val resourceLinks = listOf(
            Triple("1. Isolation Exercise Guide", "Interactive guide to target muscle groups directly with hypertrophy hacks.", "https://thefitnessphantom.com/list-of-isolation-exercise-guide/"),
            Triple("2. Dumbbell Exercise Directory", "Exhaustive workout directory containing only dumbbells per muscle.", "https://thefitnessphantom.com/dumbbell-exercises-for-each-muscle/"),
            Triple("3. Compound Barbell Catalog", "Master the heavy compound lifts (Bench, Squats, Overheads).", "https://thefitnessphantom.com/barbell-exercises-list-by-muscle-group/"),
            Triple("4. Push Pull Legs Ultimate Blueprint", "Check out detailed diagrams of weekly split allocations.", "https://thefitnessphantom.com/push-pull-legs-exercises-list/")
        )

        items(resourceLinks) { (title, desc, url) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(browserIntent)
                    },
                colors = CardDefaults.cardColors(containerColor = GymSurfaceElevated),
                border = BorderStroke(1.dp, GymDivider)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(GymSurface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Link", tint = GymNeonCyan, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(desc, color = GymMuted, fontSize = 10.sp, lineHeight = 13.sp)
                    }
                }
            }
        }

        // Recent logs history
        item {
            Text(
                "Completed Set Logs (${allLogs.size} Total)",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (allLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No sets logged yet. Start a workout in Gym Mode!",
                        color = GymMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(allLogs.take(30)) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = GymSurface),
                    border = BorderStroke(1.dp, GymDivider)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                log.exerciseName,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Week ${log.weekNumber} • Day ${log.dayNumber} • Set ${log.setNumber} • ${formatTimestamp(log.timestamp)}",
                                color = GymMuted,
                                fontSize = 10.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${log.weightUsed} kg × ${log.repsCompleted}",
                                color = GymHighVizYellow,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            IconButton(onClick = { viewModel.deleteLog(log.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = GymSoftRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// GENERAL UTILS & COMMON UI
// -------------------------------------------------------------------------------------------------
@Composable
fun GymAppHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "IRON MASS",
                color = GymHighVizYellow,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Text(
                "HYBRID / PPL SPLIT PERIODIZATION",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }

        Box(
            modifier = Modifier
                .background(GymNeonCyan.copy(alpha = 0.15f), CircleShape)
                .border(1.dp, GymNeonCyan, CircleShape)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                "CYC 1-3",
                color = GymNeonCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun formatDateString(dateStr: String): String {
    try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = format.parse(dateStr) ?: return dateStr
        val displayFormat = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
        return displayFormat.format(date)
    } catch (e: Exception) {
        return dateStr
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// -------------------------------------------------------------------------------------------------
// CONFIGURABLE REST TIMER DIALOGS
// -------------------------------------------------------------------------------------------------
@Composable
fun RestTimerConfigDialog(
    currentOverwrite: Int,
    onSave: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectCustom by remember { mutableStateOf(currentOverwrite > 0) }
    var secondsText by remember { mutableStateOf(if (currentOverwrite > 0) currentOverwrite.toString() else "90") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Rest Timer Periodization",
                color = GymHighVizYellow,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        containerColor = GymSurfaceElevated,
        titleContentColor = GymHighVizYellow,
        textContentColor = Color.White,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Configure custom rest period to maintain training intensity and progressive overload between sets.",
                    color = GymMuted,
                    fontSize = 12.sp
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectCustom = false }
                        .background(if (!selectCustom) GymSurface else Color.Transparent, RoundedCornerShape(8.dp))
                        .border(1.dp, if (!selectCustom) GymHighVizYellow else GymDivider, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !selectCustom,
                        onClick = { selectCustom = false },
                        colors = RadioButtonDefaults.colors(selectedColor = GymHighVizYellow)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "Programmed Defaults",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Uses intervals prescribed by the program (e.g. 120s for compounds, 30s for core).",
                            color = GymMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectCustom = true }
                        .background(if (selectCustom) GymSurface else Color.Transparent, RoundedCornerShape(8.dp))
                        .border(1.dp, if (selectCustom) GymHighVizYellow else GymDivider, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectCustom,
                        onClick = { selectCustom = true },
                        colors = RadioButtonDefaults.colors(selectedColor = GymHighVizYellow)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Custom Overwrite Duration",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Set a single custom rest limit for all sets.",
                            color = GymMuted,
                            fontSize = 10.sp
                        )
                        
                        if (selectCustom) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = secondsText,
                                onValueChange = { secondsText = it.filter { char -> char.isDigit() } },
                                label = { Text("Rest Seconds", fontSize = 11.sp, color = GymMuted) },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = GymNeonCyan,
                                    unfocusedIndicatorColor = GymDivider,
                                    focusedContainerColor = GymSurface,
                                    unfocusedContainerColor = GymSurface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("custom_rest_seconds_text_field")
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectCustom) {
                        val secs = secondsText.toIntOrNull() ?: 90
                        onSave(secs)
                    } else {
                        onSave(0)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GymHighVizYellow),
                modifier = Modifier.testTag("save_rest_timer_config_button")
            ) {
                Text("Apply Settings", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_rest_config_button")) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}

@Composable
fun RestCompletedDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Alert",
                    tint = GymHighVizYellow,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Rest Completed!",
                    color = GymHighVizYellow,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        containerColor = GymSurfaceElevated,
        titleContentColor = GymHighVizYellow,
        textContentColor = Color.White,
        text = {
            Text(
                "Your rest interval is finished. Tap 'Start Next Set' and execute immediately to maintain the target periodization intensity for maximum mass gains!",
                color = Color.White,
                fontSize = 13.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GymNeonCyan),
                modifier = Modifier.testTag("dismiss_rest_completed_alert")
            ) {
                Text("Start Next Set", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}
