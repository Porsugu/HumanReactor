package com.example.humanreactor

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.humanreactor.aiQuestion.DataManager
import com.example.humanreactor.aiQuestion.OptionAdapter
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

   private lateinit var optionAdapter: OptionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ai_question_choosing)

        // initialize recyclerview
        val optionsRecyclerView = findViewById<RecyclerView>(R.id.ai_question_language_choosing)
        val nextButton = findViewById<Button>(R.id.ai_question_language_start)

        // initally disable next button until selction is made
        nextButton.isEnabled = false

        // set up language options
        val language_options = listOf("English", "中文")

        // set up adapter
        optionAdapter = OptionAdapter(language_options) { _, selectedOptions ->
            // update data class with selections
            DataManager.selectionData.language = selectedOptions

            // enable the start button
            nextButton.isEnabled = true
        }

        // set up the RecyclerView
        optionsRecyclerView.layoutManager = LinearLayoutManager(this)
        optionsRecyclerView.adapter = optionAdapter

        // set up the next button to navigate to second activity
//        nextButton.setOnClickListener {
//            val intent = Intent(this, SecondActivity::class.java)
//            startActivity(intent)
//        }

    }


}

