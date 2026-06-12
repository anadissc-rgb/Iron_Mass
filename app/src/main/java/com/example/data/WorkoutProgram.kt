package com.example.data

data class ExerciseInfo(
    val id: String,
    val name: String,
    val targetMuscle: String,
    val repsAndSets: String, // Display text e.g. "15 x 3" or "8, 6, 4, 2"
    val numSets: Int,
    val defaultReps: String,
    val restSeconds: Int,
    val isHeavy: Boolean,
    val isPyramid: Boolean = false
)

data class WorkoutDay(
    val title: String,
    val subtitle: String,
    val exercises: List<ExerciseInfo>,
    val isRestDay: Boolean = false
)

object WorkoutProgram {
    
    fun getWorkout(week: Int, day: Int): WorkoutDay {
        // week is 1-4 (recycles), day is 1-7
        return when (week) {
            1 -> getWeek1Workout(day)
            2 -> getWeek2Workout(day)
            3 -> getWeek3Workout(day)
            4 -> getWeek4Workout(day)
            else -> getWeek1Workout(day)
        }
    }

    private fun getWeek1Workout(day: Int): WorkoutDay {
        return when (day) {
            1 -> WorkoutDay(
                title = "Day 1 - Chest, Triceps, and Core",
                subtitle = "Hybrid Split • High Reps (Hypertrophy)",
                exercises = listOf(
                    ExerciseInfo("w1d1_1", "Flat Barbell Bench Press", "Chest", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w1d1_2", "Dumbbell Incline Press", "Chest", "12 x 3", 3, "12", 120, false),
                    ExerciseInfo("w1d1_3", "Seated Pec Deck Fly", "Chest", "12 x 3", 3, "12", 120, false),
                    ExerciseInfo("w1d1_4", "Dumbbell Pullover", "Chest", "12 x 3", 3, "12", 120, false),
                    ExerciseInfo("w1d1_5", "Triangle Push up", "Triceps", "10-15 x 2", 2, "12", 120, false),
                    ExerciseInfo("w1d1_6", "Rope Pushdown", "Triceps", "12 x 3", 3, "12", 120, false),
                    ExerciseInfo("w1d1_7", "Triceps Bench Dips", "Triceps", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w1d1_8", "Front Plank", "Core", "60s x 1", 1, "60s", 30, false),
                    ExerciseInfo("w1d1_9", "Side Plank", "Core", "30s/side x 1", 1, "30s", 30, false)
                )
            )
            2 -> WorkoutDay(
                title = "Day 2 - Back and Biceps",
                subtitle = "Hybrid Split • High Reps (Hypertrophy)",
                exercises = listOf(
                    ExerciseInfo("w1d2_1", "Pullup", "Back", "AMRAP x 3", 3, "AMRAP", 120, false),
                    ExerciseInfo("w1d2_2", "Wide Grip Front Lat Pulldown", "Back", "15 x 3", 3, "15", 180, false),
                    ExerciseInfo("w1d2_3", "V Grip Front Lat Pulldown", "Back", "12 x 2", 2, "12", 120, false),
                    ExerciseInfo("w1d2_4", "Seated Cable Rowing", "Back", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w1d2_5", "Face Pull", "Back", "12 x 3", 3, "12", 120, false),
                    ExerciseInfo("w1d2_6", "Barbell Bicep Curl", "Biceps", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w1d2_7", "Hammer Curl", "Biceps", "10 x 3", 3, "10", 120, false)
                )
            )
            3 -> WorkoutDay(
                title = "Day 3 - Legs and Shoulder",
                subtitle = "Hybrid Split • High Reps (Hypertrophy)",
                exercises = listOf(
                    ExerciseInfo("w1d3_1", "Barbell Back Squat", "Legs", "15 x 3", 3, "15", 180, false),
                    ExerciseInfo("w1d3_2", "Leg Extension", "Legs", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w1d3_3", "Hamstring Curl", "Legs", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w1d3_4", "Calf Raises", "Legs", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w1d3_5", "Barbell Overhead Press", "Shoulders", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w1d3_6", "Dumbbell Lateral Raise", "Shoulders", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w1d3_7", "Reverse Pec Deck Fly", "Shoulders", "15 x 3", 3, "15", 120, false)
                )
            )
            4 -> WorkoutDay(
                title = "Day 4 - Chest and Triceps",
                subtitle = "Hybrid Split • Heavy Sets (Maximum Tens)",
                exercises = listOf(
                    ExerciseInfo("w1d4_1", "Flat Barbell Bench Press", "Chest", "6-8 x 4", 4, "6-8", 180, true),
                    ExerciseInfo("w1d4_2", "Dumbbell Incline Press", "Chest", "6-8 x 4", 4, "6-8", 120, true),
                    ExerciseInfo("w1d4_3", "Pec Deck/Decline Cable Fly", "Chest", "8 x 3", 3, "8", 120, true),
                    ExerciseInfo("w1d4_4", "Dumbbell Pullover", "Chest", "8 x 3", 3, "8", 120, true),
                    ExerciseInfo("w1d4_5", "Bar Dips", "Triceps", "AMRAP x 3", 3, "AMRAP", 120, true),
                    ExerciseInfo("w1d4_6", "Barbell Skull Crusher", "Triceps", "10 x 3", 3, "10", 120, true),
                    ExerciseInfo("w1d4_7", "Rope Pushdown", "Triceps", "10 x 3", 3, "10", 120, true),
                    ExerciseInfo("w1d4_8", "Tricep Kickback", "Triceps", "8 x 3", 3, "8", 120, true)
                )
            )
            5 -> WorkoutDay(
                title = "Day 5 - Back, Biceps, and Core",
                subtitle = "Hybrid Split • Heavy Sets (Maximum Tens)",
                exercises = listOf(
                    ExerciseInfo("w1d5_1", "Deadlift", "Back", "8, 6, 4, 2", 4, "8", 120, true, isPyramid = true),
                    ExerciseInfo("w1d5_2", "Wide Grip Pulldown", "Back", "6-8 x 4", 4, "6-8", 120, true),
                    ExerciseInfo("w1d5_3", "Seated Cable Rowing", "Back", "6-8 x 4", 4, "6-8", 120, true),
                    ExerciseInfo("w1d5_4", "Barbell Bent-over Row", "Back", "6-8 x 4", 4, "6-8", 120, true),
                    ExerciseInfo("w1d5_5", "One-arm Dumbbell Row", "Back", "8 x 3", 3, "8", 120, true),
                    ExerciseInfo("w1d5_6", "Chin-ups", "Biceps", "AMRAP x 3", 3, "AMRAP", 120, true),
                    ExerciseInfo("w1d5_7", "EZ Bar Bicep Curl", "Biceps", "10 x 3", 3, "10", 120, true),
                    ExerciseInfo("w1d5_8", "Concentration Curl", "Biceps", "10 x 3", 3, "10", 120, true)
                )
            )
            6 -> WorkoutDay(
                title = "Day 6 - Shoulder and Legs",
                subtitle = "Hybrid Split • Heavy Sets (Maximum Tens)",
                exercises = listOf(
                    ExerciseInfo("w1d6_1", "Arnold Press", "Shoulders", "8 x 3", 3, "8", 120, true),
                    ExerciseInfo("w1d6_2", "Dumbbell Lateral Raise", "Shoulders", "8 x 3", 3, "8", 120, true),
                    ExerciseInfo("w1d6_3", "Rear Delt Fly", "Shoulders", "8 x 3", 3, "8", 120, true),
                    ExerciseInfo("w1d6_4", "Upright Row", "Shoulders", "8 x 3", 3, "8", 120, true),
                    ExerciseInfo("w1d6_5", "Shrug", "Shoulders", "8 x 3", 3, "8", 90, true),
                    ExerciseInfo("w1d6_6", "Dumbbell Lunges/Sumo Squat", "Legs", "10 x 3", 3, "10", 120, true),
                    ExerciseInfo("w1d6_7", "Leg Press", "Legs", "8-10 x 4", 4, "8-10", 90, true),
                    ExerciseInfo("w1d6_8", "Hip Thrust/Step-up", "Legs", "10 x 3", 3, "10", 90, true)
                )
            )
            else -> WorkoutDay(title = "Day 7 - Rest Day", subtitle = "Recovery & Nutrition Focus", exercises = emptyList(), isRestDay = true)
        }
    }

    private fun getWeek2Workout(day: Int): WorkoutDay {
        return when (day) {
            1 -> WorkoutDay(
                title = "Day 1 - Pull Workout",
                subtitle = "PPL Split • High Reps (Hypertrophy)",
                exercises = listOf(
                    ExerciseInfo("w2d1_1", "Pull-up", "Back", "10 x 3", 3, "10", 120, false),
                    ExerciseInfo("w2d1_2", "Wide Grip Lat Pulldown", "Back", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w2d1_3", "Seated Cable Rowing", "Back", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w2d1_4", "Decline Dumbbell Pullover", "Back", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w2d1_5", "EZ Bar Standing Bicep Curl", "Biceps", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w2d1_6", "Incline Dumbbell Curl", "Biceps", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w2d1_7", "Dumbbell Hammer Curl", "Biceps", "15 x 3", 3, "15", 120, false)
                )
            )
            2 -> WorkoutDay(
                title = "Day 2 - Push Workout",
                subtitle = "PPL Split • High Reps (Hypertrophy)",
                exercises = listOf(
                    ExerciseInfo("w2d2_1", "Barbell Flat Bench Press", "Chest", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w2d2_2", "Incline Dumbbell Bench Press", "Chest", "12 x 3", 3, "12", 120, false),
                    ExerciseInfo("w2d2_3", "Parallel Bar Dips", "Chest", "AMRAP x 3", 3, "AMRAP", 120, false),
                    ExerciseInfo("w2d2_4", "Dumbbell Front Raises", "Shoulders", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w2d2_5", "Dumbbell Lateral Raises", "Shoulders", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w2d2_6", "Bench Dips", "Triceps", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w2d2_7", "Triceps Rope/Bar Pushdown", "Triceps", "15 x 3", 3, "15", 120, false)
                )
            )
            3 -> WorkoutDay(
                title = "Day 3 - Legs and Core",
                subtitle = "PPL Split • High Reps (Hypertrophy)",
                exercises = listOf(
                    ExerciseInfo("w2d3_1", "Barbell Back Squat", "Legs", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w2d3_2", "Dumbbell Step-up/Lunges", "Legs", "12 x 3", 3, "12", 120, false),
                    ExerciseInfo("w2d3_3", "Leg Extension", "Legs", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w2d3_4", "Hamstring Curl", "Legs", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w2d3_5", "Calf Raises", "Legs", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w2d3_6", "Dead Bug Crunches", "Core", "10 x 2", 2, "10", 60, false),
                    ExerciseInfo("w2d3_7", "Plank", "Core", "60s x 1", 1, "60s", 0, false)
                )
            )
            4 -> WorkoutDay(
                title = "Day 4 - Pull Workout",
                subtitle = "PPL Split • Heavy (High Load / Strength)",
                exercises = listOf(
                    ExerciseInfo("w2d4_1", "Deadlift", "Back", "6, 4, 4, 2", 4, "6", 180, true, isPyramid = true),
                    ExerciseInfo("w2d4_2", "Wide Grip Lat Pulldown", "Back", "6-8 x 4", 4, "6-8", 180, true),
                    ExerciseInfo("w2d4_3", "Seated Cable Rowing", "Back", "6-8 x 4", 4, "6-8", 120, true),
                    ExerciseInfo("w2d4_4", "Bent-over Barbell Row", "Back", "6-8 x 4", 4, "6-8", 120, true),
                    ExerciseInfo("w2d4_5", "Barbell T Rowing", "Back", "6-8 x 4", 4, "6-8", 120, true),
                    ExerciseInfo("w2d4_6", "Chin-ups", "Biceps", "AMRAP x 3", 3, "AMRAP", 120, true),
                    ExerciseInfo("w2d4_7", "Standing Barbell Curl", "Biceps", "6-8 x 3", 3, "6-8", 120, true),
                    ExerciseInfo("w2d4_8", "Cable Curl", "Biceps", "6-8 x 3", 3, "6-8", 120, true),
                    ExerciseInfo("w2d4_9", "Preacher Curl", "Biceps", "6-8 x 3", 3, "6-8", 120, true)
                )
            )
            5 -> WorkoutDay(
                title = "Day 5 - Push Workout",
                subtitle = "PPL Split • Heavy (High Load / Strength)",
                exercises = listOf(
                    ExerciseInfo("w2d5_1", "Barbell Flat Bench Press", "Chest", "6-8 x 4", 4, "6-8", 120, true),
                    ExerciseInfo("w2d5_2", "Incline Dumbbell Press", "Chest", "6-8 x 4", 4, "6-8", 120, true),
                    ExerciseInfo("w2d5_3", "Pec Deck Fly", "Chest", "6-8 x 4", 4, "6-8", 60, true),
                    ExerciseInfo("w2d5_4", "Dumbbell Overhead Press", "Shoulders", "6-8 x 4", 4, "6-8", 120, true),
                    ExerciseInfo("w2d5_5", "Dumbbell Lateral Raises", "Shoulders", "6-8 x 4", 4, "6-8", 120, true),
                    ExerciseInfo("w2d5_6", "Weighted Bench Dips", "Triceps", "8-10 x 3", 3, "8-10", 120, true),
                    ExerciseInfo("w2d5_7", "Skull Crusher", "Triceps", "8-10 x 3", 3, "8-10", 120, true),
                    ExerciseInfo("w2d5_8", "Rope Pushdown", "Triceps", "8-10 x 3", 3, "8-10", 120, true)
                )
            )
            6 -> WorkoutDay(
                title = "Day 6 - Legs and Core",
                subtitle = "PPL Split • Heavy (High Load / Strength)",
                exercises = listOf(
                    ExerciseInfo("w2d6_1", "Back Squat", "Legs", "6-10 x 4", 4, "6-10", 180, true),
                    ExerciseInfo("w2d6_2", "Machine Leg Press", "Legs", "8-10 x 4", 4, "8-10", 120, true),
                    ExerciseInfo("w2d6_3", "Sumo Squat", "Legs", "8-10 x 3", 3, "8-10", 120, true),
                    ExerciseInfo("w2d6_4", "Romanian Deadlift", "Legs", "6-8 x 4", 4, "6-8", 120, true),
                    ExerciseInfo("w2d6_5", "Hip Thrust", "Legs", "8-10 x 3", 3, "8-10", 120, true),
                    ExerciseInfo("w2d6_6", "Calf Raises", "Legs", "10 x 3", 3, "10", 120, true)
                )
            )
            else -> WorkoutDay(title = "Day 7 - Rest Day", subtitle = "Recovery & Nutrition Focus", exercises = emptyList(), isRestDay = true)
        }
    }

    private fun getWeek3Workout(day: Int): WorkoutDay {
        return when (day) {
            1 -> WorkoutDay(
                title = "Day 1 - Chest, Triceps, and Core",
                subtitle = "Hybrid Split • High Reps (Hypertrophy)",
                exercises = listOf(
                    ExerciseInfo("w3d1_1", "Flat Barbell Bench Press", "Chest", "15 x 3", 3, "15", 150, false),
                    ExerciseInfo("w3d1_2", "Incline Barbell Bench Press", "Chest", "12 x 3", 3, "12", 120, false),
                    ExerciseInfo("w3d1_3", "Dumbbell/Machine Fly", "Chest", "12 x 3", 3, "12", 120, false),
                    ExerciseInfo("w3d1_4", "High to Low Cable Fly", "Chest", "12 x 3", 3, "12", 120, false),
                    ExerciseInfo("w3d1_5", "Triangle Push up", "Triceps", "10 x 3", 3, "10", 120, false),
                    ExerciseInfo("w3d1_6", "Rope Pushdown", "Triceps", "12 x 3", 3, "12", 90, false),
                    ExerciseInfo("w3d1_7", "Hanging Knee Raise", "Core", "10 x 2", 2, "10", 60, false),
                    ExerciseInfo("w3d1_8", "Front Plank", "Core", "60s x 1", 1, "60s", 30, false),
                    ExerciseInfo("w3d1_9", "Side Plank", "Core", "30s/side x 1", 1, "30s", 30, false)
                )
            )
            2 -> WorkoutDay(
                title = "Day 2 - Back and Biceps",
                subtitle = "Hybrid Split • High Reps (Hypertrophy)",
                exercises = listOf(
                    ExerciseInfo("w3d2_1", "Pullup", "Back", "10 x 3", 3, "10", 120, false),
                    ExerciseInfo("w3d2_2", "Wide Grip Front Lat Pulldown", "Back", "15 x 3", 3, "15", 180, false),
                    ExerciseInfo("w3d2_3", "V Grip Front Lat Pulldown", "Back", "12 x 3", 3, "12", 120, false),
                    ExerciseInfo("w3d2_4", "Seated Cable Rowing", "Back", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w3d2_5", "Face Pull", "Back", "12 x 3", 3, "12", 120, false),
                    ExerciseInfo("w3d2_6", "Barbell Bicep Curl", "Biceps", "12 x 3", 3, "12", 120, false),
                    ExerciseInfo("w3d2_7", "Incline Dumbbell Curl", "Biceps", "12 x 3", 3, "12", 120, false),
                    ExerciseInfo("w3d2_8", "Hammer Curl", "Biceps", "12 x 3", 3, "12", 120, false)
                )
            )
            3 -> WorkoutDay(
                title = "Day 3 - Legs and Shoulder",
                subtitle = "Hybrid Split • High Reps (Hypertrophy)",
                exercises = listOf(
                    ExerciseInfo("w3d3_1", "Machine Leg Press", "Legs", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w3d3_2", "Leg Extension", "Legs", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w3d3_3", "Hamstring Curl", "Legs", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w3d3_4", "Landmine Squat", "Legs", "15 x 3", 3, "15", 120, false),
                    ExerciseInfo("w3d3_5", "Dumbbell Front Raises", "Shoulders", "12 x 3", 3, "12", 120, false),
                    ExerciseInfo("w3d3_6", "Dumbbell Lateral Raise", "Shoulders", "12 x 3", 3, "12", 90, false),
                    ExerciseInfo("w3d3_7", "Reverse Pec Deck Fly", "Shoulders", "12 x 3", 3, "12", 90, false)
                )
            )
            4 -> WorkoutDay(
                title = "Day 4 - Chest and Triceps",
                subtitle = "Hybrid Split • Heavy Sets (Maximum Tens)",
                exercises = listOf(
                    ExerciseInfo("w3d4_1", "Flat Barbell Bench Press", "Chest", "6-8 x 4", 4, "6-8", 180, true),
                    ExerciseInfo("w3d4_2", "Dumbbell Incline Press", "Chest", "6-8 x 4", 4, "6-8", 120, true),
                    ExerciseInfo("w3d4_3", "Dumbbell Decline Bench Press", "Chest", "6-8 x 3", 3, "6-8", 120, true),
                    ExerciseInfo("w3d4_4", "Dumbbell Pullover", "Chest", "6-8 x 3", 3, "6-8", 120, true),
                    ExerciseInfo("w3d4_5", "Bar Dips", "Triceps", "10 x 3", 3, "10", 120, true),
                    ExerciseInfo("w3d4_6", "Dumbbell/Rope Overhead Triceps Extension", "Triceps", "8-10 x 3", 3, "8-10", 120, true),
                    ExerciseInfo("w3d4_7", "Rope Pushdown", "Triceps", "8-10 x 3", 3, "8-10", 120, true),
                    ExerciseInfo("w3d4_8", "Tricep Kickback", "Triceps", "8-10 x 3", 3, "8-10", 120, true)
                )
            )
            5 -> WorkoutDay(
                title = "Day 5 - Back, Biceps, and Core",
                subtitle = "Hybrid Split • Heavy Sets (Maximum Tens)",
                exercises = listOf(
                    ExerciseInfo("w3d5_1", "Deadlift", "Back", "4-6 x 4", 4, "4-6", 150, true),
                    ExerciseInfo("w3d5_2", "Wide Grip Lat Pulldown", "Back", "12 x 3", 3, "12", 120, true),
                    ExerciseInfo("w3d5_3", "Narrow Grip Pulldown", "Back", "10 x 3", 3, "10", 120, true),
                    ExerciseInfo("w3d5_4", "Seated Cable Rowing", "Back", "12 x 3", 3, "12", 120, true),
                    ExerciseInfo("w3d5_5", "Barbell T Rowing", "Back", "10 x 3", 3, "10", 120, true),
                    ExerciseInfo("w3d5_6", "Chin-ups", "Biceps", "AMRAP x 3", 3, "AMRAP", 120, true),
                    ExerciseInfo("w3d5_7", "EZ Bar Bicep Curl", "Biceps", "12 x 3", 3, "12", 120, true),
                    ExerciseInfo("w3d5_8", "Cable Curl", "Biceps", "12 x 3", 3, "12", 120, true),
                    ExerciseInfo("w3d5_9", "Preacher Curl", "Biceps", "12 x 3", 3, "12", 120, true)
                )
            )
            6 -> WorkoutDay(
                title = "Day 6 - Shoulder and Legs",
                subtitle = "Hybrid Split • Heavy Sets (Maximum Tens)",
                exercises = listOf(
                    ExerciseInfo("w3d6_1", "Barbell Overhead Press", "Shoulders", "6-8 x 4", 4, "6-8", 120, true),
                    ExerciseInfo("w3d6_2", "Dumbbell Lateral Raises", "Shoulders", "6-8 x 4", 4, "6-8", 120, true),
                    ExerciseInfo("w3d6_3", "Seated Bent-over Rear Delt Fly", "Shoulders", "6-8 x 3", 3, "6-8", 120, true),
                    ExerciseInfo("w3d6_4", "Shrug", "Shoulders", "8-10 x 3", 3, "8-10", 120, true),
                    ExerciseInfo("w3d6_5", "Barbell Back Squat", "Legs", "6-8 x 4", 4, "6-8", 120, true),
                    ExerciseInfo("w3d6_6", "Dumbbell Sumo Squat", "Legs", "8-10 x 3", 3, "8-10", 120, true),
                    ExerciseInfo("w3d6_7", "Romanian Deadlift", "Legs", "6-8 x 3", 3, "6-8", 120, true),
                    ExerciseInfo("w3d6_8", "Hip Thrust", "Legs", "6-8 x 3", 3, "6-8", 120, true)
                )
            )
            else -> WorkoutDay(title = "Day 7 - Rest Day", subtitle = "Recovery & Nutrition Focus", exercises = emptyList(), isRestDay = true)
        }
    }

    private fun getWeek4Workout(day: Int): WorkoutDay {
        // Week 4 is identical in exercises to Week 2 with same splits
        return getWeek2Workout(day)
    }
}
