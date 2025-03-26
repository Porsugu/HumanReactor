package com.example.humanreactor


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.humanreactor.CustomMotionActivity


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 获取按钮的引用
        val goToSecondActivityButton = findViewById<Button>(R.id.custom_btn)

        val goAIQuestionActivityButton = findViewById<Button>(R.id.ai_question_btn)

        // 设置点击事件监听器
        goToSecondActivityButton.setOnClickListener {
            val intent = Intent(this, CustomMotionActivity::class.java)
            startActivity(intent)
        }

        goAIQuestionActivityButton.setOnClickListener {
            val intent = Intent(this, AIQuestionActivity::class.java)
            startActivity(intent)
        }

    }


    override fun onDestroy() {
        super.onDestroy()

    }

}