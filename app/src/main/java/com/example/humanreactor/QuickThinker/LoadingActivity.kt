package com.example.humanreactor.QuickThinker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.humanreactor.R
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class LoadingActivity: AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private val TAG = "LoadingActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.quick_thinker_loading_activity)

        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)

        startParsingTask()
    }

    private fun startParsingTask() {
        lifecycleScope.launch {
            try {

                // delete the old data from previous things first
                updateStatus("正在清空舊數據...")
                Log.d(TAG, "startParsingTask: 開始清空舊數據")
                withContext(Dispatchers.IO) {
                    QuizParser.clearQuizListFromPrefs(this@LoadingActivity)
                }
                Log.d(TAG, "startParsingTask: 舊數據已清空")


                // 更新UI顯示狀態hi
                updateStatus("正在載入測驗題目...")

                // 在後台線程中執行文本讀取和解析
                val quizList = withContext(Dispatchers.IO) {
                    // 從raw file取文本文件
                    val content = readTextFromRawResource(R.raw.backup_data)
                    // 解析文本到QuizType列表
                    QuizParser.parseQuizFromText(content)
                }

                // 更新UI顯示進度
                updateStatus("解析完成，正在保存...")

                // 保存解析的數據到SharedPreferences
                withContext(Dispatchers.IO) {
                    QuizParser.saveQuizListToPrefs(this@LoadingActivity, quizList)
                }

                // 更新UI顯示完成狀態
                updateStatus("完成！")

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
        }
    }

    private fun updateStatus(status: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            statusText.text = status
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