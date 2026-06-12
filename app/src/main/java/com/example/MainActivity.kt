package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.database.AppDatabase
import com.example.data.repository.WorkoutRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.IronMassViewModel
import com.example.ui.viewmodel.IronMassViewModelFactory
import com.example.ui.workout.WorkoutTrackerScreen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val database = AppDatabase.getDatabase(this)
    val dao = database.workoutDao()
    val repository = WorkoutRepository(dao)
    val viewModelFactory = IronMassViewModelFactory(repository)
    val viewModel = ViewModelProvider(this, viewModelFactory)[IronMassViewModel::class.java]

    setContent {
      MyApplicationTheme {
        WorkoutTrackerScreen(viewModel)
      }
    }
  }
}
