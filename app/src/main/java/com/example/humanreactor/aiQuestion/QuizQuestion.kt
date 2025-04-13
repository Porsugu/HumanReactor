package com.example.humanreactor.aiQuestion

// this is the data class for quiz question after generated
data class QuizQuestion(
    var question: String,
    val options: List<String>,
    val correctAnswerIndex: String,
    val explanation: String
)
