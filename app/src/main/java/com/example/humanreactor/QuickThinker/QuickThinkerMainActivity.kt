package com.example.humanreactor.QuickThinker

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.humanreactor.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates

class QuickThinkerMainActivity:AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ai_question_layout)

        logQuizData()
    }

    private fun logQuizData() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "logQuizData: 開始從SharedPreferences讀取測驗數據")

                // 在IO線程中讀取SharedPreferences
                val quizList = withContext(Dispatchers.IO) {
                    QuizParser.getQuizListFromPrefs(this@QuickThinkerMainActivity)
                }

                // 記錄測驗數據總數
                Log.d(TAG, "================ 測驗數據 ================")
                Log.d(TAG, "共讀取到 ${quizList.size} 個測驗問題")

                if (quizList.isNotEmpty()) {
                    // 詳細記錄每個問題
                    quizList.forEachIndexed { index, quiz ->
                        Log.d(TAG, "----- 問題 ${index + 1} -----")
                        Log.d(TAG, "題目: ${quiz.question}")

                        Log.d(TAG, "選項:")
                        quiz.option.forEachIndexed { optIndex, option ->
                            Log.d(TAG, "  ${optIndex + 1}. $option")
                        }

                        Log.d(TAG, "正確答案索引: ${quiz.correctAnswerIndex}")
                        Log.d(TAG, "解釋: ${quiz.explanation}")
                        Log.d(TAG, "")  // 空行分隔不同問題
                    }

                    // 記錄第一個問題的完整信息（作為示例）
                    val firstQuiz = quizList[0]
                    Log.d(TAG, "==========================================")
                    Log.d(TAG, "首個問題數據樣例:")
                    Log.d(TAG, "題目: ${firstQuiz.question}")
                    Log.d(TAG, "選項數量: ${firstQuiz.option.size}")
                    Log.d(TAG, "正確答案: ${firstQuiz.correctAnswerIndex}")
                    Log.d(TAG, "解釋長度: ${firstQuiz.explanation.length} 字符")
                    Log.d(TAG, "==========================================")
                } else {
                    Log.w(TAG, "沒有找到任何測驗數據！請確認LoadingActivity是否正確保存了數據。")
                }

            } catch (e: Exception) {
                Log.e(TAG, "logQuizData: 讀取或處理數據時發生錯誤", e)
            }
        }
    }
}