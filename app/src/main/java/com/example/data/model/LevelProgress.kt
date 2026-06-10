package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "level_progress")
data class LevelProgress(
    @PrimaryKey val levelId: Int,
    val completed: Boolean = false,
    val unlocked: Boolean = false,
    val stars: Int = 0,
    val bestTimeMs: Long = 0L
)
