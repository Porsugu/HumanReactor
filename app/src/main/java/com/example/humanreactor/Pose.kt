package com.example.humanreactor

// 簡化的姿勢數據結構
data class Pose(
    val keypoints: List<Keypoint>
)

data class Keypoint(
    val type: KeypointType,
    val position: Position,
    val score: Float
)

data class Position(
    val x: Float,
    val y: Float
)

enum class KeypointType {
    NOSE, LEFT_EYE, RIGHT_EYE, LEFT_EAR, RIGHT_EAR,
    LEFT_SHOULDER, RIGHT_SHOULDER, LEFT_ELBOW, RIGHT_ELBOW,
    LEFT_WRIST, RIGHT_WRIST, LEFT_HIP, RIGHT_HIP,
    LEFT_KNEE, RIGHT_KNEE, LEFT_ANKLE, RIGHT_ANKLE
}