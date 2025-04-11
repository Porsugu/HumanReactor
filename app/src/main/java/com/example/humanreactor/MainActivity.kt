package com.example.humanreactor


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.camera.core.ExperimentalGetImage
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.load
import coil.request.ImageRequest

class MainActivity : AppCompatActivity() {

    @OptIn(ExperimentalGetImage::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        enableEdgeToEdge()

        // 获取按钮的引用
//        val goToMotionButton = findViewById<Button>(R.id.custom_btn)
//
//        val goAIQuestionActivityButton = findViewById<Button>(R.id.ai_question_btn)

        val goToMotionButton = findViewById<RelativeLayout>(R.id.poseBTN)

        val goAIQuestionActivityButton = findViewById<RelativeLayout>(R.id.aiQuestionBTN)

        val exampleButton = findViewById<Button>(R.id.exampleBTN)

        goToMotionButton.foreground = ContextCompat.getDrawable(this, R.drawable.menu_big_selector)
        // 设置点击事件监听器
        goToMotionButton.setOnClickListener {
            val intent = Intent(this, PoseDetectionActivity::class.java)
            startActivity(intent)
        }
        goAIQuestionActivityButton.foreground = ContextCompat.getDrawable(this, R.drawable.menu_big_selector)
        goAIQuestionActivityButton.setOnClickListener {
            val intent = Intent(this, AIQuestionActivity::class.java)
            startActivity(intent)
        }

        exampleButton.setOnClickListener{
            val intent = Intent(this, exampleActivity::class.java)
            startActivity(intent)
        }

        val imageLoader = ImageLoader.Builder(this)
            .components {
                add(GifDecoder.Factory())
            }
            .build()

        val background = findViewById<ImageView>(R.id.gif_background)
        background.load(R.drawable.background, imageLoader)

    }


    override fun onDestroy() {
        super.onDestroy()

    }

}