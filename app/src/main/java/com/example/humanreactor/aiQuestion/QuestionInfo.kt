package com.example.humanreactor.aiQuestion

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

/*
\n[.]
[*]:1+1=?\n
[A]:1\n
[B]:2\n
[C]:3\n
[D]:4\n
[_]:2\n
[;]:1+1當然是=2!\n
\[.]
 */
class QuestionInfo (context : Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("quiz_data", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val LIST_KEY = "quiz_questions_list"

    // get the prompt text, make it into small questions and save it
    fun parseAndSaveQuestions(promptText: String) {
        val questions = parsePromptText(promptText)
        saveQuestions(questions)
    }

    // analyse the prompt words
    private fun parsePromptText(promptText: String): List<QuizQuestion> {
        val questions = mutableListOf<QuizQuestion>()

        // when we have [*], this could cut the question, and we could get all the questions in a block
        val lines = promptText.split("\n[.]")
            .map{it.trim()}
            .filter{ it.isNotEmpty()}

        // making local variables that to store the quiz questions
        var question = ""
        val options = mutableMapOf<String, String>()
        var correctAnswer = ""
        var explanation = ""

        // analyse each question
        for(line in lines){
            // make a value of Quiz Question

            when{
                //questions
                line.startsWith("[*]:") ->{
                    question = line.substringAfter("[*]:").trim()
                }

                //options [A] to [D]
                line.matches(Regex("\\[([A-D])\\]:(.+)")) -> {
                    // Options (A, B, C, D)
                    val matches = Regex("\\[([A-D])\\]:(.+)").find(line)
                    if (matches != null) {
                        val optionLabel = matches.groupValues[1]
                        val optionText = matches.groupValues[2].trim()
                        options[optionLabel] = optionText
                    }
                }

                // correct answer
                line.startsWith("[_]:") -> {
                    correctAnswer = line.substringAfter("[_]:")
                }

                //explanation
                line.startsWith("[;]:")->{
                    explanation = line.substringAfter("[;]:")
                }

            }
        }

//        // verify we have all required components
//        if (question.isNotEmpty() && options.isNotEmpty() && correctAnswer.isNotEmpty()) {
//
//            // Find the full text of the correct answer option
//            val correctAnswerText = options.entries.find {
//                it.key == correctAnswer || it.value == correctAnswer
//            }?.value ?: correctAnswer
//
//            return QuizQuestion(question, options, correctAnswerText, explanation)
//        }
//        return null



        return questions

    }

//    // analyse single question and put it into the data class
//    private fun parseQuestionBlock(block: String): QuizQuestion? {
//
//    }
//
//    // 將新的選擇題添加到現有列表中
//    fun addQuestions(questions: List<QuizQuestion>) {
//        val existingQuestions = getQuestions().toMutableList()
//        existingQuestions.addAll(questions)
//        saveQuestions(existingQuestions)
//    }

    // 儲存選擇題列表
    fun saveQuestions(questions: List<QuizQuestion>) {
        val json = gson.toJson(questions)
        sharedPreferences.edit().putString(LIST_KEY, json).apply()
    }

//    // 獲取所有選擇題
//    fun getQuestions(): List<QuizQuestion> {
//        val json = sharedPreferences.getString(LIST_KEY, null) ?: return emptyList()
//        val type = object : TypeToken<List<QuizQuestion>>() {}.type
//        return try {
//            gson.fromJson(json, type)
//        } catch (e: Exception) {
//            e.printStackTrace()
//            emptyList()
//        }
//    }

    // 清空所有選擇題
    fun clearAllQuestions() {
        sharedPreferences.edit().remove(LIST_KEY).apply()
    }






}