package com.example.humanreactor.aiQuestion

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.humanreactor.R

class SelectTypeAcitivity : AppCompatActivity() {

    private lateinit var questionTypeAdapter: OptionAdapter
    private lateinit var questionNumAdapter: OptionAdapter
    private var questionTypeSelected = false
    private var questionNumSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ai_question_typechoosing)

        // initialize UI elements
        val typeRecyclerView = findViewById<RecyclerView>(R.id.ai_question_type_choosing)
        val numRecyclerView = findViewById<RecyclerView>(R.id.ai_question_number_choosing)
        val nextButton = findViewById<Button>(R.id.ai_question_2type_start)

        // changing the language
        updateUILanguage()

        // initially disable next button until both selection are made
        nextButton.isEnabled = false

        // set up question type options with language changes
        val questionTypeOption = getQuestionTypeOptions()

        questionTypeAdapter = OptionAdapter(questionTypeOption){_, selectedOption ->
            // update data class with selection
            DataManager.selectionData.question_type = selectedOption
            questionTypeSelected = true

            // Check if both selections are made
            checkEnableNextButton(nextButton)
        }

        // set up question type RecyclerView
        typeRecyclerView.layoutManager = LinearLayoutManager(this)
        typeRecyclerView.adapter = questionTypeAdapter

        // setting up number type under it

        val questionNumOption = listOf("5","7","10")

        questionNumAdapter = OptionAdapter(questionNumOption){_, selectedOption ->
            // update data class with selection
            DataManager.selectionData.question_number = selectedOption.toInt()
            questionNumSelected = true

            //check if both selections are made
            checkEnableNextButton(nextButton)
        }

        numRecyclerView.layoutManager = LinearLayoutManager(this)
        numRecyclerView.adapter = questionNumAdapter

        // update the text of the button
        updateButtonText(nextButton)

        // set up the next button to navigate to second activity
        nextButton.setOnClickListener {
            val intent = Intent(this, AIQuestionMainActivity::class.java)
            startActivity(intent)
        }


    }

    // getting the language type of the questions
    private fun getQuestionTypeOptions() : List<String>{
        return when(DataManager.selectionData.language){
            "中文"-> listOf("面試","情侶關係")
            "English"-> listOf("Interview", "X crash car in relationship")
            else -> listOf("Nothing", "Nothing")
        }
    }

    // updating the text language of the UI
    private fun updateUILanguage(){

        // find the UI type that have to be changing
        val typeTextView = findViewById<TextView>(R.id.ai_question_type_choose_txt)
        val numTextView = findViewById<TextView>(R.id.ai_question_num_choose_txt)

        when (DataManager.selectionData.language){
            "中文"-> {
                typeTextView.text = "請選擇題目類型"
                numTextView.text = "請選擇題目數量"
            }
            "English" -> {
                typeTextView.text = "Please choose the type you would like to challenge"
                numTextView.text = "Please choose the amount of questions"
            }
        }
    }

    // update the button text language
    private fun updateButtonText(button: Button){
        when (DataManager.selectionData.language){
            "中文" -> button.text = "開始"
            "English" -> "Start"
            else -> button.text = "HI"
        }
    }


    private fun checkEnableNextButton(nextButton: Button){
        // enable next button only if both selection have been made
        nextButton.isEnabled = questionTypeSelected && questionNumSelected
    }
}