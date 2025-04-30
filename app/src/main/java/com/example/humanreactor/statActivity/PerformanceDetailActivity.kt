//package com.example.humanreactor.statActivity
//
//import android.graphics.Color
//import android.os.Bundle
//import android.widget.Button
//import android.widget.FrameLayout
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import com.example.humanreactor.PixelHistoryChartView
//import com.example.humanreactor.R
//import com.example.humanreactor.databases.ActionDatabaseHelper
//import kotlin.random.Random
//
//class PerformanceDetailActivity : AppCompatActivity() {
//    private lateinit var tvCategoryName: TextView
//    private lateinit var btnBack: Button
//    private lateinit var btnAddTestData: Button
//    private lateinit var frameResponseTime: FrameLayout
//    private lateinit var frameAccuracy: FrameLayout
//
//    private lateinit var responseTimeChart: PixelHistoryChartView
//    private lateinit var accuracyChart: PixelHistoryChartView
//
//    private lateinit var dbHelper: ActionDatabaseHelper
//    private var categoryId: Int = -1
//    private var categoryName: String = ""
//
//    private val MAX_RESPONSE_TIME = 3000f
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_performance_detail)
//
//        categoryId = intent.getIntExtra("categoryId", -1)
//        categoryName = intent.getStringExtra("categoryName") ?: "Category"
//
//        if (categoryId == -1) {
//            Toast.makeText(this, "Invalid category", Toast.LENGTH_SHORT).show()
//            finish()
//            return
//        }
//
//        tvCategoryName = findViewById(R.id.tv_category_name)
//        btnBack = findViewById(R.id.btn_back)
//        btnAddTestData = findViewById(R.id.btn_add_test_data)
//        frameResponseTime = findViewById(R.id.frame_response_time)
//        frameAccuracy = findViewById(R.id.frame_accuracy)
//
//        tvCategoryName.text = "$categoryName Performance"
//
//        dbHelper = ActionDatabaseHelper(this)
//
//        responseTimeChart = PixelHistoryChartView(this)
//        frameResponseTime.addView(responseTimeChart)
//
//        accuracyChart = PixelHistoryChartView(this)
//        frameAccuracy.addView(accuracyChart)
//
//        loadPerformanceData()
//
//        btnBack.setOnClickListener {
//            finish()
//        }
//
//        btnAddTestData.setOnClickListener {
//            addTestPerformanceRecord()
//            loadPerformanceData()
//            Toast.makeText(this, "Test data added", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun loadPerformanceData() {
//        val records = dbHelper.getRecentPerformanceRecords(categoryId)
//
//        if (records.isEmpty()) {
//            responseTimeChart.setRecords(emptyList(), MAX_RESPONSE_TIME, "ms", Color.rgb(0, 150, 136))
//            accuracyChart.setRecords(emptyList(), 1.0f, "", Color.rgb(255, 193, 7))
//            return
//        }
//
//        responseTimeChart.setRecords(records, MAX_RESPONSE_TIME, "ms",Color.parseColor("#4CAF50"))
//
//        accuracyChart.setRecords(records, 1.0f,
//            "", Color.parseColor("#F4A261"))
//    }
//
//    private fun addTestPerformanceRecord() {
//        val responseTime = Random.nextDouble(500.0, 3000.0)
//        val accuracy = Random.nextDouble(0.5, 1.0)
//
//        val result = dbHelper.addPerformanceRecord(categoryId, responseTime, accuracy)
//    }
//}

package com.example.humanreactor.statActivity

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.humanreactor.PixelHistoryChartView
import com.example.humanreactor.R
import com.example.humanreactor.databases.ActionDatabaseHelper
import com.example.humanreactor.databases.QuizDatabaseHelper
import com.example.humanreactor.databases.PerformanceRecord
import com.example.humanreactor.databases.QuizFinishRecord
import kotlin.random.Random

class PerformanceDetailActivity : AppCompatActivity() {
    private lateinit var tvCategoryName: TextView
    private lateinit var btnBack: Button
    private lateinit var btnAddTestData: Button
    private lateinit var frameResponseTime: FrameLayout
    private lateinit var frameAccuracy: FrameLayout

    private lateinit var responseTimeChart: PixelHistoryChartView
    private lateinit var accuracyChart: PixelHistoryChartView

    private lateinit var actionDbHelper: ActionDatabaseHelper
    private lateinit var quizDbHelper: QuizDatabaseHelper

    private var categoryId: Int = -1
    private var categoryName: String = ""
    private var type: String = "action"  // Default to action

    private val MAX_RESPONSE_TIME = 10000f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_performance_detail)

        // Get data from intent
        categoryId = intent.getIntExtra("categoryId", -1)
        categoryName = intent.getStringExtra("categoryName") ?: "Category"
        type = intent.getStringExtra("type") ?: "action"

        if (categoryId == -1) {
            Toast.makeText(this, "Invalid category", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        tvCategoryName = findViewById(R.id.tv_category_name)
        btnBack = findViewById(R.id.btn_back)
        btnAddTestData = findViewById(R.id.btn_add_test_data)
        frameResponseTime = findViewById(R.id.frame_response_time)
        frameAccuracy = findViewById(R.id.frame_accuracy)

        tvCategoryName.text = "$categoryName Performance"

        // Initialize database helpers
        actionDbHelper = ActionDatabaseHelper(this)
        quizDbHelper = QuizDatabaseHelper(this)

        // Initialize charts
        responseTimeChart = PixelHistoryChartView(this)
        frameResponseTime.addView(responseTimeChart)

        accuracyChart = PixelHistoryChartView(this)
        frameAccuracy.addView(accuracyChart)

        // Load performance data based on type
        loadPerformanceData()

        btnBack.setOnClickListener {
            finish()
        }

        btnAddTestData.setOnClickListener {
            addTestPerformanceRecord()
            loadPerformanceData()
            Toast.makeText(this, "Test data added", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPerformanceData() {
        if (type == "action") {
            loadActionPerformanceData()
        } else if (type == "mental") {
            loadQuizPerformanceData()
        } else {
            Toast.makeText(this, "Invalid type", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadActionPerformanceData() {
        val records = actionDbHelper.getRecentPerformanceRecords(categoryId)

        if (records.isEmpty()) {
            responseTimeChart.setRecords(emptyList(), MAX_RESPONSE_TIME, "ms", Color.rgb(0, 150, 136))
            accuracyChart.setRecords(emptyList(), 1.0f, "", Color.rgb(255, 193, 7))
            return
        }

        // Set records directly for response time chart
        responseTimeChart.setRecords(
            records,
            MAX_RESPONSE_TIME,
            "ms",
            Color.parseColor("#4CAF50")
        )

        // Set records directly for accuracy chart
        accuracyChart.setRecords(
            records,
            1.0f,
            "",
            Color.parseColor("#F4A261")
        )
    }

    private fun loadQuizPerformanceData() {
        val records = quizDbHelper.getQuizRecordsByCategory(categoryId)

        if (records.isEmpty()) {
            responseTimeChart.setRecords(emptyList(), MAX_RESPONSE_TIME, "ms", Color.rgb(0, 150, 136))
            accuracyChart.setRecords(emptyList(), 1.0f, "", Color.rgb(255, 193, 7))
            return
        }

        // We need to adapt quiz records to match the PerformanceRecord format
        val adaptedRecords = records.map { quizRecord ->
            PerformanceRecord(
                id = quizRecord.id,
                categoryId = quizRecord.categoryId,
                responseTime = quizRecord.avgAnswerTime.toDouble(),
                accuracy = quizRecord.accuracy.toDouble(),
                timestamp = quizRecord.timestamp
            )
        }

        // Set records for response time chart
        responseTimeChart.setRecords(
            adaptedRecords,
            MAX_RESPONSE_TIME,
            "ms",
            Color.parseColor("#4CAF50")
        )

        // Set records for accuracy chart
        accuracyChart.setRecords(
            adaptedRecords,
            1.0f,
            "",
            Color.parseColor("#F4A261")
        )
    }

    private fun addTestPerformanceRecord() {
        if (type == "action") {
            addTestActionPerformanceRecord()
        } else if (type == "mental") {
            addTestQuizPerformanceRecord()
        }
    }

    private fun addTestActionPerformanceRecord() {
        val responseTime = Random.nextDouble(500.0, 3000.0)
        val accuracy = Random.nextDouble(0.5, 1.0)

        actionDbHelper.addPerformanceRecord(categoryId, responseTime, accuracy)
    }

    private fun addTestQuizPerformanceRecord() {
        val avgAnswerTime = Random.nextDouble(500.0, 3000.0)
        val accuracy = Random.nextDouble(0.5, 1.0)
        val totalQuestions = Random.nextInt(5, 20)

        val quizRecord = QuizFinishRecord(
            categoryId = categoryId,
            avgAnswerTime = avgAnswerTime.toFloat(),
            accuracy = accuracy.toFloat(),
            totalQuestions = totalQuestions
        )

        quizDbHelper.addQuizRecord(quizRecord)
    }
}