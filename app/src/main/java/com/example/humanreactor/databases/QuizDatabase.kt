package com.example.humanreactor.databases

import androidx.room.Database
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.Room

@Database(entities = [PerformanceRecord::class], version = 1, exportSchema = false)
abstract class QuizDatabase : RoomDatabase() {

    companion object {
        @Volatile
        private var INSTANCE: QuizDatabase? = null

        fun getDatabase(context: Context): QuizDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuizDatabase::class.java,
                    "quiz_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
