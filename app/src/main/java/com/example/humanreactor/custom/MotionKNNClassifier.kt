package com.example.humanreactor.custom

import android.util.Log


class MotionKNNClassifier {
    private val trainingData = mutableListOf<Pair<List<Double>, String>>()
    private var k = 3 // 可調整的參數

    // 添加訓練樣本
    fun addSample(features: List<Double>, label: String) {
        trainingData.add(Pair(features, label))
    }

    // 使用已有的動作數據進行訓練
    fun train(dataset: List<Pair<List<List<Double>>, String>>) {
        dataset.forEach { (samplesList, className) ->
            // 對於每個動作類別的所有樣本
            samplesList.forEach { angles ->
                addSample(angles, className)
            }
        }
        Log.d("KNNClassifier", "訓練完成，共有 ${trainingData.size} 個樣本")
    }

    // 預測單一樣本
    fun predict(features: List<Double>): String {
        if (trainingData.isEmpty()) return "未知"

        // 計算到所有訓練樣本的距離
        val distances = trainingData.map { (sampleFeatures, label) ->
            val distance = calculateDistance(features, sampleFeatures)
            Triple(distance, label, sampleFeatures)
        }

        // 找出最近的k個樣本
        val nearest = distances.sortedBy { it.first }.take(k)

        // 計算最常見的類別
        val votes = nearest.groupBy { it.second }
            .mapValues { it.value.size }

        return votes.maxByOrNull { it.value }?.key ?: "未知"
    }

    // 預測一系列樣本（取多數決）
    fun predictMotion(samplesList: List<List<Double>>): String {
        val predictions = samplesList.map { predict(it) }

        // 計算每個類別的出現次數
        val countMap = mutableMapOf<String, Int>()
        predictions.forEach { prediction ->
            countMap[prediction] = (countMap[prediction] ?: 0) + 1
        }

        // 返回出現次數最多的類別
        return countMap.entries.maxByOrNull { it.value }?.key ?: "未知"
    }

    // 計算歐氏距離
    private fun calculateDistance(a: List<Double>, b: List<Double>): Double {
        if (a.size != b.size) {
            throw IllegalArgumentException("特徵向量長度不匹配")
        }

        return Math.sqrt(a.zip(b).sumOf { (x, y) ->
            Math.pow(x - y, 2.0)
        })
    }

    // 清除所有訓練數據
    fun clear() {
        trainingData.clear()
    }

    // 設置K值
    fun setK(newK: Int) {
        if (newK > 0) {
            k = newK
        }
    }
}