package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.LevelProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [LevelProgress::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun levelProgressDao(): LevelProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dibujayescapa_database"
                )
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDatabase(database.levelProgressDao())
                    }
                }
            }

            suspend fun populateDatabase(dao: LevelProgressDao) {
                // Initialize level progress for 50 levels
                val initialLevels = List(50) { i ->
                    val levelId = i + 1
                    LevelProgress(
                        levelId = levelId,
                        completed = false,
                        unlocked = levelId == 1, // Only level 1 is unlocked initially
                        stars = 0,
                        bestTimeMs = 0L
                    )
                }
                dao.insertAllProgress(initialLevels)
            }
        }
    }
}
