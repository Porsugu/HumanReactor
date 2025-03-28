package com.example.humanreactor.custom

data class Motion(
    val name: String,
    val color: Int,
    var isTrained: Boolean = false,
    var shoulderAngle: Double = 0.0,    //right shoulder to nose to left shoulder
    var leftElbowAngle: Double = 0.0,   //left wrist to left elbow to left shoulder
    var rightElbowAngle: Double = 0.0, //right wrist to right elbow to right shoulder
    var leftWristAngle: Double = 0.0,
    var rightWristAngle: Double = 0.0,
    var rightArmAngle: Double = 0.0,
    var leftArmAngle: Double = 0.0,
    var neckAngle: Double = 0.0,
    var samples:  MutableList<Pose> = mutableListOf()
) {
    // 檢查是否已有角度數據
    fun hasAngles(): Boolean {
        return shoulderAngle != 0.0 || leftElbowAngle != 0.0 || rightElbowAngle != 0.0
    }
}
//hi