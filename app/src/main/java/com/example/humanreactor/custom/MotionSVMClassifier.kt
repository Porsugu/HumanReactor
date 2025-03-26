package com.example.humanreactor.custom

import android.util.Log
import libsvm.svm
import libsvm.svm_model
import libsvm.svm_node
import libsvm.svm_parameter
import libsvm.svm_problem

//class MotionSVMClassifier {
//    private var model: svm_model? = null
//    private val classLabels = mutableMapOf<Int, String>()
//    private val classIndices = mutableMapOf<String, Int>()
//
//    // 訓練SVM模型
//    fun train(dataset: List<Pair<List<List<Double>>, String>>) {
//        // 準備訓練數據
//        val allSamples = mutableListOf<Pair<List<Double>, String>>()
//
//        dataset.forEach { (samplesList, className) ->
//            samplesList.forEach { angles ->
//                allSamples.add(Pair(angles, className))
//            }
//
//            // 建立類別映射
//            if (!classIndices.containsKey(className)) {
//                val nextIndex = classIndices.size + 1
//                classIndices[className] = nextIndex
//                classLabels[nextIndex] = className
//            }
//        }
//
//        Log.e("SVMClassifier", "Num classes: ${classLabels.size}")
//
//        if (allSamples.isEmpty()) {
//            Log.e("SVMClassifier", "沒有訓練數據")
//            return
//        }
//
//        // 創建SVM問題
//        val prob = svm_problem()
//        prob.l = allSamples.size
//        prob.y = DoubleArray(allSamples.size)
//        prob.x = Array(allSamples.size) { arrayOfNulls<svm_node>(0) }
//
//        // 填充數據
//        allSamples.forEachIndexed { i, (features, label) ->
//            prob.y[i] = classIndices[label]!!.toDouble()
//
//            val nodes = Array(features.size) { j ->
//                svm_node().apply {
//                    index = j + 1
//                    value = features[j]
//                }
//            }
//            prob.x[i] = nodes
//        }
//
//        // 設置參數
//        val param = svm_parameter().apply {
//            svm_type = svm_parameter.C_SVC
//            kernel_type = svm_parameter.RBF
//            gamma = 0.1
//            C = 10.0
//            cache_size = 100.0
//            eps = 0.001
//            probability = 1
//        }
//
//        // 訓練模型
//        model = svm.svm_train(prob, param)
//        Log.d("SVMClassifier", "SVM模型訓練完成")
//    }
//
//    // 預測單一樣本
//    fun predict(features: List<Double>): String {
//        val model = this.model ?: return "未知"
//
//        // 轉換為SVM節點
//        val nodes = Array(features.size) { i ->
//            svm_node().apply {
//                index = i + 1
//                value = features[i]
//            }
//        }
//
//        // 進行預測
//        val prediction = svm.svm_predict(model, nodes)
//
//        return classLabels[prediction.toInt()] ?: "未知"
//    }
//
//    // 預測一系列樣本
//    fun predictMotion(samplesList: List<List<Double>>): String {
//        val predictions = samplesList.map { predict(it) }
//
//        // 多數決
//        val countMap = mutableMapOf<String, Int>()
//        predictions.forEach { prediction ->
//            countMap[prediction] = (countMap[prediction] ?: 0) + 1
//        }
//
//        return countMap.entries.maxByOrNull { it.value }?.key ?: "未知"
//    }
//
//    fun  modelTrained():Boolean{
//        return model != null
//    }
//    // 釋放資源
//    fun clear() {
//        model = null
//        classLabels.clear()
//        classIndices.clear()
//    }
//}

class MotionSVMClassifier {
    private var model: svm_model? = null
    private val classLabels = mutableMapOf<Int, String>()
    private val classIndices = mutableMapOf<String, Int>()
    private var featureRanges = mutableListOf<Pair<Double, Double>>() // 用於存儲每個特徵的最小值和最大值

    fun train(dataset: List<Pair<List<List<Double>>, String>>) {
        if (dataset.isEmpty()) return

        // 1. 準備所有樣本
        val allSamples = mutableListOf<Pair<List<Double>, String>>()
        dataset.forEach { (samplesList, className) ->
            samplesList.forEach { features ->
                allSamples.add(Pair(features, className))
            }

            // 建立類別索引
            if (!classIndices.containsKey(className)) {
                val nextIndex = classIndices.size + 1
                classIndices[className] = nextIndex
                classLabels[nextIndex] = className
            }
        }

        // 2. 計算每個特徵的範圍，用於歸一化
        calculateFeatureRanges(allSamples.map { it.first })

        // 3. 資料增強
        val augmentedSamples = augmentData(allSamples)

        // 4. 尋找最佳參數
        val (bestC, bestGamma) = findBestParameters(augmentedSamples)

        // 5. 使用最佳參數和全部數據進行最終訓練
        trainFinalModel(augmentedSamples, bestC, bestGamma)
    }

    private fun calculateFeatureRanges(samples: List<List<Double>>) {
        if (samples.isEmpty()) return

        val featureCount = samples[0].size
        featureRanges.clear()

        for (i in 0 until featureCount) {
            val values = samples.map { it[i] }
            val min = values.minOrNull() ?: 0.0
            val max = values.maxOrNull() ?: 1.0
            featureRanges.add(Pair(min, max))
        }
    }

    private fun normalizeFeatures(features: List<Double>): List<Double> {
        return features.mapIndexed { index, value ->
            if (index >= featureRanges.size) value else {
                val (min, max) = featureRanges[index]
                if (max == min) 0.5 else (value - min) / (max - min)
            }
        }
    }

    private fun augmentData(samples: List<Pair<List<Double>, String>>): List<Pair<List<Double>, String>> {
        val augmented = mutableListOf<Pair<List<Double>, String>>()

        // 保留原始數據
        augmented.addAll(samples)

        // 添加噪聲版本
        samples.forEach { (features, label) ->
            val noisy = features.map { value ->
                value * (1 + (Math.random() - 0.5) * 0.05) // 添加±2.5%的噪聲
            }
            augmented.add(Pair(noisy, label))
        }

        return augmented
    }

    private fun findBestParameters(samples: List<Pair<List<Double>, String>>): Pair<Double, Double> {
        // 將樣本分為訓練和驗證集
        val shuffled = samples.shuffled()
        val splitIndex = (shuffled.size * 0.7).toInt()
        val trainingSamples = shuffled.take(splitIndex)
        val validationSamples = shuffled.drop(splitIndex)

        // 嘗試不同參數組合
        val cValues = listOf(0.1, 1.0, 10.0, 100.0)
        val gammaValues = listOf(0.001, 0.01, 0.1, 1.0)

        var bestAccuracy = 0.0
        var bestC = 1.0
        var bestGamma = 0.1

        for (c in cValues) {
            for (gamma in gammaValues) {
                val tempModel = trainWithParams(trainingSamples, c, gamma)
                val accuracy = evaluateModel(tempModel, validationSamples)

                if (accuracy > bestAccuracy) {
                    bestAccuracy = accuracy
                    bestC = c
                    bestGamma = gamma
                }
            }
        }

        Log.d("SVMOptimized", "最佳參數: C=$bestC, gamma=$bestGamma, 準確率=$bestAccuracy")
        return Pair(bestC, bestGamma)
    }

    private fun trainWithParams(samples: List<Pair<List<Double>, String>>, c: Double, gamma: Double): svm_model {
        val prob = svm_problem()
        prob.l = samples.size
        prob.y = DoubleArray(samples.size)
        prob.x = Array(samples.size) { arrayOfNulls<svm_node>(0) }

        // 準備歸一化後的數據
        samples.forEachIndexed { i, (features, label) ->
            val normalizedFeatures = normalizeFeatures(features)

            prob.y[i] = classIndices[label]!!.toDouble()
            val nodes = Array(normalizedFeatures.size) { j ->
                svm_node().apply {
                    index = j + 1
                    value = normalizedFeatures[j]
                }
            }
            prob.x[i] = nodes
        }

        // 設置參數
        val param = svm_parameter().apply {
            svm_type = svm_parameter.C_SVC
            kernel_type = svm_parameter.RBF
            this.gamma = gamma
            this.C = c
            cache_size = 100.0
            eps = 0.001
            probability = 1
        }

        return svm.svm_train(prob, param)
    }

    private fun evaluateModel(model: svm_model, samples: List<Pair<List<Double>, String>>): Double {
        var correct = 0

        samples.forEach { (features, label) ->
            val normalizedFeatures = normalizeFeatures(features)

            val nodes = Array(normalizedFeatures.size) { j ->
                svm_node().apply {
                    index = j + 1
                    value = normalizedFeatures[j]
                }
            }

            val prediction = svm.svm_predict(model, nodes)
            val predictedLabel = classLabels[prediction.toInt()]

            if (predictedLabel == label) {
                correct++
            }
        }

        return correct.toDouble() / samples.size
    }

    private fun trainFinalModel(samples: List<Pair<List<Double>, String>>, c: Double, gamma: Double) {
        model = trainWithParams(samples, c, gamma)
    }

    fun predict(features: List<Double>): String {
        val model = this.model ?: return "未知"

        // 歸一化特徵
        val normalizedFeatures = normalizeFeatures(features)

        // 轉換為SVM節點
        val nodes = Array(normalizedFeatures.size) { i ->
            svm_node().apply {
                index = i + 1
                value = normalizedFeatures[i]
            }
        }

        // 進行預測
        val prediction = svm.svm_predict(model, nodes)
        Log.d("SVMOptimized", "class: ${classLabels[prediction.toInt()] ?: "未知"}")
        return classLabels[prediction.toInt()] ?: "未知"
    }

    fun predictMotion(samplesList: List<List<Double>>): String {
        val predictions = samplesList.map { predict(it) }

        // 多數決
        val countMap = mutableMapOf<String, Int>()
        predictions.forEach { prediction ->
            countMap[prediction] = (countMap[prediction] ?: 0) + 1
        }

        return countMap.entries.maxByOrNull { it.value }?.key ?: "未知"
    }

    fun modelTrained():Boolean{
        return model != null
    }
}