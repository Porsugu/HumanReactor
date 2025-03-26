package com.example.humanreactor

data class KeypointDrawData(
    val x: Float,
    val y: Float,
    val score: Float,
    val type: KeypointType
)

data class ConnectionDrawData(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float
)