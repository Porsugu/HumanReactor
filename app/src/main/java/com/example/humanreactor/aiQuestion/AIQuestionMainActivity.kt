package com.example.humanreactor.aiQuestion

import com.example.humanreactor.R

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class AIQuestionMainActivity : AppCompatActivity() {

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



        // add the option with choose right or choose wrong

        //change the words in timer?
    }


    //calling the model for generating the question and multiple choice
    public fun modelCall(){
        val generativeModel = GenerativeModel(
            modelName = "gemini-1.5-pro-latest",
            apiKey = "AIzaSyBB-qR26rDkBT96zzDNR0PZEMlOfHKB4Rc"


        )



        // resolving the concerancy issue using main scope
        MainScope().launch{

            val prompt = "我需要一個關於求職的問題，請按照以下要求：\n" +
                    "    1. 問題本身要刁鑽但簡潔，20字以內,，也不能太複雜令我可以在五秒内作答，\n" +
                    "    3. 只有一個選項是100%正確的，其他選項要看起來合理但不完全正確\n"
            val response = generativeModel.generateContent(prompt)
            savedQuestion = response.toString()
            questionTextView.text = response.text
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
