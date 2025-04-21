//package com.example.humanreactor.chart
//
//import android.content.Context
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Paint
//import android.util.AttributeSet
//import android.view.View
//import com.example.humanreactor.databases.PerformanceRecord
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//
//class PixelHistoryChartView @JvmOverloads constructor(
//    context: Context,
//    attrs: AttributeSet? = null,
//    defStyleAttr: Int = 0
//) : View(context, attrs, defStyleAttr) {
//
//    private val paint = Paint()
//    private val textPaint = Paint().apply {
//        color = Color.BLACK
//        textSize = 25f
//        textAlign = Paint.Align.CENTER
//    }
//
//    private val records = mutableListOf<PerformanceRecord>()
//    private var maxValue: Float = 1f
//    private var unit: String = ""
//    private var color: Int = Color.GREEN
//
//    private val pixelSize = 8
//    private val pixelGap = 2
//
//    fun setRecords(records: List<PerformanceRecord>, maxValue: Float = 1f, unit: String = "", color: Int = Color.GREEN) {
//        this.records.clear()
//        this.records.addAll(records)
//        this.maxValue = maxValue
//        this.unit = unit
//        this.color = color
//        invalidate()
//    }
//
//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//
//        if (records.isEmpty()) {
//            drawNoDataMessage(canvas)
//            return
//        }
//
//        val width = width
//        val height = height
//
//        val totalRecords = records.size
//        val barWidth = width / totalRecords
//
//        for (i in 0 until totalRecords) {
//            val record = records[i]
//            val value = if (unit == "ms") record.responseTime.toFloat() else record.accuracy.toFloat()
//            val normalizedValue = (value / maxValue).coerceIn(0f, 1f)
//
//            val barHeight = (height * 0.8 * normalizedValue).toInt()
//            val barStartX = i * barWidth
//            val barStartY = height - barHeight
//
//            drawPixelBar(canvas, barStartX, barStartY, barWidth, barHeight, color)
//
//            val valueText = if (unit == "ms") {
//                String.format("%.0f", value)
//            } else {
//                String.format("%.0f%%", value * 100)
//            }
//            canvas.drawText(
//                valueText,
//                barStartX + barWidth / 2f,
//                barStartY - 10f,
//                textPaint
//            )
//
//            val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
//            val dateText = dateFormat.format(Date(record.timestamp))
//            textPaint.textSize = 20f
//            canvas.drawText(
//                dateText,
//                barStartX + barWidth / 2f,
//                height - 10f,
//                textPaint
//            )
//            textPaint.textSize = 25f
//        }
//    }
//
//    private fun drawPixelBar(canvas: Canvas, startX: Int, startY: Int, width: Int, height: Int, color: Int) {
//        paint.color = color
//
//        val pixelsWide = (width - pixelGap) / (pixelSize + pixelGap)
//        val pixelsHigh = (height - pixelGap) / (pixelSize + pixelGap)
//
//        for (x in 0 until pixelsWide) {
//            for (y in 0 until pixelsHigh) {
//                val pixelX = startX + x * (pixelSize + pixelGap) + pixelGap
//                val pixelY = startY + y * (pixelSize + pixelGap) + pixelGap
//
//                canvas.drawRect(
//                    pixelX.toFloat(),
//                    pixelY.toFloat(),
//                    (pixelX + pixelSize).toFloat(),
//                    (pixelY + pixelSize).toFloat(),
//                    paint
//                )
//            }
//        }
//    }
//
//    private fun drawNoDataMessage(canvas: Canvas) {
//        textPaint.textSize = 30f
//        canvas.drawText(
//            "No data available",
//            width / 2f,
//            height / 2f,
//            textPaint
//        )
//        textPaint.textSize = 25f
//    }
//}

package com.example.humanreactor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.example.humanreactor.databases.PerformanceRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PixelHistoryChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint()
    private val textPaint = Paint().apply {
        color = Color.parseColor("#FFF8F0")
        textSize = 25f
        textAlign = Paint.Align.CENTER
    }
    private val textBackgroundPaint = Paint().apply {
//        color = Color.parseColor("#DDFFFFFF")
        style = Paint.Style.FILL
    }
    private val backgroundPaint = Paint().apply {
//        color = Color.WHITE
        color = Color.parseColor("#66363636")
    }

    private val records = mutableListOf<PerformanceRecord>()
    private var maxValue: Float = 1f
    private var unit: String = ""
    private var color: Int = Color.GREEN
    private var backgroundColor: Int = Color.WHITE

    private val pixelSize = 8
    private val pixelGap = 2

    fun setRecords(records: List<PerformanceRecord>, maxValue: Float = 1f, unit: String = "", color: Int = Color.GREEN) {
        this.records.clear()
        this.records.addAll(records)
        this.maxValue = maxValue
        this.unit = unit
        this.color = color
        invalidate()
    }

    override fun setBackgroundColor(color: Int) {
        this.backgroundColor = color
        backgroundPaint.color = color
        invalidate()
    }

    fun setTextBackgroundColor(color: Int) {
        textBackgroundPaint.color = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        if (records.isEmpty()) {
            drawNoDataMessage(canvas)
            return
        }

        val width = width
        val height = height

        val totalRecords = records.size
        val barWidth = width / totalRecords

        for (i in 0 until totalRecords) {
            val record = records[i]
            val value = if (unit == "ms") record.responseTime.toFloat() else record.accuracy.toFloat()
            val normalizedValue = (value / maxValue).coerceIn(0f, 1f)

            val barHeight = (height * 0.8 * normalizedValue).toInt()
            val barStartX = i * barWidth
            val barStartY = height - barHeight

            drawPixelBar(canvas, barStartX, barStartY, barWidth, barHeight, color)

            val valueText = if (unit == "ms") {
                String.format("%.0f", value)
            } else {
                String.format("%.0f%%", value * 100)
            }

            val textBounds = Rect()
            textPaint.getTextBounds(valueText, 0, valueText.length, textBounds)

            val textX = barStartX + barWidth / 2f
            val textY = barStartY - 10f
            val padding = 10f

            canvas.drawRoundRect(
                textX - textBounds.width() / 2f - padding,
                textY + textBounds.top - padding,
                textX + textBounds.width() / 2f + padding,
                textY + textBounds.bottom + padding,
                8f, 8f,
                textBackgroundPaint
            )

            canvas.drawText(
                valueText,
                textX,
                textY,
                textPaint
            )

            val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
            val dateText = dateFormat.format(Date(record.timestamp))
            textPaint.textSize = 20f

            val dateBounds = Rect()
            textPaint.getTextBounds(dateText, 0, dateText.length, dateBounds)

            val dateX = barStartX + barWidth / 2f
            val dateY = height - 10f
            val datePadding = 8f

            canvas.drawRoundRect(
                dateX - dateBounds.width() / 2f - datePadding,
                dateY + dateBounds.top - datePadding,
                dateX + dateBounds.width() / 2f + datePadding,
                dateY + dateBounds.bottom + datePadding,
                8f, 8f,
                textBackgroundPaint
            )

            canvas.drawText(
                dateText,
                dateX,
                dateY,
                textPaint
            )
            textPaint.textSize = 25f
        }
    }

    private fun drawPixelBar(canvas: Canvas, startX: Int, startY: Int, width: Int, height: Int, color: Int) {
        paint.color = color

        val pixelsWide = (width - pixelGap) / (pixelSize + pixelGap)
        val pixelsHigh = (height - pixelGap) / (pixelSize + pixelGap)

        for (x in 0 until pixelsWide) {
            for (y in 0 until pixelsHigh) {
                val pixelX = startX + x * (pixelSize + pixelGap) + pixelGap
                val pixelY = startY + y * (pixelSize + pixelGap) + pixelGap

                canvas.drawRect(
                    pixelX.toFloat(),
                    pixelY.toFloat(),
                    (pixelX + pixelSize).toFloat(),
                    (pixelY + pixelSize).toFloat(),
                    paint
                )
            }
        }
    }

    private fun drawNoDataMessage(canvas: Canvas) {
        textPaint.textSize = 30f

        val message = "No data available"

        val bounds = Rect()
        textPaint.getTextBounds(message, 0, message.length, bounds)

        val textX = width / 2f
        val textY = height / 2f
        val padding = 20f

        canvas.drawRoundRect(
            textX - bounds.width() / 2f - padding,
            textY + bounds.top - padding,
            textX + bounds.width() / 2f + padding,
            textY + bounds.bottom + padding,
            12f, 12f,
            textBackgroundPaint
        )

        canvas.drawText(
            message,
            textX,
            textY,
            textPaint
        )

        textPaint.textSize = 25f
    }
}