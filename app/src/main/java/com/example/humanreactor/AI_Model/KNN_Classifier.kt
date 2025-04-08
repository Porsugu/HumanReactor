package com.example.humanreactor.AI_Model

import com.example.humanreactor.customizedMove.NormalizedSample
import kotlin.math.sqrt

/**
 * Pose classification model using KNN algorithm
 */
class KNN_Classifier(
    private val k: Int = 5,
    private val confidenceThreshold: Float = 0.95f,
    private val useWeightedVoting: Boolean = true  // 使用距離加權投票
) {
    private val samples = mutableListOf<NormalizedSample>()
    private var labels = setOf<String>()

    fun train(newSamples: List<NormalizedSample>) {
        samples.clear()
        samples.addAll(newSamples)
        labels = newSamples.map { it.label }.toSet()
    }

    fun predict(features: List<Float>): Pair<String, Float> {
        if (samples.isEmpty()) return Pair("unknown", 0f)

        // 找到k個最近的樣本
        val neighbors = samples
            .map { Pair(it, calculateDistance(features, it.features)) }
            .sortedBy { it.second }
            .take(k)

        if (useWeightedVoting) {
            // 距離加權投票（越近權重越大）
            val votes = mutableMapOf<String, Float>()
            var totalWeight = 0f

            neighbors.forEach { (sample, distance) ->
                // 避免除以零
                val weight = if (distance < 0.0001f) 1000f else 1f / distance
                votes[sample.label] = (votes[sample.label] ?: 0f) + weight
                totalWeight += weight
            }

            // 找出得票最多的標籤
            val (bestLabel, bestVotes) = votes.maxByOrNull { it.value } ?:
            return Pair("unknown", 0f)

            // 計算置信度
            val confidence = bestVotes / totalWeight

            return if (confidence >= confidenceThreshold) {
                Pair(bestLabel, confidence)
            } else {
                Pair("unknown", confidence)
            }
        } else {
            // 基本KNN計數
            val counts = neighbors.groupBy { it.first.label }
                .mapValues { it.value.size }

            val (bestLabel, bestCount) = counts.maxByOrNull { it.value } ?:
            return Pair("unknown", 0f)

            val confidence = bestCount.toFloat() / k

            return if (confidence >= confidenceThreshold) {
                Pair(bestLabel, confidence)
            } else {
                Pair("unknown", confidence)
            }
        }
    }

    private fun calculateDistance(a: List<Float>, b: List<Float>): Float {
        var sum = 0f
        for (i in a.indices) {
            val diff = a[i] - b[i]
            sum += diff * diff
        }
        return sqrt(sum)
    }

    fun isTrained() = samples.isNotEmpty()
    fun getLabels() = labels.toList()
}