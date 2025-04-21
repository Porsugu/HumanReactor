package com.example.humanreactor.statActivity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.humanreactor.R

class StatActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stat)

        val btnAction = findViewById<Button>(R.id.btn_action)
        val btnMental = findViewById<Button>(R.id.btn_mental)

        btnAction.setOnClickListener {
            val intent = Intent(this, CategorySelectionActivity::class.java)
            intent.putExtra("type", "action")
            startActivity(intent)
        }

        btnMental.setOnClickListener {
            // For now, we'll just navigate to the same activity
            // but we'll ignore mental processing as per requirements
            val intent = Intent(this, CategorySelectionActivity::class.java)
            intent.putExtra("type", "mental")
            startActivity(intent)
        }
    }
}