package com.example.humanreactor

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout

class QuickThinkerActivity :AppCompatActivity(), View.OnClickListener {

    private lateinit var english_btn: ConstraintLayout
    private lateinit var chinese_btn: ConstraintLayout
    private lateinit var japanse_btn: ConstraintLayout
    private lateinit var next_btn: ConstraintLayout
    private var selectedOption: ConstraintLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.quick_thinker_description_layout)

        //initialize views
        english_btn = findViewById(R.id.language_english_BTN)
        chinese_btn = findViewById(R.id.language_chinese_BTN)
        japanse_btn = findViewById(R.id.language_japanese_BTN)
        next_btn = findViewById(R.id.language_next_BTN)

        // set click listener for options
        english_btn.setOnClickListener(this)
        chinese_btn.setOnClickListener(this)
        japanse_btn.setOnClickListener(this)


        // set click listener for next button
        next_btn.setOnClickListener {
            selectedOption?.let{

                // handle the next action
                val selectedOptionText = when (selectedOption) {
                    english_btn -> "english"
                    chinese_btn -> "chinese"
                    japanse_btn -> "japanese"
                    else -> ""
                }

                Toast.makeText(
                    this,
                    "Selected: $selectedOptionText. Moving to next step!",
                    Toast.LENGTH_SHORT
                ).show()

                // if it is clicked,  go to the next class, the option class

            }

        }

    } // end of onCreate function

    // to set the items to be on click
    override fun onClick(view: View) {
        // reset all  options to unselected state
        english_btn.isSelected = false
        chinese_btn.isSelected = false
        japanse_btn.isSelected = false

        // set the clicked options as selected
        view.isSelected = true
        selectedOption = view as ConstraintLayout

        // enable the next button
        next_btn.isEnabled = true
        next_btn.isClickable = true
//        next_btn.isFocused = true

    } // end of onClick function

} //  end of the class