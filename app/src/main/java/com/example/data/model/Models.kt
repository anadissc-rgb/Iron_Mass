package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val weekNumber: Int,    // 1 to 4
    val cycleNumber: Int = 1, // 1 to 3
    val dayNumber: Int,     // 1 to 7
    val exerciseName: String,
    val setNumber: Int,
    val weightUsed: Double,
    val repsCompleted: Int,
    val weightUnit: String = "kg",
    val isCompleted: Boolean = true
)

@Entity(tableName = "macro_logs")
data class MacroLog(
    @PrimaryKey val dateString: String, // "YYYY-MM-DD"
    val breakfastChecked: Boolean = false,
    val lunchChecked: Boolean = false,
    val dinnerChecked: Boolean = false,
    val preWorkoutChecked: Boolean = false,
    val postWorkoutChecked: Boolean = false,
    val waterGlasses: Int = 0,
    
    // Actual logged macros per meal (grams)
    val breakfastProteinG: Double = 0.0,
    val breakfastCarbsG: Double = 0.0,
    val breakfastFatsG: Double = 0.0,

    val lunchProteinG: Double = 0.0,
    val lunchCarbsG: Double = 0.0,
    val lunchFatsG: Double = 0.0,

    val preWorkoutProteinG: Double = 0.0,
    val preWorkoutCarbsG: Double = 0.0,
    val preWorkoutFatsG: Double = 0.0,

    val postWorkoutProteinG: Double = 0.0,
    val postWorkoutCarbsG: Double = 0.0,
    val postWorkoutFatsG: Double = 0.0,

    val dinnerProteinG: Double = 0.0,
    val dinnerCarbsG: Double = 0.0,
    val dinnerFatsG: Double = 0.0
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val weightKg: Double = 80.0,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val remindersEnabled: Boolean = true,
    val programStartDate: Long = System.currentTimeMillis(),
    val name: String = "Gym Warrior",
    val age: Int = 25,
    val heightCm: Double = 175.0,
    val chestSizeCm: Double = 100.0,
    val bicepSizeCm: Double = 38.0,
    val waistSizeCm: Double = 82.0,
    val thighSizeCm: Double = 58.0,
    val calfSizeCm: Double = 36.0,
    val emergencyContact: String = "911",
    
    // Customizable Macro Checklist Targets
    val breakfastTitle: String = "1. Breakfast Checklist",
    val breakfastProtein: String = "30-40g",
    val breakfastCarbs: String = "50-60g",
    val breakfastFats: String = "15-20g",
    val breakfastSuggestions: String = "Oatmeal with whey, whole eggs, fruit",

    val lunchTitle: String = "2. Lunch Checklist",
    val lunchProtein: String = "30-40g",
    val lunchCarbs: String = "70-100g",
    val lunchFats: String = "15-20g",
    val lunchSuggestions: String = "Chicken breast, long white/brown rice, mixed veggies",

    val preWorkoutTitle: String = "3. Pre-Workout (30 mins prior)",
    val preWorkoutProtein: String = "30-40g",
    val preWorkoutCarbs: String = "40-50g",
    val preWorkoutFats: String = "0g",
    val preWorkoutSuggestions: String = "Soya Chunks or Egg Whites with sweet potatoes or banana",

    val postWorkoutTitle: String = "4. Post-Workout Recovery",
    val postWorkoutProtein: String = "At least 30-40g (Crucial Synthesis)",
    val postWorkoutCarbs: String = "Calculated: 1g/kg based on weight",
    val postWorkoutFats: String = "0g",
    val postWorkoutSuggestions: String = "Whey isolate, salmon with ragi / potatoes",

    val dinnerTitle: String = "5. Dinner Checklist",
    val dinnerProtein: String = "30-40g",
    val dinnerCarbs: String = "40-50g",
    val dinnerFats: String = "10-15g",
    val dinnerSuggestions: String = "Salmon or paneer, baked broccoli & leafy greens"
)
