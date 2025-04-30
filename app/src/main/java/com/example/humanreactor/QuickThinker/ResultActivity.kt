package com.example.humanreactor.QuickThinker

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.example.humanreactor.MainActivity
import com.example.humanreactor.R
import com.example.humanreactor.databases.QuizDatabaseHelper
import com.example.humanreactor.databases.QuizFinishRecord
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter

class ResultActivity : AppCompatActivity() {

    private lateinit var pieChart : PieChart
    private lateinit var lineChart : LineChart
    private var correctAnswers: Int = 0
    private var questionTotal: Int = 0
    private var correctRate: Float = 0F
    private lateinit var timeList : List<Long>
    private lateinit var sharedPrefManager: SharedPrefManager
    private lateinit var quizDatabaseHelper : QuizDatabaseHelper


    private lateinit var category : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.quick_thinker_result_activity)

        // initialize the chart
        pieChart = findViewById(R.id.correctnessChart)
        lineChart = findViewById(R.id.responseTimeChart)

        quizDatabaseHelper = QuizDatabaseHelper(this)

        // initialize shared preference manager
        sharedPrefManager = SharedPrefManager(this)

        // calculate the correct answers with the values
        correctAnswers = sharedPrefManager.getCorrectNum()
        questionTotal = sharedPrefManager.getNumber()
        timeList = sharedPrefManager.getTimeList()

        // cal the correct answer rate
        correctRate = correctAnswers.toFloat() / questionTotal.toFloat()

        // set the back to amin button
        findViewById<ConstraintLayout>(R.id.result_back_btn).setOnClickListener {

            //  add quiz category into the data base
            val catID = quizDatabaseHelper.addQuizCategory(sharedPrefManager.getCategory().toString()).toInt()

            // Add record associated with the category
            val quizRecord =QuizFinishRecord(
                categoryId = catID,
                avgAnswerTime = timeList.sum().toFloat() / questionTotal.toFloat(),
                accuracy = correctRate,
                totalQuestions = questionTotal
            )
            quizDatabaseHelper.addQuizRecord(quizRecord)

            // showing these in log format to check
            val categories = quizDatabaseHelper.getAllQuizCategories()
            for (category in categories) {
                Log.d("Category", "ID: ${category.id}, Name: ${category.name}")

                val records = quizDatabaseHelper.getQuizRecordsByCategory(category.id)

                if (records.isNotEmpty()) {
                    val times = records.map { it.avgAnswerTime }
                    val averageTime = times.sum() / times.size

                    Log.d("CategoryStats", "Average Answer Time: $averageTime seconds")

                    for (record in records) {
                        Log.d("Record", """
                            Record ID: ${record.id}
                            Category ID: ${record.categoryId}
                            Avg Time: ${record.avgAnswerTime}
                            Accuracy: ${record.accuracy}
                            Total Questions: ${record.totalQuestions}
                            Timestamp: ${record.timestamp}
                        """.trimIndent())
                    }
                }
                else {
                    Log.d("CategoryStats", "No quiz records found for category '${category.name}'")
                }
            }


            // jumping back to main page
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }


        Log.d("Result Activity", "The correct Rate here is $correctRate")

        // setting up pie chart
        setupPieChart(correctRate)
        setupLineChart()
    }

    // setting up pie chart
    private fun setupPieChart(correctRate : Float){
        with(pieChart){

            // set background colour
            setBackgroundResource(R.color.block_divider_black)
            val customFont = ResourcesCompat.getFont(context, R.font.ibmplexmono_regular)

            // setting the center words
            setDrawCenterText(true)
            centerText = String.format("%.1f%%", correctRate * 100)
            setCenterTextSize(18f)
            setCenterTextColor(Color.parseColor("#F4A261"))
            setCenterTextTypeface(Typeface.DEFAULT_BOLD)
            setCenterTextTypeface(customFont)

            // setting the pie values
            isDrawHoleEnabled = true
            setHoleColor(getColor(R.color.block_option_transparent))
            setTransparentCircleColor(getColor(R.color.block_divider_black))
            setTransparentCircleAlpha(100)
            holeRadius = 58f
            transparentCircleRadius = 68f

            description.isEnabled = false

            legend.typeface = customFont
            legend.isEnabled = true
            legend.textColor = Color.WHITE
            legend.textSize = 16f
            legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
            legend.form = com.github.mikephil.charting.components.Legend.LegendForm.SQUARE  // More visible form
            // set up the rotation angle to 0
            rotationAngle = 0f

            // setting the data
            val entries = ArrayList<PieEntry>()
            entries.add(PieEntry(correctRate, "Correct"))
            entries.add(PieEntry(1 - correctRate, "Wrong"))


            // initializing the dataset
            val dataSet = PieDataSet(entries, "")
            dataSet.valueTypeface = customFont
            dataSet.colors = listOf(getColor(R.color.main), getColor(R.color.onMain))
            dataSet.valueTextColor = Color.parseColor("#4CAF50") // Change to your desired color for values
            setCenterTextTypeface(Typeface.DEFAULT_BOLD)
            setCenterTextTypeface(customFont)
            dataSet.setDrawValues(false)  // 不显示数值
            dataSet.sliceSpace = 2f  // 在扇区之间添加小间距

            val data = PieData(dataSet)
            data.setValueTypeface(customFont)
            setData(data)

            // This is the key change - set the entry label color to black
            setDrawEntryLabels(false) // Make sure labels are enabled

            // adding rotation and highlight
            isRotationEnabled = true
            isHighlightPerTapEnabled = true

            // not using all the instruction lines
            dataSet.valueLinePart1OffsetPercentage = 0f
            dataSet.valueLinePart1Length = 0f
            dataSet.valueLinePart2Length = 0f
            dataSet.valueLineWidth = 0f
            dataSet.valueLineColor = Color.parseColor("#4CAF50")

            // adding the animation
            animateY(1400)

            invalidate()  // update the pie chart

        }
    }

    // setting up line chart
    private fun setupLineChart(){
        with(lineChart){

            setBackgroundResource(R.color.block_divider_black)
            val customFont = ResourcesCompat.getFont(context, R.font.ibmplexmono_regular)
            description.isEnabled = false

            // off the touch mode has here we dont have to
            setTouchEnabled(false)
            isDragEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)

            // enableed the chart animation
            animateX(1500)

            // design the chart sample
            legend.textColor = R.color.text
            legend.textSize = 16f

            // setting x axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = getColor(R.color.text)
                setDrawGridLines(true)      // should it be false?
                gridColor = Color.parseColor("#242424")
                axisLineColor = getColor(R.color.text)
                textSize = 16f

                // force exactly the labels as the question numbers
                setLabelCount(questionTotal, true)

                // Force labels only at exact positions
                granularity = 1f
                isGranularityEnabled = true

                // Ensure labels are centered under the data points
                setCenterAxisLabels(false)

                // setting custom font
                typeface = customFont

                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "Q${value.toInt() + 1}"
                    }
                }
            } // end of x axis apply


            // setting y axis
            axisLeft.apply{
                textColor = getColor(R.color.text)
                setDrawGridLines(true)
                textSize = 14f
                gridColor = Color.parseColor("#242424")
                axisLineColor = getColor(R.color.text)

                // setting custom font
                typeface = customFont

                // formatting the seconds from milliseconds into a better one
                valueFormatter = object  : ValueFormatter(){
                    override fun getFormattedValue(value: Float): String {

                        // converting miliseconds to seconds with 2 decimal place
                        val seconds = value
                        return String.format("%.3fs", seconds)
                    }
                }

            }


            axisRight.isEnabled = false     // not letting it use the right hand side of the axis


            // populate the line chart data

            // if there are no data, return
            if(timeList.isEmpty()) return

            // use milliseconds directly
            val entries = timeList.mapIndexed( { index, time ->
                Entry (index.toFloat(), time.toFloat() / 1000)
            })

            // create a single dataset for all answers
            val dataset = LineDataSet(entries, "Response Time (ms)").apply{

                color = Color.parseColor("#F4A261")
                // set the circle color for the points
                setCircleColor(color)
                lineWidth = 2f
                circleRadius = 4f
                setDrawCircleHole(true)
                circleHoleRadius = 2f
                circleHoleColor = getColor(R.color.text)

                valueTextColor = Color.parseColor("#B1AFAF")
                valueTextSize = 13f
                mode = LineDataSet.Mode.CUBIC_BEZIER
                cubicIntensity = 0.2f

                setDrawFilled(true)
//                fillColor = Color.parseColor("#5500E676") // 半透明填充
                highLightColor = Color.parseColor("#FFFFFF")

                // formatting the seconds from milliseconds into a better one
                valueFormatter = object  : ValueFormatter(){
                    override fun getFormattedValue(value: Float): String {

                        // converting miliseconds to seconds with 2 decimal place
                        val seconds = value
                        return String.format("%.3fs", seconds)
                    }
                }
            }

            data?.setValueTypeface(customFont)

            legend.apply {
                textColor = Color.parseColor("#B1AFAF")
                textSize = 14f
                legend.typeface = customFont
                // Any other legend properties you want to set
            }

            // apply to the chart
            val lineData = LineData(dataset)
            lineChart.data = lineData
            lineChart.invalidate()    // refresh chart


        }
    }

}