package com.example.humanreactor

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ai_question_layout)

        questionTextView = findViewById(R.id.fast_reaction_question_display)
        modelCall()
//        fetchQuestion()
    }

    public fun modelCall(){
        val generativeModel = GenerativeModel(
            modelName = "gemini-1.5-pro-latest",
            apiKey = "AIzaSyBB-qR26rDkBT96zzDNR0PZEMlOfHKB4Rc"


        )

        val prompt = "我想要你設計出一條關於求職相關的題目，必需足夠刁鑽，也不能太複雜令我可以在五秒内作答，限制30字以内，不能有選擇題"
        //resolving the concerancy issue
        MainScope().launch{
            val response = generativeModel.generateContent(prompt)
            savedQuestion = response.toString()
            questionTextView.text = response.text
        }

    }


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
}