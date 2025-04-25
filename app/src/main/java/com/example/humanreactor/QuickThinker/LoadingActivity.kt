package com.example.humanreactor.QuickThinker

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.humanreactor.R
import com.example.humanreactor.exampleActivity
import com.example.humanreactor.vercel_port.GeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader


class LoadingActivity: AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var percentageText: TextView
    private var progressStatus = 0
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val showPercentage = true // Set to true if you want to show percentage text


    private lateinit var language: String
    private var num : Int = 0
    private lateinit var category: String
    private lateinit var sharedPrefManager: SharedPrefManager


    private val TAG = "LoadingActivity"
    private val geminiClient by lazy {
        GeminiClient("https://vercel-genimi-api-port.vercel.app//")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.quick_thinker_loading_activity)
        sharedPrefManager = SharedPrefManager(this)

        //get the requirements for the loading class
        language = sharedPrefManager.getLanguage().toString()
        num = sharedPrefManager.getNumber()
        category = sharedPrefManager.getCategory().toString()

        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)
        percentageText = findViewById(R.id.percentageText)

        if (showPercentage) {
            percentageText.visibility = View.VISIBLE
        }

        startParsingTask()
    }

    private fun startParsingTask() {

        // Reset progress
        progressStatus = 0
        progressBar.progress = 0

        lifecycleScope.launch {
            try {

                // delete the old data from previous things first
                clearExistingData()

                // check the wifi connecting status
                updateStatus(5, "Checking internet connection...")
                val isWifiConnected = isWifiConnected()
                Log.d(TAG, "loadData: WiFi連接狀態: ${if (isWifiConnected) "已連接" else "未連接"}")

                if (isWifiConnected) {
                    // using online API
                    updateStatus(10,"Getting data from the online API")
//                    updateStatus("正在從在線API獲取數據...")
                    fetchDataFromApi()
                } else {
                    // using local data
                    updateStatus(10,"No wifi connection, using local data")
                    loadDataFromLocalDatabase()
                }
            }
            catch (e: Exception) {
                Log.e(TAG, "loadData: 載入數據時發生錯誤", e)
                withContext(Dispatchers.Main) {
                    showErrorDialog(e.message ?: "未知錯誤")
                }
            }

        } //  life scope function end

    } // start parsing function end

    private suspend fun clearExistingData(){
        updateStatus(1, "Clearing the old data")
        Log.d(TAG, "startParsingTask: 開始清空舊數據")
        withContext(Dispatchers.IO) {
            QuizParser.clearQuizListFromPrefs(this@LoadingActivity)
        }
        Log.d(TAG, "startParsingTask: 舊數據已清空")

    }

    private suspend fun fetchDataFromApi(){
        try {
            Log.d(TAG, "fetchDataFromApi: 開始從API獲取數據")
            updateStatus(20, "正在連接服務器...")

            // preparing the prompt for the code
            var prompt = "make $num questions about [$category]'s quiz question, each of the question has " +
                    "4 options，please give me the correct answer and explanation. The question should be tricky " +
                    "but concise, within 20 characters, and not too complex so I can answer it within five seconds,Here is a format you must have to follow" +
                    "*Question ( it should not be over 10 -15 words\n" +
                    "1. Option 1\n" +
                    "2. Option 2\n" +
                    "3. Option 3\n" +
                    "4. Option 4\n" +
                    "-(Correct Answer)\n" +
                    "+(Explanation)\n"

            Log.e(TAG, "the language here is $language")

            if(language != "english"){
                prompt = prompt + "please give all the questions in $language only, no english please"
            }

            updateStatus(30, "Generating Questions...")

            // use the process prompt logic to get the Gemini reaction
            val response = getGeminiResponseText(prompt)

            updateStatus(60, "Dealing with the response...")

            // parase gemini's text into quiz type list
            val quizList = QuizParser.parseQuizFromText(response)

            // 保存到SharedPreferences
            updateStatus(80, "Finish analysing, now saving...")

            withContext(Dispatchers.IO) {
                QuizParser.saveQuizListToPrefs(this@LoadingActivity, quizList)
            }

            updateStatus(100, "測驗題目生成完成！")
             //等待一段時間後跳轉到主頁面
                withContext(Dispatchers.Main) {
                    // 延遲500毫秒，讓用戶看到"完成"狀態
                    kotlinx.coroutines.delay(500)
                    navigateToMainActivity()
                }
        }
        catch (e: Exception) {
                // 處理錯誤情況
                withContext(Dispatchers.Main) {
                    showErrorDialog(e.message ?: "未知錯誤")
                }
            }
    }

    /**
     * 发送提示到Gemini API并返回响应文本
     *
     * @param prompt 发送到API的提示文本
     * @return 从API获取的响应文本，或错误消息
     */
    private suspend fun getGeminiResponseText(prompt: String): String {
        return try {
            // 调用API获取响应
            geminiClient.generateContent(prompt)
        } catch (e: Exception) {
            Log.e(exampleActivity.TAG, "获取Gemini响应出错", e)
            // 捕获异常并将其作为错误消息返回
            "发生错误: ${e.message}"
        }
    }

    private suspend fun loadDataFromLocalDatabase(){

        try{
            // update UI showing status
            updateStatus(20, "正在載入測驗題目...")



            // 在後台線程中執行文本讀取和解析
                val quizList = withContext(Dispatchers.IO) {
                    // get the text from raw file sets
                    var content = ""
                    if(category == "interview"){
                        content = readTextFromRawResource(R.raw.interview_question)
                    }
                    else if (category == "relationship"){
                        content = readTextFromRawResource(R.raw.relationship_question)
                    }
                    // 解析文本到QuizType列表
                    // 更新UI顯示進度
                    updateStatus(40, "正在解析問題...")
                    QuizParser.parseQuizFromText(content)
                }

                updateStatus(80, "解析完成，正在保存...")

                // 保存解析的數據到SharedPreferences
                withContext(Dispatchers.IO) {
                    QuizParser.saveQuizListToPrefs(this@LoadingActivity, quizList)
                }

            // 更新UI顯示完成狀態
            updateStatus(100, "完成！")


                // 等待一段時間後跳轉到主頁面
                withContext(Dispatchers.Main) {
                    // 延遲500毫秒，讓用戶看到"完成"狀態
                    kotlinx.coroutines.delay(500)
                    navigateToMainActivity()
                }
            } catch (e: Exception) {
                // 處理錯誤情況
                withContext(Dispatchers.Main) {
                    showErrorDialog(e.message ?: "未知錯誤")
                }
            }
    } // end of loadDataFromLocalDatabase function

    private suspend fun updateStatus(progress:Int, status: String) {
        withContext(Dispatchers.Main) {
            progressStatus = progress
            progressBar.progress = progress
            statusText.text = status

            if (showPercentage) {
                percentageText.text = "$progress%"
            }
        }
    }

    private fun readTextFromRawResource(resourceId: Int): String {
        try {
            val inputStream = resources.openRawResource(resourceId)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line).append("\n")
            }

            reader.close()
            return stringBuilder.toString()
        } catch (e: IOException) {
            throw IOException("無法讀取文件", e)
        }
    }

    private fun isWifiConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun showErrorDialog(errorMessage: String) {
        AlertDialog.Builder(this)
            .setTitle("錯誤")
            .setMessage("載入測驗題目時出錯：$errorMessage")
            .setPositiveButton("重試") { _, _ -> startParsingTask() }
            .setNegativeButton("退出") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun navigateToMainActivity() {
        // 替換為您的主頁面Activity
        val intent = Intent(this, QuickThinkerMainActivity::class.java)
        startActivity(intent)
        finish() // 結束LoadingActivity，防止用戶按返回鍵回到此頁面
    }

}