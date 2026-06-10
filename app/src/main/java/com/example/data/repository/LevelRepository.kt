package com.example.data.repository

import com.example.data.db.LevelProgressDao
import com.example.data.model.LevelProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LevelRepository(private val levelProgressDao: LevelProgressDao) {

    val allProgress: Flow<List<LevelProgress>> = levelProgressDao.getAllProgress()

    /**
     * Completes a level, updates achievements, and automatically unlocks the next level.
     */
    suspend fun completeLevel(levelId: Int, stars: Int, timeMs: Long) = withContext(Dispatchers.IO) {
        levelProgressDao.completeLevel(levelId, stars, timeMs)
        if (levelId < 50) {
            levelProgressDao.unlockLevel(levelId + 1)
        }
    }

    /**
     * Resets the entire game progress.
     */
    suspend fun resetAllProgress() = withContext(Dispatchers.IO) {
        levelProgressDao.resetAllProgress()
    }

    /**
     * Ensures all 50 levels exist in the database, populating them if they are missing.
     * This provides a fail-safe backup for the Room onCreate callback.
     */
    suspend fun ensureDatabasePopulated() = withContext(Dispatchers.IO) {
        val currentList = levelProgressDao.getAllProgress().firstOrNull() ?: emptyList()
        if (currentList.size < 50) {
            val progressMap = currentList.associateBy { it.levelId }
            val fullList = List(50) { index ->
                val levelId = index + 1
                progressMap[levelId] ?: LevelProgress(
                    levelId = levelId,
                    completed = false,
                    unlocked = levelId == 1,
                    stars = 0,
                    bestTimeMs = 0L
                )
            }
            levelProgressDao.insertAllProgress(fullList)
        }
    }
}
