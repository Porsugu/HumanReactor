package com.example.humanreactor.customizedMove

import com.google.mlkit.vision.pose.Pose


data class Move (
    var name: String,
    var color: Int,
    var isTrained: Boolean = false,
    var samples:  MutableList<Pose> = mutableListOf()){

}