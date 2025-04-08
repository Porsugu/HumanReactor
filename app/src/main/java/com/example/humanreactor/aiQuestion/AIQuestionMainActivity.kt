package com.example.humanreactor.aiQuestion

import android.content.Intent
import android.content.pm.PackageManager
import com.example.humanreactor.R

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PackageManagerCompat
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.Locale

class AIQuestionMainActivity : AppCompatActivity() {

    private lateinit var questionTextView: TextView
    private lateinit var savedQuestion:String
    private lateinit var timerTextView : TextView
    private lateinit var mc1TextView: TextView
    private lateinit var mc2TextView: TextView
    private lateinit var mc3TextView: TextView
    private lateinit var mc4TextView: TextView
    private lateinit var mc1CardView: ConstraintLayout
    private lateinit var mc2CardView: ConstraintLayout
    private lateinit var mc3CardView: ConstraintLayout
    private lateinit var mc4CardView: ConstraintLayout
    private var correctAnswer: String = "" // saving the correct answer
    private var countDownTimer: CountDownTimer? = null
    private var correctAnswerExplanation: String = "" // 儲存正確答案的解釋


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ai_question_layout)

        // setting variables on text, mc and timer
        questionTextView = findViewById(R.id.fast_reaction_question_display)
        mc1TextView = findViewById(R.id.text_option_a)
        mc2TextView = findViewById(R.id.text_option_b)
        mc3TextView = findViewById(R.id.text_option_c)
        mc4TextView = findViewById(R.id.text_option_d )
        mc1CardView = findViewById(R.id.option_a_container)
        mc2CardView = findViewById(R.id.option_b_container)
        mc3CardView = findViewById(R.id.option_c_container)
        mc4CardView = findViewById(R.id.option_d_container)
        timerTextView = findViewById(R.id.fast_reaction_timer_display)

        modelCall()
    }

    // check if the answers are correct
    private fun checkAnswer(selectedOption:String){
        // cancel the count down timer
        countDownTimer?.cancel()
        timerTextView.visibility = View.GONE

        // do with the selected option and make the results
        val isCorrect = selectedOption == correctAnswer

        // 顯示結果到日誌
        Log.d("ANSWER_CHECK", "Selected: $selectedOption, Correct: $correctAnswer, IsCorrect: $isCorrect")
        Log.d("ANSWER_EXPLANATION", "Explanation: $correctAnswerExplanation")

        highlightAnswers (selectedOption)

        // add the option with choose right or choose wrong

    }

    // highlighted shows the correct answer and the user option choice
    private fun highlightAnswers(selectedOption: String){

        // show the green color when it is correct
        when(correctAnswer){
            "A" -> mc1CardView.setBackgroundResource(R.drawable.correct_option_background)
            "B" -> mc2CardView.setBackgroundResource(R.drawable.correct_option_background)
            "C" -> mc3CardView.setBackgroundResource(R.drawable.correct_option_background)
            "D" -> mc4CardView.setBackgroundResource(R.drawable.correct_option_background)
        }

        // show the red color when it is wrong
        if(selectedOption != correctAnswer){
            when (selectedOption){
                "A" -> mc1CardView.setBackgroundResource(R.drawable.wrong_option_box)
                "B" -> mc2CardView.setBackgroundResource(R.drawable.wrong_option_box)
                "C" -> mc3CardView.setBackgroundResource(R.drawable.wrong_option_box)
                "D" -> mc4CardView.setBackgroundResource(R.drawable.wrong_option_box)
            }
        }
    }

    //calling the model for generating the question and multiple choice
    public fun modelCall(){
        val generativeModel = GenerativeModel(
            modelName = "gemini-1.5-pro-latest",
            apiKey = "AIzaSyBB-qR26rDkBT96zzDNR0PZEMlOfHKB4Rc"
        )

        var prompt = " "

        if(DataManager.selectionData.language == "中文") {
            prompt =
                "我需要一個關於" + DataManager.selectionData.question_type + "的問題，請按照以下要求：\n"+
                        "    1. 問題本身要刁鑽但簡潔，20字以內,，也不能太複雜令我可以在五秒内作答，\n" +
                        "    2. 提供4個選項（A、B、C、D）， 選項需要15字以内\n" +
                        "    3. 只有一個選項是100%正確的，其他選項要看起來合理但不完全正確\n" +
                        "    4. 正確選項應該代表最有價值的答案\n" +
                        "    5. 請明確標明哪個是正確答案（例如：「正確答案：C」）\n" +
                        "    6. 請將問題與選項分開呈現，格式如下：\n" +
                        "問題：[問題文字]\n" +
                        "A. [選項A]\n" +
                        "B. [選項B]\n" +
                        "C. [選項C]\n" +
                        "D. [選項D]\n" +
                        "正確答案：[A/B/C/D], 【答案解釋】"
        }
        else{
            prompt =
                "I need a question about " + DataManager.selectionData.question_type + ", please follow these requirements: \n"+
                        "1. The question should be tricky but concise, within 20 characters, and not too complex so I can answer it within five seconds, \n" +
                        "2. Provide 4 options (A, B, C, D)\n" +
                        "3. Only one option should be 100% correct, other options should look reasonable but not completely correct\n"+
                        "4. The correct option should represent the most valuable answer\n" +
                        "5. Please clearly indicate which is the correct answer (e.g., Correct answer: C)\n"+
                        "6. For each incorrect option, please explain why\n" +
                        "7. Please present the question and options separately, in the following format:\n"+
                        "Question: [question text]\n" +
                        "A. [option A]\n" +
                        "B. [option B]\n"+
                        "C. [option C]\n" +
                        "D. [option D]\n" +
                        "Correct answer: [A/B/C/D], Explanation for incorrect answers "

        }

        // resolving the concerancy issue using main scope
        MainScope().launch{

            try{
                val response = generativeModel.generateContent(prompt)
                savedQuestion = response.text?:"cannot generate the question"

                // analyse the repsonse from ai, and separate the questions and options
                parseResponseAndDisplayOptions(savedQuestion)
            }
            catch (e: Exception) {
                Log.e("MODEL_ERROR", "there are error while generating the qeustion", e)
                questionTextView.text = "error while generating: ${e.message}"
            }
//            savedQuestion = response.toString()
//            questionTextView.text = response.text
        }

    }

    // analyse ai's response and show quesitons and options
    private fun parseResponseAndDisplayOptions(response: String){
        try{
            // separating questions and answers
            val lines = response.split("\n")
            var question = ""
            val options = mutableMapOf<String,String>()
            var capturingExplanation = false
            var explanationText = StringBuilder()


            // searching for questions and options

            for(line in lines){
                when{
                    line.startsWith("問題：")->{question = line.substringAfter("問題：").trim()}
                    line.startsWith("A.")|| line.startsWith("A：") || line.startsWith("A. ")->{
                        options["A"] = line.substringAfter(".").trim()
                    }
                    line.startsWith("B.") || line.startsWith("B：") || line.startsWith("B. ") -> {
                        options["B"] = line.substringAfter(".").trim()
                    }
                    line.startsWith("C.") || line.startsWith("C：") || line.startsWith("C. ") -> {
                        options["C"] = line.substringAfter(".").trim()
                    }
                    line.startsWith("D.") || line.startsWith("D：") || line.startsWith("D. ") -> {
                        options["D"] = line.substringAfter(".").trim()
                    }
                    line.contains("正確答案" )|| line.contains( "Correct answer") -> {
                        correctAnswer = line.substringAfter("：").trim().take(1) // 只取第一個字符（A/B/C/D）


                        // check if that row contains any explanation
                        if(line.contains(",")|| line.contains("，")){
                            val explanation = if (line.contains(",")) {
                                line.substringAfter(",").trim()

                            } else {
                                line.substringAfter("，").trim()

                            }
                            explanationText.append(explanation).append("\n")
                        }

                        capturingExplanation = true     // start capturing the explanation

                    }
                }
            }

            correctAnswerExplanation = explanationText.toString().trim()

            runOnUiThread {
                questionTextView.text = question
                mc1TextView.text = "A. ${options["A"] ?: ""}"
                mc2TextView.text = "B. ${options["B"] ?: ""}"
                mc3TextView.text = "C. ${options["C"] ?: ""}"
                mc4TextView.text = "D. ${options["D"] ?: ""}"

                mc1TextView.setOnClickListener{checkAnswer("A")}
                mc2TextView.setOnClickListener{checkAnswer("B")}
                mc3TextView.setOnClickListener{checkAnswer("C")}
                mc4TextView.setOnClickListener{checkAnswer("D")}

                Log.d("CORRECT_ANSWER", "The correct answer is : $correctAnswer")
                Log.d("ANSWER_EXPLANATION", "Explanation: $correctAnswerExplanation")

                startTimer()
            }

        } catch (e: Exception) {
            Log.e("PARSE_ERROR", "解析AI回應時出錯", e)
            runOnUiThread {
                questionTextView.text = "解析問題時出錯: ${e.message}"
            }
        }

    }

    // using 5s timer sub function
    private fun startTimer(){
        // make the timer visible
        timerTextView.visibility = View.VISIBLE

        //create a 5 second countdown timer with 1 second intervals
        countDownTimer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Update the timer text with remaining seconds
                val secondsRemaining = millisUntilFinished / 1000 + 1
                timerTextView.text = "$secondsRemaining"
            }

            override fun onFinish() {
                // Hide the timer when finished
                timerTextView.visibility = View.GONE
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel the timer to prevent memory leaks
        countDownTimer?.cancel()
    }
}



