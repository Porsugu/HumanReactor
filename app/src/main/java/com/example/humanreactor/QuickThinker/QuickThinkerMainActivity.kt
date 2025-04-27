package com.example.humanreactor.QuickThinker

import android.content.ContentValues.TAG
import android.nfc.Tag
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.example.humanreactor.R
import com.example.humanreactor.aiQuestion.QuestionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates

class QuickThinkerMainActivity:AppCompatActivity() {

    private lateinit var questionList : List<QuizType>
    private var currentQuestionIndex = 0
    private var startTime: Long = 0
    private var questionNumbers = 0
    private var answerTimes = mutableListOf<Long>()
    private var userAnswers = mutableListOf<Int>()
    private lateinit var sharedPrefManager: SharedPrefManager

    private lateinit var listofSuccess_Interview: List<String>


    private lateinit var questionTextView: TextView
    private lateinit var timerTextView: TextView
    private var countDownTimer: CountDownTimer? = null
    private val timerDuration = 10000L // 10 seconds
    private val timerInterval = 1000L // every second update once

    private lateinit var option1Layout: ConstraintLayout
    private lateinit var option2Layout: ConstraintLayout
    private lateinit var option3Layout: ConstraintLayout
    private lateinit var option4Layout: ConstraintLayout

    private lateinit var option1TextView: TextView
    private lateinit var option2TextView: TextView
    private lateinit var option3TextView: TextView
    private lateinit var option4TextView: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.quick_thinker_main_activity)

        questionTextView = findViewById(R.id.textView9)

        option1Layout = findViewById(R.id.question_main_opt1)
        option2Layout = findViewById(R.id.question_main_opt2)
        option3Layout = findViewById(R.id.question_main_opt3)
        option4Layout = findViewById(R.id.question_main_opt4)

        option1TextView = findViewById(R.id.option1Text)
        option2TextView = findViewById(R.id.option2Text)
        option3TextView = findViewById(R.id.option3Text)
        option4TextView = findViewById(R.id.option4Text)

        timerTextView = findViewById(R.id.fast_reaction_timer_display)

        sharedPrefManager = SharedPrefManager(this)

        questionNumbers = sharedPrefManager.getNumber()

        //using log data quiz to load the quiz data, to check is everything alright and load the other functions
        logQuizData()


    }

    /** timer functions : startTimer() and stopTimer()
     *  - to record and show the time user made
     */

    private fun startTimer(){
        startTime = System.currentTimeMillis()
        countDownTimer?.cancel()    // cancel the previous timer

        //create a new timer
        countDownTimer = object : CountDownTimer(timerDuration, timerInterval){

            // update the timer on ui
            override fun onTick(millisUntilFinished: Long) {
                val secondRemaining = millisUntilFinished / 1000
                timerTextView.text = secondRemaining.toString()

            }

            // when it is finished
            override fun onFinish() {
                timerTextView.text = "0"
                recordAnswer(-1)            // record as over time,will make it later
                Log.d(TAG, "Record answer is -1 here")
                showCorrectAnswer()   // will make it later
            }
        }.start()

    }

    private fun stopTimer() {
        countDownTimer?.cancel()
    }

    private fun showStartQuizDialog() {
        AlertDialog.Builder(this)
            .setTitle("Ready to start?")
            .setMessage("Press Start when you are ready to begin the quiz.")
            .setPositiveButton("Start") { dialog, _ ->
                dialog.dismiss()
                startTimer()        // starting the first question timer
            }
            .setCancelable(false)
            .create()
            .show()
    }

    /** load functions : loadQuestionList()
     *  - to load those current question and options about the quiz functions
     *
     */

    private fun loadQuestionList(){
        lifecycleScope.launch {

            try {
                questionList = withContext(Dispatchers.IO) {
                    QuizParser.getQuizListFromPrefs(this@QuickThinkerMainActivity)
                }
            }  catch (e: Exception) {
                Log.e(TAG, "loadData: 載入數據時發生錯誤", e)
                withContext(Dispatchers.Main) {
                    showErrorDialog(e.message ?: "未知錯誤")
                }
            }
        }
    }

    private fun showErrorDialog(errorMessage: String) {
        AlertDialog.Builder(this)
            .setTitle("錯誤")
            .setMessage("載入測驗題目時出錯：$errorMessage")
            .setPositiveButton("重試") { _, _ -> loadQuestionList() }
            .setNegativeButton("退出") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun loadCurrentQuestion(){

        if(currentQuestionIndex < questionNumbers){

            val currentQuestion = questionList[currentQuestionIndex]

            // setting the question text
            questionTextView.text = "Q${currentQuestionIndex + 1} : ${currentQuestion.question}"

            // setting the options with words
            option1TextView.text = currentQuestion.option[0]
            option2TextView.text = currentQuestion.option[1]
            option3TextView.text = currentQuestion.option[2]
            option4TextView.text = currentQuestion.option[3]


            // setting the options with on click listener
            option1Layout.setOnClickListener { handleOptionClick(1) }
            option2Layout.setOnClickListener { handleOptionClick(2) }
            option3Layout.setOnClickListener { handleOptionClick(3) }
            option4Layout.setOnClickListener { handleOptionClick(4) }

        }
    }

    private fun handleOptionClick(optionIndex : Int){
        stopTimer()
        recordAnswer(optionIndex)
        showCorrectAnswer()
    }

    private fun recordAnswer(optionIndex: Int){
        // cal the answer time with millisecond
        val endTime = System.currentTimeMillis()
        val timeElapsed = endTime - startTime
        answerTimes.add(timeElapsed)    // record the answering time
        userAnswers.add(optionIndex)  // 這一行在你的代碼中缺失
    }

    private fun showCorrectAnswer() {
        val currentQuestion = questionList[currentQuestionIndex]
        val userChoice = userAnswers.lastOrNull() ?: -1
        val correctChoice = currentQuestion.correctAnswerIndex.toInt()

        // prepare with the dialog class's nneed
        val timeUsed = answerTimes.lastOrNull() ?: timerDuration
        val timeUsedSeconds = timeUsed / 1000.0

        var sentence_of_humour = ""

        if(userChoice == correctChoice){
            sentence_of_humour = currentQuestion.correctSentence
        } else {
            sentence_of_humour = currentQuestion.wrongSentences
        }

        // 創建結果展示對話框（會在步驟6實現）
        showResultDialog(userChoice, correctChoice, timeUsedSeconds, currentQuestion.explanation, sentence_of_humour)
    }

    private fun showResultDialog(userChoice : Int, correctChoice : Int, timeUsedSeconds:Double, explanation:String,sentence_of_humour:String){

        val dialog = AnswerDialogFragment.newInstance(userChoice, correctChoice, timeUsedSeconds,explanation, sentence_of_humour)
        dialog.show(supportFragmentManager, "AnswerDialogFragment")

    }


    /** Log functions : logQuizData()
     *  - to show the information in the log about the quiz list and track is there have any question there
     */

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
                        Log.d(TAG, "sentence 1: ${quiz.correctSentence}")
                        Log.d(TAG, "sentence 2: ${quiz.wrongSentences}")
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

                questionList = quizList

                showStartQuizDialog()

                loadCurrentQuestion()

            } catch (e: Exception) {
                Log.e(TAG, "logQuizData: 讀取或處理數據時發生錯誤", e)
            }
        }
    }
}