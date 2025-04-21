package com.example.humanreactor.chart

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class PixelChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint()
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    private var value: Float = 0f  // Value between 0 and 1
    private var maxValue: Float = 1f
    private var unit: String = ""
    private var color: Int = Color.GREEN

    // Size of each pixel block
    private val pixelSize = 12

    fun setValue(value: Float, maxValue: Float = 1f, unit: String = "", color: Int = Color.GREEN) {
        this.value = value.coerceIn(0f, maxValue)
        this.maxValue = maxValue
        this.unit = unit
        this.color = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width
        val height = height

        // Calculate how many blocks we can fit
        val blocksWide = width / (pixelSize + 2)
        val blocksHigh = (height - 40) / (pixelSize + 2)  // Leave space for text

        // How many blocks to fill based on value
        val fillBlocks = ((value / maxValue) * blocksWide).toInt()

        // Draw background grid
        paint.color = Color.LTGRAY
        for (x in 0 until blocksWide) {
            for (y in 0 until blocksHigh) {
                canvas.drawRect(
                    (x * (pixelSize + 2)).toFloat(),
                    (y * (pixelSize + 2)).toFloat(),
                    ((x + 1) * pixelSize + x * 2).toFloat(),
                    ((y + 1) * pixelSize + y * 2).toFloat(),
                    paint
                )
            }
        }

        // Draw value blocks
        paint.color = color
        for (x in 0 until fillBlocks) {
            for (y in 0 until blocksHigh) {
                canvas.drawRect(
                    (x * (pixelSize + 2)).toFloat(),
                    (y * (pixelSize + 2)).toFloat(),
                    ((x + 1) * pixelSize + x * 2).toFloat(),
                    ((y + 1) * pixelSize + y * 2).toFloat(),
                    paint
                )
            }
        }

        // Draw value text
        val valueText = if (unit.isNotEmpty()) {
            "${value.toInt()} $unit"
        } else {
            String.format("%.1f%%", value * 100)
        }

        canvas.drawText(
            valueText,
            (width / 2).toFloat(),
            (height - 10).toFloat(),
            textPaint
        )
    }
}