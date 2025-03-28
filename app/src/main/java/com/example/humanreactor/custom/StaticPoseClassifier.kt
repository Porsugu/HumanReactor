package com.example.humanreactor.custom

import android.util.Log

class StaticPoseClassifier {
    // 存儲目標姿勢的特徵模板
    private val poseTemplates = mutableMapOf<String, List<Double>>()

    // 相似度閾值
    private var similarityThreshold = 0.80

    fun modelTrained():Boolean{
        return poseTemplates.size > 0
    }

    // 添加姿勢模板
    fun addPoseTemplate(poseName: String, angleFeatures: List<Double>) {
        poseTemplates[poseName] = angleFeatures
    }

    // 從dataset載入所有姿勢
    fun loadFromDataset(dataset: List<Pair<List<List<Double>>, String>>) {
        Log.d("Classifier", "dataset size: ${dataset.size}")
        dataset.forEach { (samplesList, poseName) ->
            // 計算平均特徵作為模板
            val avgFeatures = calculateAverageFeatures(samplesList)
            addPoseTemplate(poseName, avgFeatures)
        }

    }

    // 計算平均特徵
    private fun calculateAverageFeatures(samples: List<List<Double>>): List<Double> {
        if (samples.isEmpty()) return emptyList()

        val featureCount = samples[0].size
        val sums = MutableList(featureCount) { 0.0 }

        samples.forEach { sample ->
            for (i in sample.indices) {
                sums[i] = sums[i] + sample[i]
            }
        }

        // 計算平均值
        return sums.map { it / samples.size }
    }

    // 預測單一樣本
    fun predict(features: List<Double>): Pair<String, Double> {
        var bestMatch = ""
        var bestSimilarity = 0.0

        poseTemplates.forEach { (poseName, template) ->
            val similarity = calculateSimilarity(features, template)

            if (similarity > bestSimilarity) {
                bestSimilarity = similarity
                bestMatch = poseName
            }
        }


        if (bestSimilarity < 0.6){
            bestMatch = "unknown"
        }

        if(bestMatch != "unknown"){
            Log.d("Classifier", "pose: ${bestMatch}")
            Log.d("Classifier", "pose similarity: ${bestSimilarity}")
        }

        return Pair(bestMatch, bestSimilarity)
    }

    // 對一系列樣本進行預測（多數決）
    fun predictPose(samples: List<List<Double>>): String {
        val predictions = samples.map { predict(it) }

        // 篩選高置信度預測
        val confidentPredictions = predictions.filter { (_, confidence) ->
            confidence >= similarityThreshold
        }

        // 如果沒有高置信度預測，返回未知
        if (confidentPredictions.isEmpty()) {
            return "未知"
        }

        // 計算每個姿勢的得票數
        val votes = confidentPredictions.groupBy { it.first }
            .mapValues { it.value.size }

        // 返回得票最多的姿勢
        return votes.maxByOrNull { it.value }?.key ?: "未知"
    }


    // 計算特徵向量間的相似度
    private fun calculateSimilarity(features1: List<Double>, features2: List<Double>): Double {
        if (features1.size != features2.size) return 0.0

        // 計算歐氏距離
        val distance = Math.sqrt(features1.zip(features2).sumOf { (a, b) ->
            Math.pow(a - b, 2.0)
        })

        // 將距離轉換為相似度（0-1範圍，1表示完全匹配）
        // sigma參數控制相似度的容忍度，較大的sigma更寬容
        val sigma = Math.sqrt(features1.size.toDouble()) * 2
        return Math.exp(-(distance * distance) / (2 * sigma * sigma))
    }

    // 設置相似度閾值
    fun setSimilarityThreshold(threshold: Double) {
        if (threshold in 0.0..1.0) {
            similarityThreshold = threshold
        }
    }

    // 獲取所有可用的姿勢
    fun getAvailablePoses(): List<String> {
        return poseTemplates.keys.toList()
    }
}