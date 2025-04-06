package com.example.humanreactor

import android.graphics.Bitmap
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.ai.client.generativeai.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class AIQuestionActivity : AppCompatActivity() {

    private lateinit var questionTextView: TextView
    private lateinit var savedQuestion:String
    private lateinit var timerTextView : TextView
    private lateinit var mc1TextView: TextView
    private lateinit var mc2TextView: TextView
    private lateinit var mc3TextView: TextView
    private lateinit var mc4TextView: TextView
    private var correctAnswer: String = "" // saving the correct answer
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ai_question_layout)

        // setting variables on text, mc and timer
        questionTextView = findViewById(R.id.fast_reaction_question_display)
        mc1TextView = findViewById(R.id.text_option_a)
        mc2TextView = findViewById(R.id.text_option_b)
        mc3TextView = findViewById(R.id.text_option_c)
        mc4TextView = findViewById(R.id.text_option_d )
        timerTextView = findViewById(R.id.fast_reaction_timer_display)

        modelCall()
        startTimer()
    }

    // check if the answers are correct
    private fun checkAnswer(selectedOption:String){
        // cancel the count down timer
        countDownTimer?.cancel()
        timerTextView.visibility = View.GONE

        // do with the selected option and make the results
        val isCorrect = selectedOption == correctAnswer

        highlightAnswers (selectedOption)

        // add the option with choose right or choose wrong

        //change the words in timer?
    }

    // highlighted shows the correct answer and the user option choice
    private fun highlightAnswers(selectedOption: String){

        // setting the background resources
        mc1TextView.setBackgroundResource(R.drawable.mc_box_background)
        mc2TextView.setBackgroundResource(R.drawable.mc_box_background)
        mc3TextView.setBackgroundResource(R.drawable.mc_box_background)
        mc4TextView.setBackgroundResource(R.drawable.mc_box_background)

        // show the green color when it is correct
        when(correctAnswer){
            "A" -> mc1TextView.setBackgroundResource(R.drawable.correct_option_background)
            "B" -> mc2TextView.setBackgroundResource(R.drawable.correct_option_background)
            "C" -> mc3TextView.setBackgroundResource(R.drawable.correct_option_background)
            "D" -> mc4TextView.setBackgroundResource(R.drawable.correct_option_background)
        }

        // show the red color when it is wrong
        if(selectedOption != correctAnswer){
            when (selectedOption){
                "A" -> mc1TextView.setBackgroundResource(R.drawable.wrong_option_box)
                "B" -> mc2TextView.setBackgroundResource(R.drawable.wrong_option_box)
                "C" -> mc3TextView.setBackgroundResource(R.drawable.wrong_option_box)
                "D" -> mc4TextView.setBackgroundResource(R.drawable.wrong_option_box)
            }
        }
    }

    //calling the model for generating the question and multiple choice
    public fun modelCall(){
        val generativeModel = GenerativeModel(
            modelName = "gemini-1.5-pro-latest",
            apiKey = "AIzaSyBB-qR26rDkBT96zzDNR0PZEMlOfHKB4Rc"


        )

        val prompt = "我需要一個關於求職的問題，請按照以下要求：\n" +
                "    1. 問題本身要刁鑽但簡潔，20字以內,，也不能太複雜令我可以在五秒内作答，\n" +
                "    2. 提供4個選項（A、B、C、D）\n" +
                "    3. 只有一個選項是100%正確的，其他選項要看起來合理但不完全正確\n" +
                "    4. 正確選項應該代表最有價值的答案\n" +
                "    5. 請明確標明哪個是正確答案（例如：「正確答案：C」）\n" +
                "    6. 請將問題與選項分開呈現，格式如下：\n" +
                "問題：[問題文字]\n" +
                "A. [選項A]\n" +
                "B. [選項B]\n" +
                "C. [選項C]\n" +
                "D. [選項D]\n" +
                "正確答案：[A/B/C/D]"

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
                    line.contains("正確答案") -> {
                        correctAnswer = line.substringAfter("：").trim().take(1) // 只取第一個字符（A/B/C/D）
                    }
                }
            }

            runOnUiThread {
                questionTextView.text = question
                mc1TextView.text = "A. ${options["A"] ?: ""}"
                mc2TextView.text = "B. ${options["B"] ?: ""}"
                mc3TextView.text = "C. ${options["C"] ?: ""}"
                mc4TextView.text = "D. ${options["D"] ?: ""}"

                Log.d("CORRECT_ANSWER", "The correct answer is : $correctAnswer")
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
        countDownTimer = object : CountDownTimer(5000, 1000) {
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



    /*

    // Fetching a question using Gemini AI API
    private fun fetchQuestion() {
        // Construct the request body according to Gemini API's expected format
        val jsonBody = JSONObject().apply {
            put("contents", JSONObject().apply {
                put("parts", JSONArray().put(
                    JSONObject().apply {
                        put("text", "我想要你設計出一條關於逃生技巧的題目，我需要在五秒内作答")
                    }
                ))
            })
        }

        val requestBody = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            jsonBody.toString()
        )

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=AIzaSyBB-qR26rDkBT96zzDNR0PZEMlOfHKB4Rc")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    questionTextView.text = "API 請求失敗: ${e.message}"
                    Log.e("API_REQUEST_ERROR", "Request failed", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    try {
                        val jsonResponse = JSONObject(responseBody)

                        // Log the full response for debugging
                        Log.d("API_RESPONSE", responseBody)

                        // Extract the question
                        val question = extractQuestionFromResponse(jsonResponse)

                        runOnUiThread {
                            questionTextView.text = question
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            questionTextView.text = "解析錯誤: ${e.message}"
                            Log.e("API_PARSE_ERROR", "Error parsing response", e)
                        }
                    }
                } else {
                    runOnUiThread {
                        questionTextView.text = "回應為空"
                    }
                }
            }
        })
    }

    // Helper function to extract question from different possible response formats
    private fun extractQuestionFromResponse(jsonResponse: JSONObject): String {
        return when {
            jsonResponse.has("candidates") -> {
                val candidates = jsonResponse.getJSONArray("candidates")
                if (candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    firstCandidate.getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                        .trim()
                } else "無法找到問題"
            }
            jsonResponse.has("generatedContent") -> {
                jsonResponse.getString("generatedContent").trim()
            }
            else -> "無法解析問題"
        }
    }

     */
}
