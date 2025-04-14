package com.example.humanreactor.QuickThinker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.humanreactor.R

class QuickThinkerQuestionType:AppCompatActivity(), View.OnClickListener {

    private lateinit var interview_btn: ConstraintLayout
    private lateinit var relationship_btn: ConstraintLayout
    private lateinit var more_btn: ConstraintLayout
    private lateinit var five_btn: ConstraintLayout
    private lateinit var seven_btn: ConstraintLayout
    private lateinit var nine_btn: ConstraintLayout
    private lateinit var next_btn: ConstraintLayout
    private lateinit var sharedPrefManager: SharedPrefManager
    private var selectedOption1: ConstraintLayout? = null
    private var selectedOption2: ConstraintLayout? = null

    // look at each category selected satatus
    private var category1Selected = false
    private var category2Selected = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.quicker_thinker_question_type)

        //initialize views
        interview_btn = findViewById(R.id.type_interview_BTN)
        relationship_btn = findViewById(R.id.type_relationship_BTN)
        more_btn = findViewById(R.id.type_more_BTN)
        five_btn = findViewById(R.id.number_5_BTN)
        seven_btn = findViewById(R.id.number_7_BTN)
        nine_btn = findViewById(R.id.number_9_BTN)
        next_btn = findViewById(R.id.type_next_BTN)

        // set click listener for options
        interview_btn.setOnClickListener(this)
        relationship_btn.setOnClickListener(this)
        more_btn.setOnClickListener(this)
        five_btn.setOnClickListener(this)
        seven_btn.setOnClickListener(this)
        nine_btn.setOnClickListener(this)
        next_btn.setOnClickListener(this)

        sharedPrefManager = SharedPrefManager(this)

        // set click listener for next button
        next_btn.setOnClickListener {

            // handle the next action
            val selectedOptionType = when(selectedOption1){
                interview_btn -> "interview"
                relationship_btn -> "relationship"
                // for more button will implement the customized category and show it in the box
                // more_btn -> "other"
                else -> ""
            }

            val selectedOptionNumber = when(selectedOption2){
                five_btn -> "5"
                seven_btn -> "7"
                nine_btn -> "9"
                else -> ""
            }

            sharedPrefManager.saveCategory(selectedOptionType)
            sharedPrefManager.saveNumber(selectedOptionNumber.toInt())
            // if it is clicked,  go to the next class, the main running class
//            val intent = Intent(this, QuickThinkerQuestionType::class.java)
//            startActivity(intent)
        }


    }// end of onCreate function

    // set the items to be on click
    override fun onClick(v: View?) {
        when(v){
            // first type option
            interview_btn -> {
                //resetting the first cateoory
                interview_btn.isSelected = false
                relationship_btn.isSelected = false
                more_btn.isSelected = false

                // set it as correct selected option
                interview_btn.isSelected = true

                // update the use
                selectedOption1 = interview_btn

                // update categoty one usage
                category1Selected = true
            }
            relationship_btn -> {
                interview_btn.isSelected = false
                relationship_btn.isSelected = false
                more_btn.isSelected = false

                relationship_btn.isSelected = true
                selectedOption1 = relationship_btn
                category1Selected = true
            }

//            more_btn -> {
//                interview_btn.isSelected = false
//                relationship_btn.isSelected = false
//                more_btn.isSelected = false
//
//                more_btn.isSelected = true
//
//                // 更新選中的選項引用
//                selectedOption1 = more_btn
//
//                // 更新第一類別的選擇狀態
//                category1Selected = true
//            }

            // second option
            five_btn -> {
                // reset second option status
                five_btn.isSelected = false
                seven_btn.isSelected = false
                nine_btn.isSelected = false

                five_btn.isSelected = true
                selectedOption2 = five_btn
                category2Selected = true
            }
            seven_btn -> {
                five_btn.isSelected = false
                seven_btn.isSelected = false
                nine_btn.isSelected = false

                seven_btn.isSelected = true
                selectedOption2 = seven_btn
                category2Selected = true
            }
            nine_btn -> {
                five_btn.isSelected = false
                seven_btn.isSelected = false
                nine_btn.isSelected = false

                nine_btn.isSelected = true
                selectedOption2 = nine_btn
                category2Selected = true
            }
        }

        updateNextButtonState()     // update the next button status

    } // end of OnClick function

    // update the next button situation
    private fun updateNextButtonState(){
        val bothSelected = category1Selected && category2Selected

        // only when both are selected, the next button will work
        next_btn.isEnabled = bothSelected
        next_btn.isClickable = bothSelected

        val selectedOptionText1 = when (selectedOption1) {
            interview_btn -> "interview"
            relationship_btn -> "relationship"
            else -> ""
        }

        val selectedIOptionText2 = when (selectedOption2){
            five_btn -> "5"
            seven_btn -> "7"
            nine_btn -> "9"
            else -> ""
        }

        Toast.makeText(
            this,
            "Selected: $selectedOptionText1, $selectedIOptionText2. Moving to next step!",
            Toast.LENGTH_SHORT
        ).show()

        // only change when i want
        if(bothSelected){
            next_btn.alpha = 1.0f
        } else{
            next_btn.alpha = 0.5f
        }
    }



}