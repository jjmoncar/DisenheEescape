package com.example.data.db

import androidx.room.*
import com.example.data.model.LevelProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface LevelProgressDao {
    @Query("SELECT * FROM level_progress ORDER BY levelId ASC")
    fun getAllProgress(): Flow<List<LevelProgress>>

    @Query("SELECT * FROM level_progress WHERE levelId = :levelId LIMIT 1")
    suspend fun getProgressById(levelId: Int): LevelProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: LevelProgress)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllProgress(progressList: List<LevelProgress>)

    @Query("UPDATE level_progress SET completed = 1, stars = MAX(stars, :stars), bestTimeMs = CASE WHEN bestTimeMs == 0 THEN :timeMs ELSE MIN(bestTimeMs, :timeMs) END WHERE levelId = :levelId")
    suspend fun completeLevel(levelId: Int, stars: Int, timeMs: Long)

    @Query("UPDATE level_progress SET unlocked = 1 WHERE levelId = :levelId")
    suspend fun unlockLevel(levelId: Int)

    @Query("UPDATE level_progress SET completed = 0, stars = 0, bestTimeMs = 0, unlocked = CASE WHEN levelId = 1 THEN 1 ELSE 0 END")
    suspend fun resetAllProgress()
}
