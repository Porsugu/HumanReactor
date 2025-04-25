package com.example.humanreactor

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.humanreactor.QuickThinker.QuickThinkerQuestionType
import com.example.humanreactor.QuickThinker.SharedPrefManager

class QuickThinkerActivity :AppCompatActivity(), View.OnClickListener {

    private lateinit var english_btn: ConstraintLayout
    private lateinit var chinese_btn: ConstraintLayout
    private lateinit var japanse_btn: ConstraintLayout
    private lateinit var next_btn: ConstraintLayout
    private var selectedOption: ConstraintLayout? = null
    private lateinit var sharedPrefManager: SharedPrefManager

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

        sharedPrefManager = SharedPrefManager(this)

        // Check internet connectivity and update UI accordingly
        updateLanguageButtonsBasedOnConnectivity()


        // set click listener for next button
        next_btn.setOnClickListener {
            selectedOption?.let{

                // handle the next action
                val selectedOptionText = when (selectedOption) {
                    english_btn -> "english"
                    // will open it later
                    chinese_btn -> "traditional chinese"
                    japanse_btn -> "japanese"
                    else -> ""
                }

                Toast.makeText(
                    this,
                    "Selected: $selectedOptionText. Moving to next step!",
                    Toast.LENGTH_SHORT
                ).show()

                sharedPrefManager.saveLanguage(selectedOptionText)

                // if it is clicked,  go to the next class, the option class
                val intent = Intent(this, QuickThinkerQuestionType::class.java)
                startActivity(intent)

            }

        }

    } // end of onCreate function

    // Function to check internet connectivity
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
        } else {
            // For devices running Android versions below M
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo != null && networkInfo.isConnected
        }
    }

    // Function to update buttons based on connectivity
    private fun updateLanguageButtonsBasedOnConnectivity() {
        if (isNetworkAvailable()) {
            // Enable Chinese and Japanese buttons if internet is available
            chinese_btn.visibility = View.VISIBLE
            japanse_btn.visibility = View.VISIBLE
            chinese_btn.isEnabled = true
            japanse_btn.isEnabled = true
        } else {
            // Disable or hide Chinese and Japanese buttons if no internet, hide the buttons completely
            chinese_btn.visibility = View.GONE
            japanse_btn.visibility = View.GONE

            // Reset selection if either Chinese or Japanese was selected
            if (selectedOption == chinese_btn || selectedOption == japanse_btn) {
                selectedOption = null
                chinese_btn.isSelected = false
                japanse_btn.isSelected = false
                next_btn.isEnabled = false
                next_btn.isClickable = false
            }

            // Show a message to the user
            Toast.makeText(
                this,
                "Chinese and Japanese options require internet connection",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

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