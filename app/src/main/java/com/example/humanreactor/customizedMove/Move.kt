package com.example.humanreactor.customizedMove

import com.google.mlkit.vision.pose.Pose

data class NormalizedSample(
    val features: List<Float>,
    val label: String
)

data class Move (
    var name: String,
    var color: Int,
    var isTrained: Boolean = false,
    var isCollected: Boolean = false,
    var samples:  MutableList<Pose> = mutableListOf(),
    var normalizedSamples: MutableList<NormalizedSample> = mutableListOf(),
    var dbId: Int = 0
){

}