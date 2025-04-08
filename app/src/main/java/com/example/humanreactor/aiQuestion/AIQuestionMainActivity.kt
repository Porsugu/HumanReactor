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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PackageManagerCompat
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.Locale

class AIQuestionMainActivity : AppCompatActivity() {

    private lateinit var questionTextView: TextView
    private lateinit var savedQuestion: String
    private lateinit var timerTextView: TextView
    private lateinit var speechOutputTextView: TextView     // for showing the voice to txt
    private lateinit var startSpeechButton: Button      // start speech transform text
    private lateinit var stopSpeechButton: Button       // stop speech transform text

    private var countDownTimer: CountDownTimer? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    private var userSpeechText: StringBuilder = StringBuilder()
    private val RECORD_AUDIO_PERMISSION_CODE = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ai_question_layout)

        // setting variables on text, mc and timer
        questionTextView = findViewById(R.id.fast_reaction_question_display)
        timerTextView = findViewById(R.id.fast_reaction_timer_display)
        speechOutputTextView = findViewById(R.id.ai_question_speech_text)
        startSpeechButton = findViewById(R.id.voice_button)
        stopSpeechButton = findViewById(R.id.voice_button_stop)


        // Display a loading indicator in the question text view
        if(DataManager.selectionData.language == "中文"){
            questionTextView.text = "加載題目中..."
        }
        else {
            questionTextView.text = "Loading question..."
        }


        // setting the start speech button
        startSpeechButton.setOnClickListener { startSpeechRecognition() }

        stopSpeechButton.setOnClickListener {
            stopSpeechRecognition()
            // 直接處理收集到的文字
            processSpeechText()
        }


        // check and ask for microphone permission
        checkAudioPermission()

        // initializing speech recognition
        initializeSpeechRecognizer()

        modelCall()
    }

    // check and ask for microphone permission
    private fun checkAudioPermission(){
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_CODE
            )
        }
    }

    // deal with the permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == RECORD_AUDIO_PERMISSION_CODE){

            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Microphone is already permitted", Toast.LENGTH_SHORT).show()
                initializeSpeechRecognizer()

            }
            else {
                Toast.makeText(this, "Microphone is not being permitted, voice function cannot use haha.", Toast.LENGTH_SHORT).show()
                startSpeechButton.isEnabled = false

            }

        } //end of checking if request code == record audio permission code

    }// end of override

    // initializing speech recognizer
    private fun initializeSpeechRecognizer(){

        // check is the device can use speech recognition
        if(!SpeechRecognizer.isRecognitionAvailable(this)){
            Toast.makeText(this, "Your device does not support voice recognition, im sorry.", Toast.LENGTH_SHORT).show()
            return

        }

        // create a speech recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        // set the language recognize intent
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)

            // following user's choice in language to set up the identified language
            when (DataManager.selectionData.language) {
                "中文" -> {
                    // setting to chinese - taiwan
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-TW")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "zh-TW")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)

                    // the special setting for chinese
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 800L)  // 略微縮短最小語音長度
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 800L)  // 縮短靜音識別
                }
                else -> {
                    // setting to english
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")

                    // the english setting for its special
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)  // 英文可能需要更長的停頓
                }
            }

            // the setting that both can use
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)  // 獲取部分結果
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)  // 增加候選結果數量
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 500L)

        }

        // set to recognize listener
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) { Log.d("SpeechRecognition", "Ready for speech") }

            override fun onBeginningOfSpeech() { Log.d("SpeechRecognition", "Beginning of speech") }

            override fun onRmsChanged(rmsdB: Float) {   // 實現音量變化的視覺反饋
                val scaledValue = (rmsdB * 5).toInt().coerceIn(0, 100)

                // 如果您有音量指示器，可以在這裡更新
                runOnUiThread {
                    // 例如：透過改變按鈕顏色或大小來顯示聲音強度
                    val scale = 1.0f + (rmsdB / 30.0f).coerceIn(0.0f, 0.3f)
                    stopSpeechButton.scaleX = scale
                    stopSpeechButton.scaleY = scale
                }}

            override fun onBufferReceived(buffer: ByteArray?) { Log.d("SpeechRecognition", "Buffer received") }

            override fun onEndOfSpeech() {
                Log.d("SpeechRecognition", "End of speech")
                // 重新開始語音識別，實現連續識別
                speechRecognizer?.startListening(recognizerIntent)
            }

            override fun onError(error: Int) {
                // 處理不同類型的錯誤
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> if (DataManager.selectionData.language == "中文") "音頻錯誤" else "Audio error"
                    SpeechRecognizer.ERROR_CLIENT -> if (DataManager.selectionData.language == "中文") "客戶端錯誤" else "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> if (DataManager.selectionData.language == "中文") "權限不足" else "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> if (DataManager.selectionData.language == "中文") "網絡錯誤" else "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> if (DataManager.selectionData.language == "中文") "網絡超時" else "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> if (DataManager.selectionData.language == "中文") "未匹配到結果" else "No match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> if (DataManager.selectionData.language == "中文") "識別器忙" else "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> if (DataManager.selectionData.language == "中文") "服務器錯誤" else "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> if (DataManager.selectionData.language == "中文") "語音超時" else "Speech timeout"
                    else -> if (DataManager.selectionData.language == "中文") "未知錯誤 $error" else "Unknown error $error"
                }
                Log.e("SpeechRecognition", "Error: $errorMessage")

                // 對於常見錯誤實現自動恢復
                when (error) {
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                        // 識別器忙，取消當前識別並稍後重試
                        speechRecognizer?.cancel()
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (stopSpeechButton.visibility == View.VISIBLE) {
                                speechRecognizer?.startListening(recognizerIntent)
                            }
                        }, 800)
                    }

                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                        // 網絡相關錯誤，顯示提示
                        runOnUiThread {
                            Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                        // 自動重試
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (stopSpeechButton.visibility == View.VISIBLE) {
                                speechRecognizer?.startListening(recognizerIntent)
                            }
                        }, 1500)
                    }

                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        // 無匹配或超時，靜默重試
                        if (stopSpeechButton.visibility == View.VISIBLE) {
                            speechRecognizer?.startListening(recognizerIntent)
                        }
                    }

                    else -> {
                        // 其他錯誤，顯示提示並重試
                        runOnUiThread {
                            Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
                        }

                        if (stopSpeechButton.visibility == View.VISIBLE) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                speechRecognizer?.startListening(recognizerIntent)
                            }, 1000)
                        }
                    }
                }

            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    userSpeechText.append("$text. ")
                    speechOutputTextView.text = userSpeechText.toString()

                    // 將語音文字發送到 API 進行分析（可選）
                    // sendTextToApi(text)

                    // 重新啟動語音識別以實現連續聽取
                    speechRecognizer?.startListening(recognizerIntent)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    // 顯示部分識別結果
                    speechOutputTextView.text = userSpeechText.toString() + text
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.d("SpeechRecognition", "Event: $eventType")
            }

        })


    }

    // start speech recognition
    private fun startSpeechRecognition() {
        userSpeechText.clear()
        speechOutputTextView.text = ""
        speechRecognizer?.startListening(recognizerIntent)
        startSpeechButton.isEnabled = false
        stopSpeechButton.isEnabled = true
    }

    // stop using speech recognition
    private fun stopSpeechRecognition() {
        // 先取消當前的識別任務
        speechRecognizer?.cancel()  // 使用 cancel() 而不是 stopListening()

        // 更新 UI
        startSpeechButton.isEnabled = true
        stopSpeechButton.isEnabled = false

        // 顯示處理中提示
        if (DataManager.selectionData.language == "中文") {
            Toast.makeText(this, "正在處理您的回答...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Processing your answer...", Toast.LENGTH_SHORT).show()
        }
    }

    // deal with the collected words
    private fun processSpeechText() {
        val text = userSpeechText.toString().trim()
        if (text.isNotEmpty()) {
            // 顯示處理中狀態
            speechOutputTextView.text = text + "\n\n" +
                    if (DataManager.selectionData.language == "中文") "正在評分中..." else "Evaluating..."

            // 發送文字到 API 進行分析
            sendTextToApi(text)
        } else {
            // 如果沒有識別到文字
            if (DataManager.selectionData.language == "中文") {
                Toast.makeText(this, "未識別到語音，請重試", Toast.LENGTH_SHORT).show()
                speechOutputTextView.text = "未識別到語音，請重試"
            } else {
                Toast.makeText(this, "No speech detected, please try again", Toast.LENGTH_SHORT).show()
                speechOutputTextView.text = "No speech detected, please try again"
            }
            // 重新啟用開始按鈕
            startSpeechButton.isEnabled = true
        }
    }

    private fun sendTextToApi(text: String) {
        // using api to analyse
        MainScope().launch {
            try {
                val generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-pro-latest",
                    apiKey = "AIzaSyBB-qR26rDkBT96zzDNR0PZEMlOfHKB4Rc"
                )


                // we can change the prompt
                var prompt = " "


                //changing the prompt with the language
                if(DataManager.selectionData.language == "中文"){
                    prompt = "用戶對這個問題：'$savedQuestion' 的回答是：'$text'。請分析這個回答，並給出評分及評語。"
                }
                else {
                    prompt = "User with this question:'$savedQuestion' s answer is: '$text'. Please analyse this response, and give a mark on it and give explanantions."
                }

                // showing the loading status
                runOnUiThread{

                    if(DataManager.selectionData.language == "中文"){
                        questionTextView.text = "加載題目中..."
                    }
                    else {
                        questionTextView.text = "Loading question..."
                    }

                }

                val response = generativeModel.generateContent(prompt)
                // 記錄 API 回應
                Log.d("API_RESPONSE", "評分結果: ${response.text ?: "無回應"}")
                savedQuestion = response.toString()

                // Update UI with the generated question
                runOnUiThread {
                    questionTextView.text = response.text
                    val result = response.text ?:
                    if (DataManager.selectionData.language == "中文") "無法獲取評分" else "Could not get evaluation"

                    speechOutputTextView.text = text + "\n\n" +
                            if (DataManager.selectionData.language == "中文") "評分結果：\n" else "Evaluation result:\n" +
                                    result

                    // Now that the question is displayed, start the timer
                    startTimer()

                    // Additional UI updates if needed
                    // For example, enable answer buttons, etc.
                }

                // show the response of API
                Toast.makeText(this@AIQuestionMainActivity, "API 回應：${response.text}", Toast.LENGTH_LONG).show()
                response.text?.let { Log.d("API_RESPONSE", it) }

                // here can deal with the api response
            } catch (e: Exception) {
                Log.e("API_ERROR", "API 調用失敗", e)
                // 顯示錯誤信息
                runOnUiThread {
                    if (DataManager.selectionData.language == "中文") {
                        Toast.makeText(this@AIQuestionMainActivity, "評分失敗：${e.message}", Toast.LENGTH_SHORT).show()
                        speechOutputTextView.text = text + "\n\n評分失敗，請稍後再試"
                    } else {
                        Toast.makeText(this@AIQuestionMainActivity, "Evaluation failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        speechOutputTextView.text = text + "\n\nEvaluation failed, please try again later"
                    }

                    // 啟用開始按鈕
                    startSpeechButton.isEnabled = true
                }
            }
        }
    }


    //calling the model for generating the question and multiple choice
    public fun modelCall() {
        val generativeModel = GenerativeModel(
            modelName = "gemini-1.5-pro-latest",
            apiKey = "AIzaSyBB-qR26rDkBT96zzDNR0PZEMlOfHKB4Rc"
        )

        // resolving the concerancy issue using main scope
        MainScope().launch {
            try {
                // 顯示加載狀態
                runOnUiThread {
                    if (DataManager.selectionData.language == "中文") {
                        questionTextView.text = "正在生成問題..."
                    } else {
                        questionTextView.text = "Generating question..."
                    }
                }

                // 根據語言設置提示詞
                val prompt = if (DataManager.selectionData.language == "中文") {
                    "我需要一個關於" + DataManager.selectionData.question_type + "的問題，請按照以下要求：\n" +
                            "    1. 問題本身要刁鑽但簡潔，15字以內，也不能太複雜令我可以在五秒内作答"
                } else {
                    "I need a question about " + DataManager.selectionData.question_type + ", please follow these requirements:\n" +
                            "    1. The question itself should be tricky but concise, within 15 characters, and not too complex so I can answer within five seconds."
                }

                // 呼叫 API 生成問題
                val response = generativeModel.generateContent(prompt)
                savedQuestion = response.toString()

                // 更新 UI 顯示問題
                runOnUiThread {
                    questionTextView.text = response.text

                    // 問題生成後開始計時
                    startTimer()

                    // 啟用開始語音按鈕
                    startSpeechButton.isEnabled = true
                }

            } catch (e: Exception) {
                Log.e("API_ERROR", "問題生成失敗", e)

                // 顯示錯誤信息
                runOnUiThread {
                    if (DataManager.selectionData.language == "中文") {
                        questionTextView.text = "問題生成失敗，請重試"
                    } else {
                        questionTextView.text = "Failed to generate question, please try again"
                    }
                }
            }
        }

    }

    // using 5s timer sub function
    private fun startTimer() {
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
}



