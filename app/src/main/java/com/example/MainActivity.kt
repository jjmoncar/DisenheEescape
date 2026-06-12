package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.db.AppDatabase
import com.example.data.repository.LevelRepository
import com.example.ui.game.GameApp
import com.example.ui.game.GameViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Initialize Google AdMob SDK
    AdMobManager.initialize(applicationContext)

    setContent {
      MyApplicationTheme {
        val context = LocalContext.current.applicationContext
        val database = AppDatabase.getDatabase(context)
        val repository = LevelRepository(database.levelProgressDao())
        val factory = GameViewModel.Factory(application, repository)
        val gameViewModel: GameViewModel = viewModel(factory = factory)

        GameApp(viewModel = gameViewModel)
      }
    }
  }
}
