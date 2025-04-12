package com.example.humanreactor.AI_Model

import android.util.Log
import com.example.humanreactor.customizedMove.NormalizedSample
import kotlin.random.Random

/**
 * 輕量級決策樹森林分類器
 *
 * 特點:
 * - 比神經網絡更快速，適合手機運行
 * - 比規則基礎分類器更強大，能學習更複雜的模式
 * - 無需大量參數調整，易於實現和優化
 */
class LightweightTreeClassifier(
    private var confidenceThreshold: Float = 0.85f,
    private val numTrees: Int = 20,                // 決策樹數量
    private val maxDepth: Int = 10,                 // 最大樹深度
    private val minSamplesPerLeaf: Int = 3,        // 每葉最小樣本數
    private val featureSamplingRatio: Float = 0.7f // 特徵採樣比例
) {
    companion object {
        private const val TAG = "LightweightTreeClassifier"
        private const val UNKNOWN_ACTION = "unknown"
    }

    // 決策樹森林
    private val forest = mutableListOf<DecisionTree>()

    // 分類標籤
    private var classes = listOf<String>()

    // 特徵重要性
    private val featureImportance = mutableMapOf<Int, Float>()

    // 是否已訓練
    private var isTrained = false

    // 特徵數量
    private var numFeatures = 0

    /**
     * 決策樹節點類型
     */
    sealed class TreeNode {
        // 內部節點（分裂點）
        data class SplitNode(
            val featureIndex: Int,       // 用於分裂的特徵索引
            val threshold: Float,        // 分裂閾值
            val leftChild: TreeNode,     // 左子樹 (≤ threshold)
            val rightChild: TreeNode,    // 右子樹 (> threshold)
            var importance: Float = 0f   // 此分裂節點的重要性
        ) : TreeNode()

        // 葉節點（預測結果）
        data class LeafNode(
            val classProbabilities: Map<String, Float> // 各類別的機率
        ) : TreeNode()
    }

    /**
     * 決策樹類
     */
    inner class DecisionTree {
        var rootNode: TreeNode = TreeNode.LeafNode(mapOf())
        private val usedFeatureIndices = mutableSetOf<Int>()

        /**
         * 建立決策樹
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

            // 判斷停止條件
            if (currentDepth >= maxDepth ||
                samples.size <= minSamplesPerLeaf ||
                classCounts.size <= 1 ||
                availableFeatures.isEmpty()) {
                return TreeNode.LeafNode(classProbabilities)
            }

            // 找最佳分裂點
            val bestSplit = findBestSplit(samples, availableFeatures)

            // 如果找不到好的分裂點，返回葉節點
            if (bestSplit.gain <= 0.001f) {
                return TreeNode.LeafNode(classProbabilities)
            }

            // 根據最佳分裂點分割數據
            val (leftSamples, rightSamples) = samples.partition {
                    sample -> sample.features.getOrElse(bestSplit.featureIndex) { 0f } <= bestSplit.threshold
            }

            // 記錄已使用的特徵
            usedFeatureIndices.add(bestSplit.featureIndex)

            // 創建分裂節點並遞迴建立子樹
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
        fun predict(features: List<Float>): Map<String, Float> {
            // 從根節點開始遍歷
            var currentNode = rootNode

            // 遍歷直到到達葉節點
            while (currentNode is TreeNode.SplitNode) {
                val splitNode = currentNode
                val featureIndex = splitNode.featureIndex
                val featureValue = if (featureIndex < features.size) features[featureIndex] else 0f

                // 根據特徵值選擇子樹
                currentNode = if (featureValue <= splitNode.threshold) {
                    splitNode.leftChild
                } else {
                    splitNode.rightChild
                }
            }

            // 返回葉節點的類別機率
            return (currentNode as TreeNode.LeafNode).classProbabilities
        }

        /**
         * 獲取決策樹使用的特徵索引
         */
        fun getUsedFeatures(): Set<Int> {
            return usedFeatureIndices
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

            // 遍歷樹的所有節點計算重要性
            calculateNodeImportance(rootNode, importance)

            return importance
        }

        /**
         * 遞迴計算節點重要性
         */
        private fun calculateNodeImportance(
            node: TreeNode,
            importance: MutableMap<Int, Float>
        ) {
            when (node) {
                is TreeNode.SplitNode -> {
                    // 累加分裂節點重要性
                    val featureIndex = node.featureIndex
                    importance[featureIndex] = (importance[featureIndex] ?: 0f) + node.importance

                    // 遞迴處理子節點
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
     * 分裂點評估結果
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
            Log.d(TAG, "開始訓練輕量級決策樹分類器, 樣本數: ${samples.size}")

            // 檢查樣本
            if (samples.isEmpty()) {
                Log.e(TAG, "沒有訓練樣本")
                return false
            }

            // 初始化
            forest.clear()
            featureImportance.clear()

            // 獲取類別標籤和特徵數量
            classes = samples.map { it.label }.distinct().filterNot { it == UNKNOWN_ACTION }

            if (samples.firstOrNull()?.features.isNullOrEmpty()) {
                Log.e(TAG, "樣本特徵為空")
                return false
            }

            numFeatures = samples.maxOf { it.features.size }

            Log.d(TAG, "特徵數量: $numFeatures, 類別數量: ${classes.size}")

            // 隨機森林訓練 (每棵樹使用數據的隨機子集)
            for (i in 0 until numTrees) {
                // 隨機選擇訓練樣本 (Bootstrap Sampling)
                val bootstrapSamples = bootstrapSampling(samples)

                // 隨機選擇特徵子集
                val featureIndices = randomFeatureSubset(numFeatures)

                // 建立決策樹
                val tree = DecisionTree()
                tree.rootNode = tree.build(bootstrapSamples, featureIndices)

                // 將樹加入森林
                forest.add(tree)

                Log.d(TAG, "訓練完成決策樹 ${i+1}/$numTrees")
            }

            // 計算特徵重要性 (所有樹的平均)
            calculateOverallFeatureImportance()

            isTrained = true
            Log.d(TAG, "輕量級決策樹分類器訓練完成")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "訓練過程發生錯誤", e)
            return false
        }
    }

    /**
     * 計算整體特徵重要性
     */
    private fun calculateOverallFeatureImportance() {
        // 初始化
        for (i in 0 until numFeatures) {
            featureImportance[i] = 0f
        }

        // 累加所有樹的特徵重要性
        forest.forEach { tree ->
            val treeImportance = tree.calculateFeatureImportance(numFeatures)
            treeImportance.forEach { (featureIndex, importance) ->
                featureImportance[featureIndex] = (featureImportance[featureIndex] ?: 0f) + importance
            }
        }

        // 平均化特徵重要性
        if (forest.isNotEmpty()) {
            featureImportance.keys.forEach { key ->
                featureImportance[key] = featureImportance[key]!! / forest.size
            }
        }

        // 標準化到 [0, 1] 範圍
        val maxImportance = featureImportance.values.maxOrNull() ?: 1f
        if (maxImportance > 0) {
            featureImportance.keys.forEach { key ->
                featureImportance[key] = featureImportance[key]!! / maxImportance
            }
        }

        Log.d(TAG, "最重要的5個特徵: ${
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

        // 生成所有特徵的索引列表並隨機打亂
        return (0 until numFeatures).shuffled().take(numFeaturesToSelect)
    }

    /**
     * 使用自助法 (Bootstrap) 採樣
     */
    private fun bootstrapSampling(samples: List<NormalizedSample>): List<NormalizedSample> {
        val result = mutableListOf<NormalizedSample>()
        val numSamples = samples.size

        // 隨機選擇與原樣本集相同大小的樣本 (有放回抽樣)
        repeat(numSamples) {
            val index = Random.nextInt(numSamples)
            result.add(samples[index])
        }

        return result
    }

    /**
     * 找到最佳分裂點
     */
    private fun findBestSplit(
        samples: List<NormalizedSample>,
        availableFeatures: List<Int>
    ): SplitEvaluation {
        // 初始化最佳分裂評估結果
        var bestSplit = SplitEvaluation(0, 0f, 0f)

        // 當前節點的熵 (不純度)
        val currentEntropy = calculateEntropy(samples)

        // 評估每個可用特徵的分裂點
        for (featureIndex in availableFeatures) {
            // 獲取該特徵的所有值
            val featureValues = samples.mapNotNull {
                if (featureIndex < it.features.size) it.features[featureIndex] else null
            }

            if (featureValues.isEmpty()) continue

            // 選擇一些候選分裂閾值
            val uniqueValues = featureValues.distinct().sorted()
            val candidateThresholds = mutableListOf<Float>()

            // 如果唯一值太多，則選取等距分隔的點
            if (uniqueValues.size > 10) {
                val step = uniqueValues.size / 10
                for (i in 0 until uniqueValues.size step step) {
                    candidateThresholds.add(uniqueValues[i])
                }
            } else {
                candidateThresholds.addAll(uniqueValues)
            }

            // 評估每個候選閾值
            for (threshold in candidateThresholds) {
                // 根據閾值分割樣本
                val (leftSamples, rightSamples) = samples.partition {
                        sample ->
                    val value = if (featureIndex < sample.features.size) sample.features[featureIndex] else 0f
                    value <= threshold
                }

                // 如果分割不平衡，則跳過
                if (leftSamples.isEmpty() || rightSamples.isEmpty()) continue

                // 計算左右子節點的熵
                val leftEntropy = calculateEntropy(leftSamples)
                val rightEntropy = calculateEntropy(rightSamples)

                // 計算熵增益
                val leftWeight = leftSamples.size.toFloat() / samples.size
                val rightWeight = rightSamples.size.toFloat() / samples.size
                val weightedEntropy = leftWeight * leftEntropy + rightWeight * rightEntropy
                val gain = currentEntropy - weightedEntropy

                // 更新最佳分裂點
                if (gain > bestSplit.gain) {
                    bestSplit = SplitEvaluation(featureIndex, threshold, gain)
                }
            }
        }

        return bestSplit
    }

    /**
     * 計算熵 (不純度度量)
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
                entropy -= p * kotlin.math.ln(p)
            }
        }
        return entropy.toFloat()
    }

    /**
     * 預測動作
     */
    fun predict(features: List<Float>): Pair<String, Float> {
        if (!isTrained || forest.isEmpty()) {
            return Pair(UNKNOWN_ACTION, 0f)
        }

        // 獲取森林中所有樹的預測
        val predictions = forest.map { tree -> tree.predict(features) }

        // 合併所有樹的預測結果
        val combinedPrediction = mutableMapOf<String, Float>()

        // 初始化所有類別的機率為0
        classes.forEach { className ->
            combinedPrediction[className] = 0f
        }

        // 累加所有樹的機率
        predictions.forEach { prediction ->
            prediction.forEach { (className, probability) ->
                combinedPrediction[className] = (combinedPrediction[className] ?: 0f) + probability
            }
        }

        // 平均化機率
        combinedPrediction.keys.forEach { className ->
            combinedPrediction[className] = combinedPrediction[className]!! / forest.size
        }

        // 找出機率最高的類別
        val bestPrediction = combinedPrediction.maxByOrNull { it.value }
            ?: return Pair(UNKNOWN_ACTION, 0f)

        // 檢查置信度是否超過閾值
        return if (bestPrediction.value >= confidenceThreshold) {
            Pair(bestPrediction.key, bestPrediction.value)
        } else {
            Pair(UNKNOWN_ACTION, bestPrediction.value)
        }
    }

    /**
     * 使用滑動窗口進行穩定預測
     */
    fun predictWithWindow(
        features: List<Float>,
        previousPredictions: List<Pair<String, Float>>,
        requiredConsensus: Int = 2
    ): Pair<String, Float> {
        // 當前預測
        val currentPrediction = predict(features)

        // 結合之前的預測
        val allPredictions = previousPredictions + currentPrediction

        // 僅考慮非未知且置信度足夠高的預測
        val validPredictions = allPredictions
            .filter { it.first != UNKNOWN_ACTION && it.second >= confidenceThreshold * 0.95f }

        // 計算每個預測的出現次數
        val predictionCounts = validPredictions
            .groupBy { it.first }
            .mapValues { it.value.size }

        // 找出最常見的預測
        val mostCommon = predictionCounts.maxByOrNull { it.value }

        // 檢查是否達到共識閾值
        return if (mostCommon != null && mostCommon.value >= requiredConsensus) {
            // 計算此預測的平均置信度
            val avgConfidence = validPredictions
                .filter { it.first == mostCommon.key }
                .map { it.second }
                .average()
                .toFloat()

            // 添加穩定性獎勵
            val stabilityBonus = minOf(0.1f, 0.02f * mostCommon.value)
            val finalConfidence = minOf(1.0f, avgConfidence + stabilityBonus)

            Pair(mostCommon.key, finalConfidence)
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
     * 獲取置信度閾值
     */
    fun getConfidenceThreshold(): Float {
        return confidenceThreshold
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
        info["numTrees"] = forest.size
        info["maxDepth"] = maxDepth
        info["confidenceThreshold"] = confidenceThreshold
        info["numFeatures"] = numFeatures
        info["numImportantFeatures"] = featureImportance.count { it.value > 0.5f }

        return info
    }
}