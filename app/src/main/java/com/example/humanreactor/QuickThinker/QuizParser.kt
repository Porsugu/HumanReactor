package com.example.humanreactor.QuickThinker

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class QuizType (
    var question: String,
    val option: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String,
    val correctSentence  : String,
    val wrongSentences : String
)

class QuizParser{

    companion object {
        private const val PREFS_NAME = "QuizPreferences"
        private const val QUIZ_LIST_KEY = "quiz_list"

        /**
         * 從文本內容解析測驗題目
         * @param content 文本內容
         * @return quiz question list
         */

        fun parseQuizFromText(content: String): List<QuizType>{
            val quizList = mutableListOf<QuizType>()
            val lines = content.split("\n")

            var currentQuestion  = ""
            var currentOptions = mutableListOf<String>()
            var currentCorrectAnswer = ""
            var currentExplanation = ""
            var currentcorrectSentence  = ""
            var currentwrongSentence = ""
            var parsingState = ParsingState.NONE

            for(line in lines){
                val trimmedLine = line.trim()

                if(trimmedLine.isEmpty()) continue

                // setting the lines with the string letters
                when {
                    trimmedLine.startsWith("*") -> {

                        // if there are a full set of question, then get it into the list
                        if(currentQuestion.isNotEmpty() && currentOptions.isNotEmpty() &&
                            currentCorrectAnswer.isNotEmpty()){
                            quizList.add(
                                QuizType(
                                    question = currentQuestion.trim(),
                                    option = currentOptions.toList(),
                                    correctAnswerIndex = currentCorrectAnswer.trim().toInt(),
                                    explanation = currentExplanation.trim(),
                                    correctSentence = currentcorrectSentence.trim(),
                                    wrongSentences  = currentwrongSentence.trim()
                                )
                            )
                        } // end of if statement

                        // after inserting all the data, start a new question
                        currentQuestion = trimmedLine.substring(1).trim()
                        currentOptions = mutableListOf()
                        currentCorrectAnswer = ""
                        currentExplanation = ""
                        currentcorrectSentence  = ""
                        currentwrongSentence = ""
                        parsingState = ParsingState.QUESTION
                    } // end of trimmed line

                    // find let the option number gets into the list
                    trimmedLine.matches(Regex("^[1-4]\\..+")) -> {
                        val optionNumber = trimmedLine.substring(0,1)
                        val optionContent = trimmedLine.substring(2).trim()
                        currentOptions.add(optionContent)
                        parsingState = ParsingState.OPTIONS

                    } // options trimmed line end

                    // after options done and now goes with the correct answer
                    parsingState == ParsingState.OPTIONS && trimmedLine.startsWith("-") -> {
                        currentCorrectAnswer = trimmedLine.substring(1).trim()
                        parsingState = ParsingState.CORRECT_ANSWER
                    } // correct answer trimmed line end


                    // making the explanation
                    trimmedLine.startsWith("+") -> {
                        currentExplanation = trimmedLine.substring(1).trim()
                        parsingState = ParsingState.EXPLANATION
                    }

                    trimmedLine.startsWith("@") ->{
                        currentcorrectSentence = trimmedLine.substring(1).trim()
                        parsingState = ParsingState.CORRECT
                    }

                    trimmedLine.startsWith("#") -> {
                        currentwrongSentence = trimmedLine.substring(1).trim()
                    }

                    parsingState == ParsingState.QUESTION -> {
                        currentQuestion += " " + trimmedLine
                    }

                    parsingState == ParsingState.EXPLANATION -> {
                        currentExplanation += " " + trimmedLine
                    }

                    parsingState == ParsingState.CORRECT ->{
                        currentCorrectAnswer += " " + trimmedLine
                    }

                    parsingState == ParsingState.WRONG ->{
                        currentwrongSentence += " " + trimmedLine
                    }

                }
            } // end of for loop

            // adding the last question
            if(currentQuestion.isNotEmpty() && currentOptions.isNotEmpty() &&
                currentCorrectAnswer.isNotEmpty()){

                quizList.add(
                    QuizType(
                        question = currentQuestion.trim(),
                        option = currentOptions.toList(),
                        correctAnswerIndex = currentCorrectAnswer.trim().toInt(),
                        explanation = currentExplanation.trim(),
                        correctSentence = currentcorrectSentence.trim(),
                        wrongSentences  = currentwrongSentence.trim()

                    )
                )
            } // end of the last question if

            return quizList

        } // end of function

        /**
         * 將測驗題目列表保存到 SharedPreferences
         * @param context 上下文
         * @param quizList 測驗題目列表
         */
        fun saveQuizListToPrefs(context: Context, quizList: List<QuizType>) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            val gson = Gson()
            val json = gson.toJson(quizList)
            editor.putString(QUIZ_LIST_KEY, json)
            editor.apply()
        }

        /**
         * 從 SharedPreferences 中讀取測驗題目列表
         * @param context 上下文
         * @return 測驗題目列表，如果沒有則返回空列表
         */
        fun getQuizListFromPrefs(context: Context): List<QuizType> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(QUIZ_LIST_KEY, null) ?: return emptyList()
            val gson = Gson()
            val type = object : TypeToken<List<QuizType>>() {}.type
            return gson.fromJson(json, type)
        }

        /**
         * clear SharedPreferences list of quizzes
         * @param context 上下文
         */
        fun clearQuizListFromPrefs(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.remove(QUIZ_LIST_KEY)
            editor.apply()
        }


    } // end of companion object

    private enum class ParsingState {
        NONE, QUESTION, OPTIONS, CORRECT_ANSWER, EXPLANATION, CORRECT, WRONG
    }

}