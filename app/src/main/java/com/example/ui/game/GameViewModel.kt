package com.example.ui.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.LevelProgress
import com.example.data.repository.LevelRepository
import com.example.game.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.sqrt

sealed interface GameState {
    data object MainMenu : GameState
    data object LevelSelector : GameState
    data class Playing(val level: Level) : GameState
}

class GameViewModel(
    application: Application,
    private val repository: LevelRepository
) : AndroidViewModel(application) {

    // Fetch reactive Room progress records
    val levelProgressList: StateFlow<List<LevelProgress>> = repository.allProgress
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    var gameState: GameState by mutableStateOf(GameState.MainMenu)
        private set

    // Active simulation states
    var character: CharacterState by mutableStateOf(CharacterState(50f, 50f))
        private set

    // Multi-line drawn collection (in 0..100 unit coordinate system)
    val drawnLines = mutableStateListOf<List<Offset>>()

    // Active dragging line
    var currentLine = mutableStateListOf<Offset>()
        private set

    var remainingInk by mutableStateOf(100f) // Drains down from level max ink
    var maxInkForLevel by mutableStateOf(100f)
    var levelTimerMs by mutableStateOf(0L)
    var isSimulating by mutableStateOf(false) // Whether play/physics ticks are running
    var gameCompletedAllLevels by mutableStateOf(false)

    // Modal / Overlays
    var showVictoryScreen by mutableStateOf(false)
    var showFailureScreen by mutableStateOf(false)
    var calculatedStars by mutableStateOf(3)
    var finalTimeSec by mutableStateOf(0f)
    var showTutorialDialog by mutableStateOf(false)

    private var lastScratchTime = 0L
    private var lastBounceSoundTime = 0L

    init {
        viewModelScope.launch {
            // First boot checklist: assure 50 levels are generated with standard default states in Room
            repository.ensureDatabasePopulated()
        }
        
        // Auto-show tutorial if first-time user
        val prefs = application.getSharedPreferences("game_prefs", android.content.Context.MODE_PRIVATE)
        val hasSeenTutorial = prefs.getBoolean("has_seen_tutorial_v3", false)
        if (!hasSeenTutorial) {
            showTutorialDialog = true
        }
    }

    fun showTutorial() {
        showTutorialDialog = true
    }

    fun dismissTutorial() {
        showTutorialDialog = false
        val prefs = getApplication<Application>().getSharedPreferences("game_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("has_seen_tutorial_v3", true).apply()
    }

    fun navigateToSelector() {
        gameState = GameState.LevelSelector
        closeOverlays()
    }

    fun navigateToMainMenu() {
        gameState = GameState.MainMenu
        closeOverlays()
    }

    fun startPlayingLevel(level: Level) {
        gameState = GameState.Playing(level)
        loadLevel(level)
    }

    private fun loadLevel(level: Level) {
        closeOverlays()
        drawnLines.clear()
        currentLine.clear()
        character = CharacterState(
            x = level.startX,
            y = level.startY,
            vx = 0f,
            vy = 0f,
            activeGravityX = level.baseGravityX,
            activeGravityY = level.baseGravityY
        )
        maxInkForLevel = level.inkLimit
        remainingInk = level.inkLimit
        levelTimerMs = 0L
        isSimulating = true // Balls starts dropping instantly on load for snappy feedback!
    }

    fun toggleSimulation() {
        isSimulating = !isSimulating
    }

    /**
     * Resets the character ball back to safe start position *WITHOUT* wiping drawings
     * Extremely friendly UX for rapid retries on demanding levels.
     */
    fun retryCharacterOnly() {
        val activeLevelObj = getActiveLevel() ?: return
        character = CharacterState(
            x = activeLevelObj.startX,
            y = activeLevelObj.startY,
            vx = 0f,
            vy = 0f,
            activeGravityX = activeLevelObj.baseGravityX,
            activeGravityY = activeLevelObj.baseGravityY
        )
        showFailureScreen = false
        isSimulating = true
    }

    /**
     * Clears all drawn crayon lines and resets character position back to home
     */
    fun fullResetLevel() {
        val activeLevelObj = getActiveLevel() ?: return
        loadLevel(activeLevelObj)
        SketchAudioEngine.playCrumple()
    }

    private fun getActiveLevel(): Level? {
        val state = gameState
        return if (state is GameState.Playing) state.level else null
    }

    private fun closeOverlays() {
        showVictoryScreen = false
        showFailureScreen = false
    }

    // --- Pointer Inputs (Normalising incoming coordinates with canvas boundaries is handled in Composable) ---

    fun onDrawStart(normalizedPos: Offset) {
        if (showVictoryScreen || showFailureScreen) return
        if (remainingInk <= 0.5f) return

        currentLine.clear()
        currentLine.add(normalizedPos)
    }

    fun onDrawMove(normalizedPos: Offset) {
        if (showVictoryScreen || showFailureScreen) return
        if (remainingInk <= 0.5f) return
        if (currentLine.isEmpty()) return

        val lastPt = currentLine.last()
        val dx = normalizedPos.x - lastPt.x
        val dy = normalizedPos.y - lastPt.y
        val dist = sqrt(dx * dx + dy * dy)

        if (dist > 0.4f) { // Spacing step
            if (remainingInk >= dist) {
                remainingInk -= dist
                currentLine.add(normalizedPos)
                
                // Play beautiful pencil sketching sound
                val now = System.currentTimeMillis()
                if (now - lastScratchTime > 110L) {
                    lastScratchTime = now
                    SketchAudioEngine.playScratch()
                }
            } else {
                // Not enough ink: tap out
                val ratio = remainingInk / dist
                val midX = lastPt.x + dx * ratio
                val midY = lastPt.y + dy * ratio
                currentLine.add(Offset(midX, midY))
                remainingInk = 0f
                onDrawEnd()
            }
        }
    }

    fun onDrawEnd() {
        if (currentLine.size >= 2) {
            drawnLines.add(currentLine.toList())
        }
        currentLine.clear()
    }

    // --- Core Physics loop tick ---

    fun tick(dtSeconds: Float) {
        if (!isSimulating || showVictoryScreen || showFailureScreen) return
        val activeLevel = getActiveLevel() ?: return

        levelTimerMs += (dtSeconds * 1000L).toLong()

        // Cache state snapshot
        val mockState = character.copy()
        
        val oldVx = character.vx
        val oldVy = character.vy

        // Compute physics movement and updates
        PhysicsEngine.update(mockState, activeLevel, drawnLines, dtSeconds)

        // Commit modifications
        character = mockState

        if (character.isDead) {
            isSimulating = false
            showFailureScreen = true
            SketchAudioEngine.playCrumple()
        } else if (character.isVictorious) {
            isSimulating = false
            handleVictory(activeLevel)
        } else {
            // Check for bouncing / collisions
            if (mockState.vy == -34f && oldVy != -34f) {
                SketchAudioEngine.playSpring()
            } else {
                val dvx = mockState.vx - oldVx
                val dvy = mockState.vy - oldVy
                val deltaV = sqrt(dvx * dvx + dvy * dvy)
                if (deltaV > 3.8f) {
                    val now = System.currentTimeMillis()
                    if (now - lastBounceSoundTime > 160L) {
                        lastBounceSoundTime = now
                        SketchAudioEngine.playBounce()
                    }
                }
            }
        }
    }

    private fun handleVictory(level: Level) {
        // Calculate stars: More remaining ink = More stars!
        val inkEfficiency = remainingInk / maxInkForLevel
        calculatedStars = when {
            inkEfficiency >= 0.65f -> 3
            inkEfficiency >= 0.30f -> 2
            else -> 1
        }
        finalTimeSec = levelTimerMs / 1000f
        showVictoryScreen = true
        SketchAudioEngine.playVictory()

        viewModelScope.launch {
            // Save completion to database
            repository.completeLevel(level.id, calculatedStars, levelTimerMs)
        }
    }

    fun playNextLevel() {
        val currLevel = getActiveLevel() ?: return
        if (currLevel.id < 50) {
            val nextLvl = LevelManager.levels[currLevel.id] // Next level index is id (since 0-indexed matches current ID if id starts at 1)
            startPlayingLevel(nextLvl)
        } else {
            // Completed 50! Show credits
            gameState = GameState.LevelSelector
            closeOverlays()
        }
    }

    fun resetAllProgress() {
        viewModelScope.launch {
            repository.resetAllProgress()
            navigateToMainMenu()
        }
    }

    // --- Factory provider definition ---
    class Factory(
        private val application: Application,
        private val repository: LevelRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
                return GameViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
