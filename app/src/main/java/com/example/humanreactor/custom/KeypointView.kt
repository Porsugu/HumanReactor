package com.example.humanreactor.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class KeypointView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val keypoints = mutableListOf<KeypointDrawData>()
    private val connections = mutableListOf<ConnectionDrawData>()

    private val keypointPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val connectionPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    fun setKeypoints(points: List<KeypointDrawData>, connects: List<ConnectionDrawData>) {
        keypoints.clear()
        keypoints.addAll(points)

        connections.clear()
        connections.addAll(connects)

        invalidate()
    }

    fun clear() {
        keypoints.clear()
        connections.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 先繪製連接線
        for (connection in connections) {
            canvas.drawLine(
                connection.startX, connection.startY,
                connection.endX, connection.endY,
                connectionPaint
            )
        }

        // 再繪製關鍵點
        for (keypoint in keypoints) {
            val radius = when {
                keypoint.score > 0.8 -> 10f  // 高置信度的點繪製得大一些
                keypoint.score > 0.5 -> 6f   // 中等置信度
                else -> 4f                  // 低置信度
            }

            canvas.drawCircle(keypoint.x, keypoint.y, radius, keypointPaint)
        }
    }
}