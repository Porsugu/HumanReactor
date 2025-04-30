package com.example.humanreactor.databases

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "performance_records")
data class QuizRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val category: String,
    val correctRate: Double,
    val timeUsed: Double,
)