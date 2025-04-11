package com.example.humanreactor

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.humanreactor.vercel_port.GeminiClient
import kotlinx.coroutines.launch


class exampleActivity : AppCompatActivity() {
//    private val geminiClient = GeminiClient(""https://vercel-genimi-api-port.vercel.app/"")
    private val geminiClient by lazy {
        GeminiClient("https://vercel-genimi-api-port.vercel.app//")
    }
    private lateinit var inputEditText: EditText
    private lateinit var generateButton: Button
    private lateinit var resultTextView: TextView
    private lateinit var loadingProgressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example)

        // 初始化视图
        inputEditText = findViewById(R.id.inputEditText)
        generateButton = findViewById(R.id.generateButton)
        resultTextView = findViewById(R.id.resultTextView)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)

        // 初始化视图
        inputEditText = findViewById(R.id.inputEditText)
        generateButton = findViewById(R.id.generateButton)
        resultTextView = findViewById(R.id.resultTextView)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)

        // 设置按钮点击监听器
        generateButton.setOnClickListener {
            val prompt = inputEditText.text.toString().trim()
            if (prompt.isNotEmpty()) {
                processPrompt(prompt)
            } else {
                Toast.makeText(this, "请输入提示词", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 处理用户输入的提示词并显示结果
     */
    private fun processPrompt(prompt: String) {
        // 更新UI状态 - 显示加载状态
        updateLoadingState(true)

        // 在协程中执行API调用
        lifecycleScope.launch {
            try {
                // 获取响应文本
                val response = getGeminiResponseText(prompt)

                // 更新UI显示响应
                resultTextView.text = response
            } finally {
                // 无论成功或失败都隐藏加载状态
                updateLoadingState(false)
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
            Log.e(TAG, "获取Gemini响应出错", e)
            // 捕获异常并将其作为错误消息返回
            "发生错误: ${e.message}"
        }
    }

    /**
     * 更新UI的加载状态
     */
    private fun updateLoadingState(isLoading: Boolean) {
        loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        generateButton.isEnabled = !isLoading
        inputEditText.isEnabled = !isLoading
    }

    companion object {
        private const val TAG = "exampleActivity"
    }
}