package com.example.humanreactor


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RelativeLayout
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ExperimentalGetImage


class MainActivity : AppCompatActivity() {

    @OptIn(ExperimentalGetImage::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 获取按钮的引用
//        val goToMotionButton = findViewById<Button>(R.id.custom_btn)
//
//        val goAIQuestionActivityButton = findViewById<Button>(R.id.ai_question_btn)

        val goToMotionButton = findViewById<RelativeLayout>(R.id.poseBTN)

        val goAIQuestionActivityButton = findViewById<RelativeLayout>(R.id.aiQuestionBTN)


        // 设置点击事件监听器
        goToMotionButton.setOnClickListener {
            val intent = Intent(this, PoseDetectionActivity::class.java)
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