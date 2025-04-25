package com.example.humanreactor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.humanreactor.QuickThinker.OptionAdapter
import com.example.humanreactor.aiQuestion.SelectTypeAcitivity


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


            // enable the start button
            nextButton.isEnabled = true
        }

        // set up the RecyclerView
        optionsRecyclerView.layoutManager = LinearLayoutManager(this)
        optionsRecyclerView.adapter = optionAdapter

        // set up the next button to navigate to second activity
        nextButton.setOnClickListener {
            val intent = Intent(this, SelectTypeAcitivity::class.java)
            startActivity(intent)
        }

    }


}
