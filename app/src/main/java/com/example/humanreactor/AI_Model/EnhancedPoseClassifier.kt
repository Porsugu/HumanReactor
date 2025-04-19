package com.example.humanreactor.AI_Model

import android.util.Log
import com.example.humanreactor.customizedMove.NormalizedSample
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Enhanced Pose Classifier
 *
 * 結合改進的隨機森林與梯度提升決策樹，同時加入專門處理人體姿勢的特殊邏輯
 *
 * 優勢:
 * - 比神經網絡計算快速，適合移動設備執行
 * - 比標準決策樹精確度更高，能捕捉到複雜模式
 * - 增加了處理靜止狀態和異常姿勢的特殊機制
 * - 自適應信任度評分，減少誤判
 */
class EnhancedPoseClassifier(
    private var confidenceThreshold: Float = 0.82f,
    private val numRfTrees: Int = 15,               // 隨機森林樹的數量
    private val numGbTrees: Int = 12,               // 梯度提升樹的數量
    private val maxDepth: Int = 8,                  // 最大樹深度
    private val minSamplesPerLeaf: Int = 4,         // 每個葉節點的最小樣本數
    private val featureSamplingRatio: Float = 0.8f, // 特徵抽樣比例
    private val learningRate: Float = 0.08f,        // GBDT學習率
    private val stabilityThreshold: Float = 0.4f    // 穩定性閾值
) {
    companion object {
        private const val TAG = "EnhancedPoseClassifier"
        private const val UNKNOWN_ACTION = "unknown"
        private const val IDLE_ACTION = "idle"      // 專門用於表示靜止狀態
    }

    // 森林集合
    private val randomForest = mutableListOf<DecisionTree>()
    private val gradientBoostedTrees = mutableListOf<GBDTTree>()

    // 分類標籤
    private var classes = listOf<String>()

    // 每個類別的標準偏差（用於異常檢測）
    private val classStdDeviation = mutableMapOf<String, MutableList<Float>>()

    // 特徵重要性
    private val featureImportance = mutableMapOf<Int, Float>()

    // 訓練狀態
    private var isTrained = false

    // 特徵數量
    private var numFeatures = 0

    // 靜止動作檢測器
    private val idleDetector = IdleStateDetector()

    // 中央化資訊
    private val classCentroids = mutableMapOf<String, List<Float>>()

    // 每個特徵在各個類別的變異係數
    private val featureVariability = mutableMapOf<Int, Float>()

    /**
     * 決策樹節點類型
     */
    sealed class TreeNode {
        // 內部節點（分割點）
        data class SplitNode(
            val featureIndex: Int,       // 用於分割的特徵索引
            val threshold: Float,        // 分割閾值
            val leftChild: TreeNode,     // 左子樹 (≤ 閾值)
            val rightChild: TreeNode,    // 右子樹 (> 閾值)
            var importance: Float = 0f   // 該分割節點的重要性
        ) : TreeNode()

        // 葉節點（預測結果）
        data class LeafNode(
            val classProbabilities: Map<String, Float>, // 每個類別的概率
            val sampleVariance: Float = 0f              // 樣本方差，用於異常檢測
        ) : TreeNode()
    }

    /**
     * 基礎決策樹類
     */
    inner class DecisionTree {
        var rootNode: TreeNode = TreeNode.LeafNode(mapOf())
        private val usedFeatureIndices = mutableSetOf<Int>()

        /**
         * 建構決策樹
         */
        fun build(
            samples: List<NormalizedSample>,
            availableFeatures: List<Int>,
            currentDepth: Int = 0
        ): TreeNode {
            // 檢查樣本數量和深度條件
            if (samples.isEmpty()) {
                return TreeNode.LeafNode(mapOf())
            }

            // 計算當前節點的類別分佈
            val classCounts = samples.groupBy { it.label }
                .mapValues { it.value.size.toFloat() }
            val totalSamples = samples.size.toFloat()
            val classProbabilities = classCounts.mapValues { it.value / totalSamples }

            // 計算樣本方差（用於異常檢測）
            val sampleVariance = calculateSampleVariance(samples)

            // 檢查停止條件
            if (currentDepth >= maxDepth ||
                samples.size <= minSamplesPerLeaf ||
                classCounts.size <= 1 ||
                availableFeatures.isEmpty()) {
                return TreeNode.LeafNode(classProbabilities, sampleVariance)
            }

            // 尋找最佳分割點
            val bestSplit = findBestSplit(samples, availableFeatures)

            // 如果找不到好的分割點，返回葉節點
            if (bestSplit.gain <= 0.001f) {
                return TreeNode.LeafNode(classProbabilities, sampleVariance)
            }

            // 根據最佳分割點分割數據
            val (leftSamples, rightSamples) = samples.partition {
                    sample ->
                val featureIndex = bestSplit.featureIndex
                if (featureIndex < sample.features.size) {
                    sample.features[featureIndex] <= bestSplit.threshold
                } else {
                    true // 默認到左子樹
                }
            }

            // 記錄使用的特徵
            usedFeatureIndices.add(bestSplit.featureIndex)

            // 創建分割節點並遞歸建立子樹
            return TreeNode.SplitNode(
                featureIndex = bestSplit.featureIndex,
                threshold = bestSplit.threshold,
                importance = bestSplit.gain,
                leftChild = build(leftSamples, availableFeatures, currentDepth + 1),
                rightChild = build(rightSamples, availableFeatures, currentDepth + 1)
            )
        }

        /**
         * 使用決策樹進行預測
         */
        fun predict(features: List<Float>): Pair<Map<String, Float>, Float> {
            // 從根節點開始
            var currentNode = rootNode

            // 追蹤路徑長度（用於可信度計算）
            var pathLength = 0

            // 遍歷直到到達葉節點
            while (currentNode is TreeNode.SplitNode) {
                val splitNode = currentNode
                val featureIndex = splitNode.featureIndex

                val featureValue = if (featureIndex < features.size) {
                    features[featureIndex]
                } else {
                    0f
                }

                // 根據特徵值選擇子樹
                currentNode = if (featureValue <= splitNode.threshold) {
                    splitNode.leftChild
                } else {
                    splitNode.rightChild
                }

                pathLength++
            }

            // 從葉節點返回類別概率與樣本方差
            val leafNode = currentNode as TreeNode.LeafNode
            val pathDepthFactor = (pathLength.toFloat() / maxDepth).coerceIn(0.5f, 1.0f)

            return Pair(leafNode.classProbabilities, leafNode.sampleVariance)
        }

        /**
         * 計算特徵重要性
         */
        fun calculateFeatureImportance(maxFeatureIndex: Int): Map<Int, Float> {
            val importance = mutableMapOf<Int, Float>()

            // 初始化所有特徵重要性為0
            for (i in 0 until maxFeatureIndex) {
                importance[i] = 0f
            }

            // 遍歷所有節點計算重要性
            calculateNodeImportance(rootNode, importance)

            return importance
        }

        /**
         * 遞歸計算節點重要性
         */
        private fun calculateNodeImportance(
            node: TreeNode,
            importance: MutableMap<Int, Float>
        ) {
            when (node) {
                is TreeNode.SplitNode -> {
                    // 累積分割節點重要性
                    val featureIndex = node.featureIndex
                    importance[featureIndex] = (importance[featureIndex] ?: 0f) + node.importance

                    // 遞歸處理子節點
                    calculateNodeImportance(node.leftChild, importance)
                    calculateNodeImportance(node.rightChild, importance)
                }
                is TreeNode.LeafNode -> {
                    // 葉節點不計算重要性
                }
            }
        }
    }

    /**
     * 梯度提升決策樹
     */
    inner class GBDTTree {
        var rootNode: TreeNode = TreeNode.LeafNode(mapOf())

        /**
         * 以殘差為目標建構GBDT樹
         */
        fun buildRegressionTree(
            samples: List<Pair<NormalizedSample, Map<String, Float>>>,
            availableFeatures: List<Int>,
            currentDepth: Int = 0
        ): TreeNode {
            // 檢查樣本數量和深度條件
            if (samples.isEmpty()) {
                return TreeNode.LeafNode(mapOf())
            }

            // 計算當前節點的平均殘差
            val residualsByClass = mutableMapOf<String, Float>()
            val countByClass = mutableMapOf<String, Int>()

            samples.forEach { (sample, residuals) ->
                residuals.forEach { (className, residual) ->
                    residualsByClass[className] = (residualsByClass[className] ?: 0f) + residual
                    countByClass[className] = (countByClass[className] ?: 0) + 1
                }
            }

            // 計算平均殘差
            val avgResiduals = residualsByClass.mapValues { (className, sum) ->
                sum / (countByClass[className] ?: 1).toFloat()
            }

            // 計算樣本方差
            val sampleVariance = calculatePairSampleVariance(samples)

            // 檢查停止條件
            if (currentDepth >= maxDepth / 2 || // GBDT樹通常較淺
                samples.size <= minSamplesPerLeaf + 2 ||
                availableFeatures.isEmpty()) {
                return TreeNode.LeafNode(avgResiduals, sampleVariance)
            }

            // 尋找最佳分割點
            val bestSplit = findBestGBDTSplit(samples, availableFeatures)

            // 如果找不到好的分割點，返回葉節點
            if (bestSplit.gain <= 0.0005f) {
                return TreeNode.LeafNode(avgResiduals, sampleVariance)
            }

            // 根據最佳分割點分割數據
            val (leftSamples, rightSamples) = samples.partition { (sample, _) ->
                val featureIndex = bestSplit.featureIndex
                if (featureIndex < sample.features.size) {
                    sample.features[featureIndex] <= bestSplit.threshold
                } else {
                    true
                }
            }

            // 如果分割過於不平衡，返回葉節點
            if (leftSamples.isEmpty() || rightSamples.isEmpty() ||
                leftSamples.size < samples.size * 0.1 || rightSamples.size < samples.size * 0.1) {
                return TreeNode.LeafNode(avgResiduals, sampleVariance)
            }

            // 創建分割節點並遞歸建立子樹
            return TreeNode.SplitNode(
                featureIndex = bestSplit.featureIndex,
                threshold = bestSplit.threshold,
                importance = bestSplit.gain,
                leftChild = buildRegressionTree(leftSamples, availableFeatures, currentDepth + 1),
                rightChild = buildRegressionTree(rightSamples, availableFeatures, currentDepth + 1)
            )
        }

        /**
         * 使用GBDT樹進行預測
         */
        fun predict(features: List<Float>): Map<String, Float> {
            // 從根節點開始
            var currentNode = rootNode

            // 遍歷直到到達葉節點
            while (currentNode is TreeNode.SplitNode) {
                val splitNode = currentNode
                val featureIndex = splitNode.featureIndex

                val featureValue = if (featureIndex < features.size) {
                    features[featureIndex]
                } else {
                    0f
                }

                // 根據特徵值選擇子樹
                currentNode = if (featureValue <= splitNode.threshold) {
                    splitNode.leftChild
                } else {
                    splitNode.rightChild
                }
            }

            // 從葉節點返回殘差預測
            val leafNode = currentNode as TreeNode.LeafNode
            return leafNode.classProbabilities
        }
    }

    /**
     * 靜止狀態檢測器
     */
    inner class IdleStateDetector {
        private val featureStabilityWindow = mutableListOf<List<Float>>()
        private val windowSize = 5
        private val movementThreshold = 0.015f // 調整此閾值以適應不同級別的"靜止"

        /**
         * 檢測是否為靜止狀態
         */
        fun detectIdleState(features: List<Float>): Pair<Boolean, Float> {
            // 更新窗口
            if (featureStabilityWindow.size >= windowSize) {
                featureStabilityWindow.removeAt(0)
            }
            featureStabilityWindow.add(features)

            // 如果窗口還沒填滿，就不判斷為靜止
            if (featureStabilityWindow.size < windowSize) {
                return Pair(false, 0f)
            }

            // 計算特徵變化
            val featureChanges = mutableListOf<Float>()

            // 只檢查重要特徵的變化
            val importantFeatures = featureImportance.filter { it.value > 0.3f }.keys

            for (featureIdx in importantFeatures) {
                if (featureIdx >= features.size) continue

                val values = featureStabilityWindow.mapNotNull {
                    if (featureIdx < it.size) it[featureIdx] else null
                }

                if (values.size >= 3) {
                    // 計算標準差作為變化指標
                    val mean = values.average().toFloat()
                    val variance = values.map { (it - mean) * (it - mean) }.average().toFloat()
                    val stdDev = sqrt(variance)

                    featureChanges.add(stdDev)
                }
            }

            // 計算整體變化
            val avgChange = if (featureChanges.isNotEmpty()) {
                featureChanges.average().toFloat()
            } else {
                1.0f
            }

            // 判斷靜止信心度
            val idleConfidence = (1.0f - (avgChange / movementThreshold)).coerceIn(0f, 1f)

            return Pair(avgChange < movementThreshold, idleConfidence)
        }
    }

    /**
     * 分割評估結果
     */
    private data class SplitEvaluation(
        val featureIndex: Int,
        val threshold: Float,
        val gain: Float
    )

    /**
     * 訓練分類器
     */
    fun train(samples: List<NormalizedSample>): Boolean {
        try {
            Log.d(TAG, "開始訓練增強型姿勢分類器，樣本數: ${samples.size}")

            // 檢查樣本
            if (samples.isEmpty()) {
                Log.e(TAG, "沒有訓練樣本")
                return false
            }

            // 初始化
            randomForest.clear()
            gradientBoostedTrees.clear()
            featureImportance.clear()
            classCentroids.clear()
            classStdDeviation.clear()
            featureVariability.clear()

            // 獲取類別標籤和特徵數量
            classes = samples.map { it.label }.distinct().filterNot { it == UNKNOWN_ACTION }

            if (samples.firstOrNull()?.features.isNullOrEmpty()) {
                Log.e(TAG, "樣本特徵為空")
                return false
            }

            numFeatures = samples.maxOf { it.features.size }

            Log.d(TAG, "特徵數量: $numFeatures, 類別數量: ${classes.size}")

            // 預處理：計算每個類別的中心點和標準差
            calculateClassStatistics(samples)

            // 預處理：計算特徵變異性
            calculateFeatureVariability(samples)

            // 第一階段：訓練隨機森林
            Log.d(TAG, "開始訓練隨機森林...")
            trainRandomForest(samples)

            // 第二階段：使用隨機森林預測結果計算殘差，然後訓練GBDT
            Log.d(TAG, "開始訓練梯度提升決策樹...")
            trainGradientBoostedTrees(samples)

            // 計算特徵重要性
            calculateOverallFeatureImportance()

            isTrained = true
            Log.d(TAG, "增強型姿勢分類器訓練完成")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "訓練過程中發生錯誤", e)
            return false
        }
    }

    /**
     * 計算類別統計資訊（中心點和標準差）
     */
    private fun calculateClassStatistics(samples: List<NormalizedSample>) {
        // 按類別分組
        val samplesByClass = samples.groupBy { it.label }

        samplesByClass.forEach { (className, classSamples) ->
            if (classSamples.isEmpty()) return@forEach

            // 初始化
            val featureCount = classSamples.maxOf { it.features.size }
            val sumFeatures = MutableList(featureCount) { 0f }
            val sumSquaredFeatures = MutableList(featureCount) { 0f }
            val stdDevList = MutableList(featureCount) { 0f }

            // 計算總和與平方和
            for (sample in classSamples) {
                for (i in sample.features.indices) {
                    val value = sample.features[i]
                    sumFeatures[i] += value
                    sumSquaredFeatures[i] += value * value
                }
            }

            // 計算中心點（平均值）
            val centroid = sumFeatures.map { it / classSamples.size }
            classCentroids[className] = centroid

            // 計算標準差
            for (i in 0 until featureCount) {
                val mean = sumFeatures[i] / classSamples.size
                val variance = (sumSquaredFeatures[i] / classSamples.size) - (mean * mean)
                stdDevList[i] = sqrt(max(0f, variance))
            }

            classStdDeviation[className] = stdDevList
        }
    }

    /**
     * 計算特徵變異性（用於判斷特徵的穩定性）
     */
    private fun calculateFeatureVariability(samples: List<NormalizedSample>) {
        if (samples.isEmpty()) return

        val featureCount = samples.maxOf { it.features.size }

        for (featureIdx in 0 until featureCount) {
            // 計算每個類別中該特徵的平均值
            val classMeans = mutableMapOf<String, Float>()
            val classCounts = mutableMapOf<String, Int>()

            samples.forEach { sample ->
                if (featureIdx < sample.features.size) {
                    val className = sample.label
                    val value = sample.features[featureIdx]

                    classMeans[className] = (classMeans[className] ?: 0f) + value
                    classCounts[className] = (classCounts[className] ?: 0) + 1
                }
            }

            // 計算平均值
            classMeans.keys.forEach { className ->
                classMeans[className] = classMeans[className]!! / classCounts[className]!!
            }

            // 計算類別間的方差
            val values = classMeans.values.toList()
            if (values.size >= 2) {
                val mean = values.average().toFloat()
                val variance = values.map { (it - mean) * (it - mean) }.average().toFloat()

                // 變異係數 = 標準差 / 平均值
                // 高變異係數表示該特徵在不同類別間差異大，可能是有用的特徵
                val cv = if (mean != 0f) sqrt(variance) / abs(mean) else 0f

                featureVariability[featureIdx] = cv
            } else {
                featureVariability[featureIdx] = 0f
            }
        }
    }

    /**
     * 訓練隨機森林
     */
    private fun trainRandomForest(samples: List<NormalizedSample>) {
        // 隨機森林訓練（每棵樹使用隨機資料子集）
        for (i in 0 until numRfTrees) {
            // 隨機選擇訓練樣本（Bootstrap Sampling）
            val bootstrapSamples = bootstrapSampling(samples)

            // 隨機選擇特徵子集
            val featureIndices = randomFeatureSubset(numFeatures)

            // 建構決策樹
            val tree = DecisionTree()
            tree.rootNode = tree.build(bootstrapSamples, featureIndices)

            // 添加樹到森林
            randomForest.add(tree)

            Log.d(TAG, "完成訓練隨機森林決策樹 ${i+1}/$numRfTrees")
        }
    }

    /**
     * 訓練梯度提升決策樹
     */
    private fun trainGradientBoostedTrees(samples: List<NormalizedSample>) {
        // 初始預測（每個類別的基准概率）
        val classPriors = mutableMapOf<String, Float>()
        val classCounts = samples.groupBy { it.label }.mapValues { it.value.size }
        val totalSamples = samples.size.toFloat()

        classes.forEach { className ->
            val count = classCounts[className] ?: 0
            // 先驗概率，做log轉換
            classPriors[className] = ln((count + 1).toFloat() / (totalSamples + classes.size))
        }

        // 當前預測（初始為先驗概率）
        val currentPredictions = samples.associate { sample ->
            sample to classPriors.toMutableMap()
        }.toMutableMap()

        // 訓練每棵GBDT樹
        for (i in 0 until numGbTrees) {
            // 計算殘差
            val residuals = samples.map { sample ->
                // 獲取當前預測
                val currentPred = currentPredictions[sample] ?: classPriors.toMutableMap()

                // 計算殘差：對於正確標籤殘差為1-p，其他為0-p
                val residualMap = classes.associateWith { className ->
                    val targetProb = if (className == sample.label) 1f else 0f
                    val currentProb = exp(currentPred[className] ?: 0f)
                    targetProb - currentProb
                }

                sample to residualMap
            }

            // 隨機選擇特徵子集
            val featureIndices = randomFeatureSubset(numFeatures)

            // 建構GBDT樹
            val tree = GBDTTree()
            tree.rootNode = tree.buildRegressionTree(residuals, featureIndices)

            // 添加樹到GBDT森林
            gradientBoostedTrees.add(tree)

            // 更新當前預測
            samples.forEach { sample ->
                val prediction = tree.predict(sample.features)
                val currentPred = currentPredictions[sample] ?: classPriors.toMutableMap()

                // 更新預測，加入學習率因子
                prediction.forEach { (className, update) ->
                    currentPred[className] = (currentPred[className] ?: 0f) + learningRate * update
                }

                currentPredictions[sample] = currentPred
            }

            Log.d(TAG, "完成訓練梯度提升決策樹 ${i+1}/$numGbTrees")
        }
    }

    /**
     * 計算樣本方差
     */
    private fun calculateSampleVariance(samples: List<NormalizedSample>): Float {
        if (samples.isEmpty() || samples.size <= 1) return 0f

        // 只計算重要特徵的方差
        val importantFeatures = featureVariability.filter { it.value > 0.5f }.keys.toList()

        if (importantFeatures.isEmpty()) return 0f

        var totalVariance = 0f
        var featureCount = 0

        for (featureIdx in importantFeatures) {
            val values = samples.mapNotNull {
                if (featureIdx < it.features.size) it.features[featureIdx] else null
            }

            if (values.size >= 2) {
                val mean = values.average().toFloat()
                val variance = values.map { (it - mean) * (it - mean) }.average().toFloat()

                totalVariance += variance
                featureCount++
            }
        }

        return if (featureCount > 0) totalVariance / featureCount else 0f
    }

    /**
     * 計算配對樣本方差
     */
    private fun calculatePairSampleVariance(samples: List<Pair<NormalizedSample, Map<String, Float>>>): Float {
        if (samples.isEmpty() || samples.size <= 1) return 0f

        val unwrappedSamples = samples.map { it.first }
        return calculateSampleVariance(unwrappedSamples)
    }

    /**
     * 計算整體特徵重要性
     */
    private fun calculateOverallFeatureImportance() {
        // 初始化
        for (i in 0 until numFeatures) {
            featureImportance[i] = 0f
        }

        // 從隨機森林累積特徵重要性
        randomForest.forEach { tree ->
            val treeImportance = tree.calculateFeatureImportance(numFeatures)
            treeImportance.forEach { (featureIndex, importance) ->
                featureImportance[featureIndex] = (featureImportance[featureIndex] ?: 0f) + importance
            }
        }

        // 從GBDT森林獲取特徵重要性信息
        var addedImportance = 0f
        gradientBoostedTrees.forEach { tree ->
            fun traverseTree(node: TreeNode) {
                when (node) {
                    is TreeNode.SplitNode -> {
                        val featureIndex = node.featureIndex
                        // GBDT的特徵重要性獲得更高的權重
                        val scaledImportance = node.importance * 1.5f

                        featureImportance[featureIndex] = (featureImportance[featureIndex] ?: 0f) + scaledImportance
                        addedImportance += scaledImportance

                        traverseTree(node.leftChild)
                        traverseTree(node.rightChild)
                    }
                    is TreeNode.LeafNode -> {
                        // 葉節點不處理
                    }
                }
            }

            traverseTree(tree.rootNode)
        }

        // 考慮特徵變異性
        featureVariability.forEach { (featureIndex, variability) ->
            featureImportance[featureIndex] = (featureImportance[featureIndex] ?: 0f) * (1f + variability)
        }

        // 平均並標準化特徵重要性
        val totalTrees = numRfTrees + numGbTrees
        if (totalTrees > 0) {
            featureImportance.keys.forEach { key ->
                featureImportance[key] = featureImportance[key]!! / totalTrees
            }
        }

        // 標準化到[0, 1]範圍
        val maxImportance = featureImportance.values.maxOrNull() ?: 1f
        if (maxImportance > 0) {
            featureImportance.keys.forEach { key ->
                featureImportance[key] = featureImportance[key]!! / maxImportance
            }
        }

        Log.d(TAG, "前5個最重要特徵: ${
            featureImportance.entries.sortedByDescending { it.value }.take(5)
                .joinToString(", ") { "${it.key}: ${it.value}" }
        }")
    }

    /**
     * 隨機選擇特徵子集
     */
    private fun randomFeatureSubset(numFeatures: Int): List<Int> {
        // 確定要選擇的特徵數量
        val numFeaturesToSelect = (numFeatures * featureSamplingRatio).toInt().coerceAtLeast(1)

        // 優先選擇重要特徵
        val sortedFeatures = if (featureImportance.isNotEmpty()) {
            featureImportance.entries.sortedByDescending { it.value }.map { it.key }
        } else {
            listOf()
        }

        // 如果已有特徵重要性信息，優先選擇重要特徵
        if (sortedFeatures.isNotEmpty()) {
            // 選擇部分重要特徵和部分隨機特徵
            val importantCount = (numFeaturesToSelect * 0.7).toInt().coerceAtLeast(1)
            val randomCount = numFeaturesToSelect - importantCount

            val importantFeatures = sortedFeatures.take(importantCount)
            val otherFeatures = (0 until numFeatures).filter { it !in importantFeatures }.shuffled().take(randomCount)

            return (importantFeatures + otherFeatures).distinct()
        }

        // 如果沒有重要性信息，完全隨機選擇
        return (0 until numFeatures).shuffled().take(numFeaturesToSelect)
    }

    /**
     * Bootstrap抽樣
     */
    private fun bootstrapSampling(samples: List<NormalizedSample>): List<NormalizedSample> {
        val result = mutableListOf<NormalizedSample>()
        val numSamples = samples.size

        // 通過重複抽樣隨機選擇樣本
        repeat(numSamples) {
            val index = Random.nextInt(numSamples)
            result.add(samples[index])
        }

        return result
    }

    /**
     * 尋找最佳分割點
     */
    private fun findBestSplit(
        samples: List<NormalizedSample>,
        availableFeatures: List<Int>
    ): SplitEvaluation {
        // 初始化最佳分割評估結果
        var bestSplit = SplitEvaluation(0, 0f, 0f)

        // 當前節點熵（不純度）
        val currentEntropy = calculateEntropy(samples)

        // 評估每個可用特徵的分割點
        for (featureIndex in availableFeatures) {
            // 獲取該特徵的所有值
            val featureValues = samples.mapNotNull {
                if (featureIndex < it.features.size) it.features[featureIndex] else null
            }

            if (featureValues.isEmpty()) continue

            // 選擇一些候選分割閾值
            val uniqueValues = featureValues.distinct().sorted()
            val candidateThresholds = mutableListOf<Float>()

            // 如果唯一值太多，選擇均勻分布的點
            if (uniqueValues.size > 12) {
                val step = uniqueValues.size / 12
                for (i in 0 until uniqueValues.size step step) {
                    candidateThresholds.add(uniqueValues[i])
                }
            } else {
                candidateThresholds.addAll(uniqueValues)
            }

            // 如果特徵被評為重要，測試更多分割點
            if (featureImportance[featureIndex] ?: 0f > 0.4f) {
                // 添加更多分割點以獲得更精確的結果
                if (uniqueValues.size > 3) {
                    // 添加相鄰值的中點
                    for (i in 0 until uniqueValues.size - 1) {
                        val midPoint = (uniqueValues[i] + uniqueValues[i + 1]) / 2
                        if (midPoint !in candidateThresholds) {
                            candidateThresholds.add(midPoint)
                        }
                    }
                }
            }

            // 評估每個候選閾值
            for (threshold in candidateThresholds) {
                // 根據閾值分割樣本
                val (leftSamples, rightSamples) = samples.partition { sample ->
                    val value = if (featureIndex < sample.features.size) sample.features[featureIndex] else 0f
                    value <= threshold
                }

                // 如果分割不平衡則跳過
                if (leftSamples.isEmpty() || rightSamples.isEmpty() ||
                    leftSamples.size < samples.size * 0.05 || rightSamples.size < samples.size * 0.05) {
                    continue
                }

                // 計算左右子節點的熵
                val leftEntropy = calculateEntropy(leftSamples)
                val rightEntropy = calculateEntropy(rightSamples)

                // 計算信息增益
                val leftWeight = leftSamples.size.toFloat() / samples.size
                val rightWeight = rightSamples.size.toFloat() / samples.size
                val weightedEntropy = leftWeight * leftEntropy + rightWeight * rightEntropy
                val gain = currentEntropy - weightedEntropy

                // 更新最佳分割點
                if (gain > bestSplit.gain) {
                    bestSplit = SplitEvaluation(featureIndex, threshold, gain)
                }
            }
        }

        return bestSplit
    }

    /**
     * 尋找GBDT的最佳分割點
     */
    private fun findBestGBDTSplit(
        samples: List<Pair<NormalizedSample, Map<String, Float>>>,
        availableFeatures: List<Int>
    ): SplitEvaluation {
        // 初始化最佳分割評估結果
        var bestSplit = SplitEvaluation(0, 0f, 0f)

        // 計算當前殘差均方誤差 (MSE)
        val currentMSE = calculateMSE(samples)

        // 評估每個可用特徵的分割點
        for (featureIndex in availableFeatures) {
            // 獲取該特徵的所有值
            val featureValues = samples.mapNotNull { (sample, _) ->
                if (featureIndex < sample.features.size) sample.features[featureIndex] else null
            }

            if (featureValues.isEmpty()) continue

            // 選擇一些候選分割閾值
            val uniqueValues = featureValues.distinct().sorted()
            val candidateThresholds = mutableListOf<Float>()

            // 如果唯一值太多，選擇均勻分布的點
            if (uniqueValues.size > 8) {
                val step = uniqueValues.size / 8
                for (i in 0 until uniqueValues.size step step) {
                    candidateThresholds.add(uniqueValues[i])
                }
            } else {
                candidateThresholds.addAll(uniqueValues)
            }

            // 評估每個候選閾值
            for (threshold in candidateThresholds) {
                // 根據閾值分割樣本
                val (leftSamples, rightSamples) = samples.partition { (sample, _) ->
                    val value = if (featureIndex < sample.features.size) sample.features[featureIndex] else 0f
                    value <= threshold
                }

                // 如果分割不平衡則跳過
                if (leftSamples.isEmpty() || rightSamples.isEmpty()) {
                    continue
                }

                // 計算左右子節點的MSE
                val leftMSE = calculateMSE(leftSamples)
                val rightMSE = calculateMSE(rightSamples)

                // 計算MSE減少量
                val leftWeight = leftSamples.size.toFloat() / samples.size
                val rightWeight = rightSamples.size.toFloat() / samples.size
                val weightedMSE = leftWeight * leftMSE + rightWeight * rightMSE
                val gain = currentMSE - weightedMSE

                // 更新最佳分割點
                if (gain > bestSplit.gain) {
                    bestSplit = SplitEvaluation(featureIndex, threshold, gain)
                }
            }
        }

        return bestSplit
    }

    /**
     * 計算均方誤差
     */
    private fun calculateMSE(samples: List<Pair<NormalizedSample, Map<String, Float>>>): Float {
        if (samples.isEmpty()) return 0f

        var totalMSE = 0f
        var count = 0

        // 計算每個類別的MSE
        for (className in classes) {
            var classMSE = 0f
            var classCount = 0

            samples.forEach { (_, residuals) ->
                val residual = residuals[className] ?: 0f
                classMSE += residual * residual
                classCount++
            }

            if (classCount > 0) {
                totalMSE += classMSE / classCount
                count++
            }
        }

        return if (count > 0) totalMSE / count else 0f
    }

    /**
     * 計算熵（不純度度量）
     */
    private fun calculateEntropy(samples: List<NormalizedSample>): Float {
        if (samples.isEmpty()) return 0f

        // 計算每個類別的樣本數
        val classCounts = samples.groupBy { it.label }
            .mapValues { it.value.size.toFloat() / samples.size }

        // 計算熵
        var entropy = 0.0
        for (p in classCounts.values) {
            if (p > 0) {
                entropy -= p * ln(p)
            }
        }
        return entropy.toFloat()
    }

    /**
     * 計算馬氏距離（用於異常檢測）
     */
    private fun calculateMahalanobisDistance(features: List<Float>, className: String): Float {
        val centroid = classCentroids[className] ?: return Float.MAX_VALUE
        val stdDevs = classStdDeviation[className] ?: return Float.MAX_VALUE

        var distance = 0f
        var count = 0

        for (i in 0 until minOf(features.size, centroid.size, stdDevs.size)) {
            val featureValue = features[i]
            val centroidValue = centroid[i]
            val stdDev = stdDevs[i]

            // 避免除零
            if (stdDev > 0.001f) {
                val normalizedDiff = (featureValue - centroidValue) / stdDev
                distance += normalizedDiff * normalizedDiff
                count++
            }
        }

        return if (count > 0) sqrt(distance / count) else Float.MAX_VALUE
    }

    /**
     * 計算異常分數
     */
    private fun calculateAnomalyScore(features: List<Float>, predictedClass: String): Float {
        // 結合馬氏距離和樣本方差
        val mahalanobisDistance = calculateMahalanobisDistance(features, predictedClass)

        // 將距離轉換為0-1之間的分數，較大的距離對應較高的異常分數
        return (1f - 1f / (1f + mahalanobisDistance * 0.5f)).coerceIn(0f, 1f)
    }

    /**
     * 預測動作
     */
    fun predict(features: List<Float>): Pair<String, Float> {
        if (!isTrained || randomForest.isEmpty()) {
            return Pair(UNKNOWN_ACTION, 0f)
        }

        // 檢查是否為靜止狀態
        val (isIdle, idleConfidence) = idleDetector.detectIdleState(features)
        if (isIdle && idleConfidence > stabilityThreshold) {
            return Pair(IDLE_ACTION, idleConfidence)
        }

        // 第一階段：從隨機森林獲取預測
        val rfPredictions = randomForest.map { tree -> tree.predict(features).first }

        // 合併隨機森林預測結果
        val rfCombinedPrediction = mutableMapOf<String, Float>()

        // 初始化所有類別的概率為0
        classes.forEach { className ->
            rfCombinedPrediction[className] = 0f
        }

        // 累積所有樹的概率
        rfPredictions.forEach { prediction ->
            prediction.forEach { (className, probability) ->
                rfCombinedPrediction[className] = (rfCombinedPrediction[className] ?: 0f) + probability
            }
        }

        // 平均概率
        rfCombinedPrediction.keys.forEach { className ->
            rfCombinedPrediction[className] = rfCombinedPrediction[className]!! / randomForest.size
        }

        // 第二階段：從GBDT獲取預測修正
        if (gradientBoostedTrees.isNotEmpty()) {
            val gbPredictions = gradientBoostedTrees.map { tree -> tree.predict(features) }

            // 合併GBDT預測
            val gbCombinedPrediction = mutableMapOf<String, Float>()

            // 初始化
            classes.forEach { className ->
                gbCombinedPrediction[className] = 0f
            }

            // 累積修正值
            gbPredictions.forEach { prediction ->
                prediction.forEach { (className, update) ->
                    gbCombinedPrediction[className] = (gbCombinedPrediction[className] ?: 0f) + update
                }
            }

            // 平均修正值
            gbCombinedPrediction.keys.forEach { className ->
                gbCombinedPrediction[className] = gbCombinedPrediction[className]!! / gradientBoostedTrees.size
            }

            // 應用GBDT修正 (使用softmax將修正後的對數概率轉換回概率)
            val combinedLogits = mutableMapOf<String, Float>()

            for (className in classes) {
                // 隨機森林對數概率 + GBDT修正 * 學習率
                val rfProb = rfCombinedPrediction[className] ?: 0f
                val rfLogit = if (rfProb > 0) ln(rfProb) else -10f
                val gbUpdate = (gbCombinedPrediction[className] ?: 0f) * learningRate

                combinedLogits[className] = rfLogit + gbUpdate
            }

            // Softmax轉換回概率空間
            val maxLogit = combinedLogits.values.maxOrNull() ?: 0f
            var sumExp = 0f

            for (logit in combinedLogits.values) {
                sumExp += exp(logit - maxLogit)
            }

            for (className in combinedLogits.keys) {
                val logit = combinedLogits[className] ?: -10f
                rfCombinedPrediction[className] = exp(logit - maxLogit) / sumExp
            }
        }

        // 找出概率最高的類別
        val bestPrediction = rfCombinedPrediction.maxByOrNull { it.value }
            ?: return Pair(UNKNOWN_ACTION, 0f)

        // 計算異常分數
        val anomalyScore = calculateAnomalyScore(features, bestPrediction.key)

        // 根據異常分數調整信心度
        val adjustedConfidence = bestPrediction.value * (1f - anomalyScore)

        // 檢查信心度是否超過閾值
        return if (adjustedConfidence >= confidenceThreshold) {
            Pair(bestPrediction.key, adjustedConfidence)
        } else {
            Pair(UNKNOWN_ACTION, adjustedConfidence)
        }
    }

    /**
     * 使用滑動窗口進行穩定預測
     */
    fun predictWithWindow(
        features: List<Float>,
        previousPredictions: List<Pair<String, Float>>,
        requiredConsensus: Int = 3
    ): Pair<String, Float> {
        // 當前預測
        val currentPrediction = predict(features)

        // 與前面的預測結合
        val allPredictions = previousPredictions + currentPrediction

        // 只考慮非未知預測且信心度足夠的預測
        val validPredictions = allPredictions
            .filter { it.first != UNKNOWN_ACTION && it.second >= confidenceThreshold * 0.9f }

        // 統計每個預測的出現次數
        val predictionCounts = validPredictions
            .groupBy { it.first }
            .mapValues { it.value.size }

        // 找出最常見的預測
        val mostCommon = predictionCounts.maxByOrNull { it.value }

        // 檢查是否達到共識閾值
        return if (mostCommon != null && mostCommon.value >= requiredConsensus) {
            // 計算這個預測的平均信心度
            val avgConfidence = validPredictions
                .filter { it.first == mostCommon.key }
                .map { it.second }
                .average()
                .toFloat()

            // 添加穩定性獎勵
            val stabilityBonus = minOf(0.15f, 0.03f * mostCommon.value)
            val finalConfidence = minOf(1.0f, avgConfidence + stabilityBonus)

            Pair(mostCommon.key, finalConfidence)
        } else if (validPredictions.isNotEmpty()) {
            // 返回最近的有效預測，但信心度較低
            validPredictions.last().let {
                Pair(it.first, it.second * 0.9f)
            }
        } else {
            Pair(UNKNOWN_ACTION, 0f)
        }
    }

    /**
     * 獲取已訓練的動作列表
     */
    fun getTrainedMoves(): List<String> {
        return classes
    }

    /**
     * 檢查分類器是否已訓練
     */
    fun isTrained(): Boolean {
        return isTrained
    }

    /**
     * 獲取信心度閾值
     */
    fun getConfidenceThreshold(): Float {
        return confidenceThreshold
    }

    /**
     * 設置信心度閾值
     */
    fun setConfidenceThreshold(newThreshold: Float) {
        confidenceThreshold = newThreshold.coerceIn(0.5f, 0.95f)
    }

    /**
     * 獲取特徵重要性列表
     */
    fun getFeatureImportance(): Map<Int, Float> {
        return featureImportance.toMap()
    }

    /**
     * 獲取診斷信息
     */
    fun getDiagnosticInfo(): Map<String, Any> {
        val info = mutableMapOf<String, Any>()

        // 基本信息
        info["numMoves"] = classes.size
        info["numRfTrees"] = randomForest.size
        info["numGbTrees"] = gradientBoostedTrees.size
        info["maxDepth"] = maxDepth
        info["confidenceThreshold"] = confidenceThreshold
        info["numFeatures"] = numFeatures
        info["numImportantFeatures"] = featureImportance.count { it.value > 0.5f }

        // 高級診斷信息
        val mostImportantFeatures = featureImportance.entries.sortedByDescending { it.value }.take(5)
            .associate { it.key to it.value }
        info["topFeatures"] = mostImportantFeatures

        return info
    }
}