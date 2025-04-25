package com.example.humanreactor


import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.camera.core.ExperimentalGetImage
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    @OptIn(ExperimentalGetImage::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        enableEdgeToEdge()

        // get the useage of the buttin
//        val goToMotionButton = findViewById<Button>(R.id.custom_btn)a
//
//        val goAIQuestionActivityButton = findViewById<Button>(R.id.ai_question_btn)

        val setButton = findViewById<ImageView>(R.id.setBTN)

        val goToMotionButton = findViewById<ConstraintLayout>(R.id.poseBTN)

        val goAIQuestionActivityButton = findViewById<ConstraintLayout>(R.id.aiQuestionBTN)
//
        val statButton = findViewById<ConstraintLayout>(R.id.statBTN)

        setButton.foreground = ContextCompat.getDrawable(this, R.drawable.btn_background)
        setButton.setOnClickListener {  }

        goToMotionButton.foreground = ContextCompat.getDrawable(this, R.drawable.menu_big_selector)
        // 设置点击事件监听器
        goToMotionButton.setOnClickListener {
            val intent = Intent(this, PoseDetectionActivity::class.java)
            startActivity(intent)
        }
        goAIQuestionActivityButton.foreground = ContextCompat.getDrawable(this, R.drawable.menu_big_selector)
        goAIQuestionActivityButton.setOnClickListener {
            val intent = Intent(this, QuickThinkerActivity::class.java)
            startActivity(intent)
        }
        statButton.foreground = ContextCompat.getDrawable(this, R.drawable.menu_big_selector)
        statButton.setOnClickListener{
            val intent = Intent(this, exampleActivity::class.java)
            startActivity(intent)
        }
//
//        val imageLoader = ImageLoader.Builder(this)
//            .components {
//                add(GifDecoder.Factory())
//            }
//            .build()

//        val background = findViewById<ImageView>(R.id.gif_background)
//        background.load(R.drawable.background, imageLoader)

    }


    override fun onDestroy() {
        super.onDestroy()

    }

}