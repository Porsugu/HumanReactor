package com.example.humanreactor.AI_Model

import android.util.Log
import com.example.humanreactor.customizedMove.NormalizedSample
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Enhanced 3D Pose Classifier
 *
 * 專為3D姿勢數據設計的分類器，適用於已標準化的姿勢資料
 * 增強對深度(Z軸)資訊的處理，並針對不同軸的重要性進行智能加權
 */
class Enhanced3DPoseClassifier(
    private var confidenceThreshold: Float = 0.85f,
    private val useDynamicThreshold: Boolean = false,
    private val useFeatureSelection: Boolean = true
) {
    companion object {
        private const val TAG = "Enhanced3DPoseClassifier"
        const val UNKNOWN_ACTION = "unknown"

        // 特徵顯著性閾值 - 用於特徵選擇
        private const val FEATURE_SIGNIFICANCE_THRESHOLD = 0.08f

        // 特徵類型標記 - 用於區分特徵類型
        const val FEATURE_TYPE_ANGLE = 0       // 角度特徵
        const val FEATURE_TYPE_DISTANCE = 1    // 距離特徵
        const val FEATURE_TYPE_POSITION = 2    // 位置特徵
        const val FEATURE_TYPE_DEPTH = 3       // Z軸(深度)特徵
    }

    // 動作定義
    private val moveDefinitions = mutableMapOf<String, MoveDefinition>()

    // 特徵重要性分數
    private val featureImportance = mutableMapOf<Int, Float>()

    // 特徵類型標記 (特徵索引 -> 特徵類型)
    private val featureTypes = mutableMapOf<Int, Int>()

    // 追蹤最近的預測歷史記錄
    private val predictionHistory = mutableListOf<PredictionRecord>()

    // 動作轉換檢測器
    private val transitionDetector = TransitionDetector()

    // 選中的特徵集
    private val selectedFeatures = mutableSetOf<Int>()

    /**
     * 動作定義
     */
    data class MoveDefinition(
        val name: String,
        val featureRanges: Map<Int, FeatureRange>,
        // 各類型特徵的相對重要性
        var angleFeatureWeight: Float = 1.0f,
        var distanceFeatureWeight: Float = 1.0f,
        var positionFeatureWeight: Float = 1.0f,
        var depthFeatureWeight: Float = 0.8f
    )

    /**
     * 特徵範圍定義，包含分佈統計資訊
     */
    data class FeatureRange(
        val featureIndex: Int,     // 特徵索引
        val featureType: Int,      // 特徵類型
        val min: Float,            // 最小值
        val max: Float,            // 最大值
        val q1: Float,             // 第一四分位數
        val median: Float,         // 中位數
        val q3: Float,             // 第三四分位數
        val mean: Float,           // 平均值
        val stdDev: Float,         // 標準差
        val weight: Float,         // 特徵權重
        val reliability: Float     // 特徵可靠性(0-1)
    ) {
        /**
         * 檢查值是否在核心範圍內(四分位數範圍)
         */
        fun isInCoreRange(value: Float): Boolean = value in q1..q3

        /**
         * 檢查值是否在可接受範圍內(最小值到最大值)
         */
        fun isInAcceptableRange(value: Float): Boolean = value in min..max

        /**
         * 計算匹配分數(0.0到1.0)
         */
        fun getMatchScore(value: Float): Float {
            // 核心區域(IQR) - 高分
            if (value in q1..q3) {
                return 1.0f
            }

            // 可接受範圍 - 中等分數
            if (value in min..max) {
                return 0.8f
            }

            // 範圍外 - 衰減分數
            val distance = when {
                value < min -> min - value
                else -> value - max
            }

            // 根據特徵類型調整容忍度
            val tolerance = when (featureType) {
                FEATURE_TYPE_ANGLE -> stdDev * 2.0f
                FEATURE_TYPE_DISTANCE -> stdDev * 2.5f
                FEATURE_TYPE_POSITION -> stdDev * 2.0f
                FEATURE_TYPE_DEPTH -> stdDev * 3.0f // Z軸更寬容
                else -> stdDev * 2.0f
            }

            // 計算衰減分數
            return if (distance <= tolerance) {
                // 指數衰減
                0.8f * exp(-distance / tolerance)
            } else {
                0.0f
            }
        }
    }

    /**
     * 預測記錄
     */
    data class PredictionRecord(
        val timestamp: Long,       // 時間戳
        val moveName: String,      // 動作名稱
        val confidence: Float,     // 置信度
        val features: List<Float>  // 特徵向量
    )

    /**
     * 動作轉換檢測器
     */
    inner class TransitionDetector {
        private val featureVelocities = mutableMapOf<Int, Float>()
        private val velocityThresholds = mutableMapOf<Int, Float>()
        private var lastFeatures: List<Float>? = null
        private var lastTimestamp: Long = 0

        /**
         * 更新特徵速度
         */
        fun update(features: List<Float>, timestamp: Long): Boolean {
            if (lastFeatures == null || lastFeatures!!.size != features.size) {
                lastFeatures = features
                lastTimestamp = timestamp
                return false
            }

            val deltaTime = (timestamp - lastTimestamp) / 1000f // 轉換為秒
            if (deltaTime <= 0) return false

            var isTransitioning = false
            var significantChanges = 0

            // 對所選特徵計算速度
            for (index in selectedFeatures) {
                if (index >= features.size || index >= lastFeatures!!.size) continue

                val deltaValue = features[index] - lastFeatures!![index]
                val velocity = deltaValue / deltaTime

                // 更新速度
                featureVelocities[index] = velocity

                // 檢查是否超過閾值
                val threshold = velocityThresholds[index] ?: 2.0f
                if (Math.abs(velocity) > threshold) {
                    significantChanges++
                }
            }

            // 如果有多個特徵變化顯著，判定為轉換狀態
            isTransitioning = significantChanges >= min(3, selectedFeatures.size / 4)

            lastFeatures = features
            lastTimestamp = timestamp

            return isTransitioning
        }

        /**
         * 設置速度閾值
         */
        fun setVelocityThresholds(thresholds: Map<Int, Float>) {
            velocityThresholds.clear()
            velocityThresholds.putAll(thresholds)
        }
    }

    /**
     * 使用標準化樣本訓練分類器
     */
    fun train(samples: List<NormalizedSample>): Boolean {
        try {
            // 清除舊數據
            moveDefinitions.clear()
            featureImportance.clear()
            featureTypes.clear()
            selectedFeatures.clear()

            // 按動作名稱分組
            val samplesByMove = samples.groupBy { it.label }

            // 確保有足夠樣本
            if (samplesByMove.isEmpty()) {
                Log.e(TAG, "No samples provided for training")
                return false
            }

            // 獲取特徵數量
            val numFeatures = samples.firstOrNull()?.features?.size ?: 0
            if (numFeatures == 0) {
                Log.e(TAG, "Samples have no features")
                return false
            }

            Log.d(TAG, "Starting training with ${samples.size} samples for ${samplesByMove.size} moves")
            Log.d(TAG, "Feature vector size: $numFeatures")

            // 分配特徵類型
            assignFeatureTypes(numFeatures)

            // 計算特徵重要性
            calculateFeatureImportance(samplesByMove, numFeatures)

            // 特徵選擇
            if (useFeatureSelection) {
                performFeatureSelection(numFeatures)
            } else {
                // 如果不使用特徵選擇，則使用所有特徵
                for (i in 0 until numFeatures) {
                    selectedFeatures.add(i)
                }
            }

            Log.d(TAG, "Selected ${selectedFeatures.size}/${numFeatures} features")

            // 為每個動作計算特徵範圍
            for ((moveName, moveSamples) in samplesByMove) {
                if (moveSamples.isEmpty()) {
                    Log.w(TAG, "Move '$moveName' has no samples, skipping")
                    continue
                }

                // 計算該動作的特徵範圍
                val featureRanges = calculateFeatureRanges(moveSamples)

                // 計算每種特徵類型的重要性
                val typeWeights = calculateFeatureTypeWeights(featureRanges)

                // 創建動作定義
                moveDefinitions[moveName] = MoveDefinition(
                    name = moveName,
                    featureRanges = featureRanges,
                    angleFeatureWeight = typeWeights.first,
                    distanceFeatureWeight = typeWeights.second,
                    positionFeatureWeight = typeWeights.third,
                    depthFeatureWeight = typeWeights.fourth
                )

                Log.d(TAG, "Registered move '$moveName' with ${featureRanges.size} feature ranges")
            }

            // 動態優化閾值
            if (useDynamicThreshold) {
                optimizeConfidenceThreshold(samples)
            }

            // 設置動作轉換檢測的速度閾值
            setupTransitionDetection(samples)

            return moveDefinitions.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error training classifier", e)
            return false
        }
    }

    /**
     * 分配特徵類型 (特徵索引 -> 特徵類型)
     */
    private fun assignFeatureTypes(numFeatures: Int) {
        featureTypes.clear()

        // 根據您的規範化方法分配特徵類型
        // 這裡的分配應該與您使用的normalizePose函數中的特徵順序一致

        // 示例分配 (請根據您的實際特徵順序調整)
        val angleFeatures = 0..6 // 前7個特徵為角度特徵
        val distanceFeatures = 7..16 // 接下來10個特徵為距離特徵
        val positionFeatures = 17..18 // 接下來2個特徵為位置特徵
        val depthFeatures = listOf(19, 20) // 最後2個為深度相關特徵

        // 分配特徵類型
        for (i in 0 until numFeatures) {
            val type = when (i) {
                in angleFeatures -> FEATURE_TYPE_ANGLE
                in distanceFeatures -> FEATURE_TYPE_DISTANCE
                in positionFeatures -> FEATURE_TYPE_POSITION
                in depthFeatures -> FEATURE_TYPE_DEPTH
                else -> FEATURE_TYPE_POSITION // 默認為位置特徵
            }
            featureTypes[i] = type
        }

        Log.d(TAG, "Assigned feature types: ${featureTypes.size} features categorized")
    }

    /**
     * 計算每種特徵類型的權重
     */
    private fun calculateFeatureTypeWeights(featureRanges: Map<Int, FeatureRange>): Quadruple<Float, Float, Float, Float> {
        // 默認權重
        var angleWeight = 1.0f
        var distanceWeight = 1.0f
        var positionWeight = 1.0f
        var depthWeight = 0.8f

        // 分組計算各類型特徵的平均重要性
        val typeImportance = mutableMapOf<Int, MutableList<Float>>()

        for ((_, range) in featureRanges) {
            val type = range.featureType
            if (!typeImportance.containsKey(type)) {
                typeImportance[type] = mutableListOf()
            }
            typeImportance[type]?.add(range.weight * range.reliability)
        }

        // 計算每種類型的平均重要性
        for ((type, values) in typeImportance) {
            if (values.isEmpty()) continue

            val avgImportance = values.average().toFloat()
            when (type) {
                FEATURE_TYPE_ANGLE -> angleWeight = avgImportance.coerceIn(0.8f, 1.2f)
                FEATURE_TYPE_DISTANCE -> distanceWeight = avgImportance.coerceIn(0.8f, 1.2f)
                FEATURE_TYPE_POSITION -> positionWeight = avgImportance.coerceIn(0.8f, 1.2f)
                FEATURE_TYPE_DEPTH -> depthWeight = avgImportance.coerceIn(0.6f, 1.1f)
            }
        }

        return Quadruple(angleWeight, distanceWeight, positionWeight, depthWeight)
    }

    /**
     * 輔助類 - 四元組
     */
    data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )

    /**
     * 計算特徵重要性
     */
    private fun calculateFeatureImportance(samplesByMove: Map<String, List<NormalizedSample>>, numFeatures: Int) {
        Log.d(TAG, "Calculating feature importance for $numFeatures features across ${samplesByMove.size} moves")

        // 每個特徵的均值
        val moveFeatureMeans = mutableMapOf<String, MutableMap<Int, Float>>()

        // 1. 計算每個動作每個特徵的平均值
        for ((moveName, samples) in samplesByMove) {
            val featureMeans = mutableMapOf<Int, Float>()
            moveFeatureMeans[moveName] = featureMeans

            for (featureIndex in 0 until numFeatures) {
                try {
                    val validSamples = samples.filter { it.features.size > featureIndex }
                    if (validSamples.isEmpty()) continue

                    val mean = validSamples.map { it.features[featureIndex] }.average().toFloat()
                    featureMeans[featureIndex] = mean
                } catch (e: Exception) {
                    Log.w(TAG, "Error calculating mean for move $moveName, feature $featureIndex", e)
                }
            }
        }

        // 2. 計算每個特徵的總體平均值
        val globalMeans = (0 until numFeatures).associateWith { featureIndex ->
            moveFeatureMeans.values
                .mapNotNull { it[featureIndex] }
                .takeIf { it.isNotEmpty() }
                ?.average()?.toFloat() ?: 0f
        }

        // 3. 計算每個特徵的類間方差和類內方差
        for (featureIndex in 0 until numFeatures) {
            try {
                val globalMean = globalMeans[featureIndex] ?: 0f

                // 只有一個動作的情況
                if (samplesByMove.size <= 1) {
                    // 根據特徵類型分配預設權重
                    featureImportance[featureIndex] = when (featureTypes[featureIndex]) {
                        FEATURE_TYPE_ANGLE -> 0.8f
                        FEATURE_TYPE_DISTANCE -> 0.7f
                        FEATURE_TYPE_POSITION -> 0.7f
                        FEATURE_TYPE_DEPTH -> 0.6f
                        else -> 0.7f
                    }
                    continue
                }

                // 計算類間方差 (Between-class variance)
                val betweenClassVariance = moveFeatureMeans.values
                    .mapNotNull { it[featureIndex] }
                    .map { (it - globalMean).pow(2) }
                    .takeIf { it.isNotEmpty() }
                    ?.average() ?: 0.0

                // 計算類內方差 (Within-class variance)
                var totalWithinClassVariance = 0.0
                var validMoveCount = 0

                for ((moveName, samples) in samplesByMove) {
                    val moveMean = moveFeatureMeans[moveName]?.get(featureIndex) ?: continue

                    val validSamples = samples.filter { it.features.size > featureIndex }
                    if (validSamples.isEmpty()) continue

                    val variance = validSamples
                        .map { (it.features[featureIndex] - moveMean).pow(2) }
                        .average()

                    totalWithinClassVariance += variance
                    validMoveCount++
                }

                // 平均類內方差
                val withinClassVariance = if (validMoveCount > 0) {
                    totalWithinClassVariance / validMoveCount
                } else {
                    0.0001 // 防止除以零
                }

                // 計算F分數 (類間方差/類內方差)
                val fScore = if (withinClassVariance > 0) {
                    (betweenClassVariance / withinClassVariance).toFloat()
                } else {
                    10f // 高分
                }

                // 根據特徵類型調整F分數
                val adjustedFScore = when (featureTypes[featureIndex]) {
                    FEATURE_TYPE_ANGLE -> fScore * 1.0f
                    FEATURE_TYPE_DISTANCE -> fScore * 1.0f
                    FEATURE_TYPE_POSITION -> fScore * 0.9f
                    FEATURE_TYPE_DEPTH -> fScore * 0.8f // Z軸特徵稍微降低重要性
                    else -> fScore
                }

                // 正規化為0-1分數
                featureImportance[featureIndex] = min(1f, adjustedFScore / 10f)

            } catch (e: Exception) {
                Log.e(TAG, "Error calculating importance for feature $featureIndex", e)
                featureImportance[featureIndex] = 0.3f // 預設低重要性
            }
        }

        Log.d(TAG, "Calculated importance for ${featureImportance.size} features")
    }

    /**
     * 輔助函數 - 計算平方
     */
    private fun Float.pow(n: Int): Float = if (n == 2) this * this else this

    /**
     * 執行特徵選擇
     */
    private fun performFeatureSelection(numFeatures: Int) {
        selectedFeatures.clear()

        // 收集所有特徵的重要性分數
        val importance = (0 until numFeatures).map { featureIndex ->
            Pair(featureIndex, featureImportance[featureIndex] ?: 0f)
        }

        // 先選取每種類型中最重要的特徵
        val typeTopFeatures = featureTypes.entries
            .groupBy { it.value } // 按類型分組
            .flatMap { (type, features) ->
                // 取每種類型中前N個最重要的特徵
                val count = when (type) {
                    FEATURE_TYPE_ANGLE -> 3
                    FEATURE_TYPE_DISTANCE -> 3
                    FEATURE_TYPE_POSITION -> 2
                    FEATURE_TYPE_DEPTH -> 2
                    else -> 2
                }

                features
                    .map { it.key }
                    .sortedByDescending { featureImportance[it] ?: 0f }
                    .take(count)
            }

        // 添加所有類型中最重要的特徵
        selectedFeatures.addAll(typeTopFeatures)

        // 再添加剩餘特徵中重要性最高的
        val remainingFeatures = importance
            .filter { it.first !in selectedFeatures }
            .sortedByDescending { it.second }

        // 添加額外的重要特徵，不超過總特徵數的60%
        val maxFeatures = (numFeatures * 0.6f).toInt().coerceAtLeast(
            selectedFeatures.size + 3 // 至少再添加3個特徵
        )

        // 添加重要性超過閾值的特徵
        for ((featureIndex, importanceScore) in remainingFeatures) {
            if (selectedFeatures.size >= maxFeatures) break

            if (importanceScore >= FEATURE_SIGNIFICANCE_THRESHOLD) {
                selectedFeatures.add(featureIndex)
            }
        }

        Log.d(TAG, "Selected ${selectedFeatures.size} features out of $numFeatures")
        selectedFeatures.sorted().forEach { featureIndex ->
            val importance = featureImportance[featureIndex] ?: 0f
            val type = when (featureTypes[featureIndex]) {
                FEATURE_TYPE_ANGLE -> "Angle"
                FEATURE_TYPE_DISTANCE -> "Distance"
                FEATURE_TYPE_POSITION -> "Position"
                FEATURE_TYPE_DEPTH -> "Depth"
                else -> "Unknown"
            }
            Log.d(TAG, "  Selected feature $featureIndex: $type (importance: $importance)")
        }
    }

    /**
     * 計算特徵範圍
     */
    private fun calculateFeatureRanges(samples: List<NormalizedSample>): Map<Int, FeatureRange> {
        val featureRanges = mutableMapOf<Int, FeatureRange>()

        // 過濾有效樣本
        val validSamples = samples.filter { it.features.isNotEmpty() }
        if (validSamples.isEmpty()) return featureRanges

        // 獲取特徵數量
        val numFeatures = validSamples.first().features.size

        // 只處理選中的特徵
        for (featureIndex in selectedFeatures) {
            if (featureIndex >= numFeatures) continue

            try {
                // 獲取該特徵的所有值
                val featureValues = validSamples
                    .filter { it.features.size > featureIndex }
                    .map { it.features[featureIndex] }

                if (featureValues.isEmpty()) continue

                // 獲取特徵重要性分數和類型
                val importance = featureImportance[featureIndex] ?: 0.5f
                val featureType = featureTypes[featureIndex] ?: FEATURE_TYPE_POSITION

                // 統計特徵分佈
                val min = featureValues.minOrNull() ?: 0f
                val max = featureValues.maxOrNull() ?: 0f
                val mean = featureValues.average().toFloat()

                // 排序用於四分位數計算
                val sortedValues = featureValues.sorted()

                // 四分位數計算
                val q1 = if (sortedValues.size >= 4)
                    sortedValues[sortedValues.size / 4] else min
                val median = if (sortedValues.isNotEmpty())
                    sortedValues[sortedValues.size / 2] else mean
                val q3 = if (sortedValues.size >= 4)
                    sortedValues[sortedValues.size * 3 / 4] else max

                // 計算標準差
                val variance = featureValues.map { (it - mean) * (it - mean) }.average().toFloat()
                val stdDev = sqrt(variance)

                // 計算可靠性 (基於分佈形狀)
                val reliability = calculateReliability(sortedValues, featureType)

                // 根據特徵類型調整權重
                var adjustedWeight = importance

                // 針對特徵類型調整權重
                adjustedWeight *= when (featureType) {
                    FEATURE_TYPE_ANGLE -> 1.0f
                    FEATURE_TYPE_DISTANCE -> 0.9f
                    FEATURE_TYPE_POSITION -> 0.85f
                    FEATURE_TYPE_DEPTH -> 0.8f // Z軸特徵權重較低
                    else -> 0.9f
                }

                // 基於可靠性調整權重
                adjustedWeight *= reliability

                // 創建特徵範圍
                featureRanges[featureIndex] = FeatureRange(
                    featureIndex = featureIndex,
                    featureType = featureType,
                    min = min,
                    max = max,
                    q1 = q1,
                    median = median,
                    q3 = q3,
                    mean = mean,
                    stdDev = stdDev,
                    weight = adjustedWeight,
                    reliability = reliability
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error processing feature $featureIndex", e)
            }
        }

        return featureRanges
    }

    /**
     * 計算特徵可靠性評分
     */
    private fun calculateReliability(values: List<Float>, featureType: Int): Float {
        if (values.size < 3) return 0.5f

        // 1. 檢查分佈的一致性 (使用四分位數範圍)
        val sortedValues = values.sorted()
        val q1 = sortedValues[values.size / 4]
        val q3 = sortedValues[values.size * 3 / 4]
        val iqr = q3 - q1

        // 如果IQR太小，可能是過於集中的分佈
        if (iqr < 0.01f) return 0.7f

        // 2. 檢查異常值比例
        val lowerBound = q1 - 1.5f * iqr
        val upperBound = q3 + 1.5f * iqr
        val outlierCount = values.count { it < lowerBound || it > upperBound }
        val outlierRatio = outlierCount.toFloat() / values.size

        // 3. 根據特徵類型調整期望的穩定性和異常容忍度
        val maxOutlierRatio = when (featureType) {
            FEATURE_TYPE_ANGLE -> 0.1f
            FEATURE_TYPE_DISTANCE -> 0.15f
            FEATURE_TYPE_POSITION -> 0.15f
            FEATURE_TYPE_DEPTH -> 0.25f // Z軸容許更多異常
            else -> 0.15f
        }

        // 4. 計算最終可靠性得分
        val outlierPenalty = min(1.0f, outlierRatio / maxOutlierRatio)
        val reliability = 1.0f - outlierPenalty

        return reliability.coerceIn(0.5f, 1.0f) // 確保可靠性在合理範圍內
    }

    /**
     * 優化置信度閾值
     */
    private fun optimizeConfidenceThreshold(samples: List<NormalizedSample>) {
        try {
            val initialThreshold = confidenceThreshold

            // 定義搜尋範圍
            val minThreshold = max(0.65f, initialThreshold - 0.1f)
            val maxThreshold = min(0.95f, initialThreshold + 0.1f)
            val step = 0.05f

            Log.d(TAG, "Optimizing confidence threshold in range $minThreshold - $maxThreshold")

            var bestThreshold = initialThreshold
            var bestF1Score = 0.0f

            // 測試不同閾值
            var currentThreshold = minThreshold
            while (currentThreshold <= maxThreshold) {
                val tempConfidenceThreshold = currentThreshold

                // 評估此閾值
                val metrics = evaluateThreshold(samples, tempConfidenceThreshold)
                val f1Score = metrics.f1Score

                Log.d(TAG, "Threshold: $tempConfidenceThreshold, F1 Score: $f1Score, " +
                        "Precision: ${metrics.precision}, Recall: ${metrics.recall}")

                // 更新最佳閾值
                if (f1Score > bestF1Score) {
                    bestF1Score = f1Score
                    bestThreshold = tempConfidenceThreshold
                }

                currentThreshold += step
            }

            // 設置最佳閾值
            confidenceThreshold = bestThreshold
            Log.d(TAG, "Optimized confidence threshold: $confidenceThreshold (F1 Score: $bestF1Score)")
        } catch (e: Exception) {
            Log.e(TAG, "Error optimizing threshold", e)
        }
    }

    /**
     * 評估指定閾值的性能
     */
    private fun evaluateThreshold(samples: List<NormalizedSample>, threshold: Float): ClassificationMetrics {
        var truePositives = 0
        var falsePositives = 0
        var falseNegatives = 0
        var trueNegatives = 0

        // 保存原閾值
        val originalThreshold = confidenceThreshold
        confidenceThreshold = threshold

        // 評估每個樣本
        for (sample in samples) {
            val prediction = predict(sample.features)
            val predictedLabel = prediction.first
            val trueLabel = sample.label

            if (predictedLabel != UNKNOWN_ACTION) {
                if (predictedLabel == trueLabel) {
                    truePositives++
                } else {
                    falsePositives++
                }
            } else {
                if (trueLabel != UNKNOWN_ACTION) {
                    falseNegatives++
                } else {
                    trueNegatives++
                }
            }
        }

        // 恢復原閾值
        confidenceThreshold = originalThreshold

        // 計算指標
        val precision = if (truePositives + falsePositives > 0)
            truePositives.toFloat() / (truePositives + falsePositives) else 0f
        val recall = if (truePositives + falseNegatives > 0)
            truePositives.toFloat() / (truePositives + falseNegatives) else 0f
        val f1Score = if (precision + recall > 0)
            2 * precision * recall / (precision + recall) else 0f

        return ClassificationMetrics(
            precision = precision,
            recall = recall,
            f1Score = f1Score,
            truePositives = truePositives,
            falsePositives = falsePositives,
            falseNegatives = falseNegatives,
            trueNegatives = trueNegatives
        )
    }

    /**
     * 分類指標數據類
     */
    data class ClassificationMetrics(
        val precision: Float,
        val recall: Float,
        val f1Score: Float,
        val truePositives: Int,
        val falsePositives: Int,
        val falseNegatives: Int,
        val trueNegatives: Int
    )

    /**
     * 設置動作轉換檢測的速度閾值
     */
    private fun setupTransitionDetection(samples: List<NormalizedSample>) {
        val velocityThresholds = mutableMapOf<Int, Float>()

        // 按動作分組
        val samplesByMove = samples.groupBy { it.label }

        // 分析每個動作的特徵變化率
        for ((moveName, moveSamples) in samplesByMove) {
            // 至少需要兩個樣本才能計算變化率
            if (moveSamples.size < 2) continue

            // 對於每個選定的特徵
            for (featureIndex in selectedFeatures) {
                // 收集特徵值
                val featureValues = moveSamples
                    .filter { it.features.size > featureIndex }
                    .map { it.features[featureIndex] }

                if (featureValues.size < 2) continue

                // 計算標準差作為變化率參考
                val mean = featureValues.average().toFloat()
                val stdDev = sqrt(featureValues.map { (it - mean) * (it - mean) }.average().toFloat())

                // 根據特徵類型設置閾值
                val baseThreshold = when (featureTypes[featureIndex]) {
                    FEATURE_TYPE_ANGLE -> 1.5f * stdDev
                    FEATURE_TYPE_DISTANCE -> 2.0f * stdDev
                    FEATURE_TYPE_POSITION -> 1.8f * stdDev
                    FEATURE_TYPE_DEPTH -> 2.5f * stdDev // Z軸允許更大變化
                    else -> 2.0f * stdDev
                }

                // 更新閾值 (取最大值以確保不會因小變化觸發)
                velocityThresholds[featureIndex] = max(
                    velocityThresholds[featureIndex] ?: 0f,
                    baseThreshold
                )
            }
        }

        // 設置轉換檢測器閾值
        transitionDetector.setVelocityThresholds(velocityThresholds)

        Log.d(TAG, "Setup transition thresholds for ${velocityThresholds.size} features")
    }

    /**
     * 從標準化特徵預測動作
     */
    fun predict(features: List<Float>): Pair<String, Float> {
        if (moveDefinitions.isEmpty() || features.isEmpty()) {
            return Pair(UNKNOWN_ACTION, 0f)
        }

        // 計算每個動作的匹配分數
        val moveScores = moveDefinitions.map { (name, definition) ->
            // 特徵分數和權重
            val typedScores = mutableMapOf<Int, MutableList<Pair<Float, Float>>>()

            // 只考慮選定的特徵
            for (featureIndex in selectedFeatures) {
                if (featureIndex >= features.size) continue

                // 獲取特徵範圍
                val range = definition.featureRanges[featureIndex] ?: continue

                // 計算匹配分數
                val featureValue = features[featureIndex]
                val matchScore = range.getMatchScore(featureValue)

                // 按特徵類型分組
                val featureType = range.featureType
                if (!typedScores.containsKey(featureType)) {
                    typedScores[featureType] = mutableListOf()
                }

                typedScores[featureType]?.add(Pair(matchScore, range.weight))
            }

            // 計算每種類型的加權分數
            val typeScores = mutableMapOf<Int, Float>()
            var totalTypeWeight = 0f

            for ((type, scoresAndWeights) in typedScores) {
                val totalWeight = scoresAndWeights.sumOf { it.second.toDouble() }.toFloat()
                if (totalWeight <= 0) continue

                val weightedScore = scoresAndWeights.sumOf {
                    (it.first * it.second).toDouble()
                }.toFloat() / totalWeight

                // 根據動作定義中的類型權重調整分數
                val typeWeight = when (type) {
                    FEATURE_TYPE_ANGLE -> definition.angleFeatureWeight
                    FEATURE_TYPE_DISTANCE -> definition.distanceFeatureWeight
                    FEATURE_TYPE_POSITION -> definition.positionFeatureWeight
                    FEATURE_TYPE_DEPTH -> definition.depthFeatureWeight
                    else -> 1.0f
                }

                typeScores[type] = weightedScore
                totalTypeWeight += typeWeight
            }

            // 計算最終加權分數
            var finalScore = 0f
            if (totalTypeWeight > 0) {
                for ((type, score) in typeScores) {
                    val typeWeight = when (type) {
                        FEATURE_TYPE_ANGLE -> definition.angleFeatureWeight
                        FEATURE_TYPE_DISTANCE -> definition.distanceFeatureWeight
                        FEATURE_TYPE_POSITION -> definition.positionFeatureWeight
                        FEATURE_TYPE_DEPTH -> definition.depthFeatureWeight
                        else -> 1.0f
                    }

                    finalScore += (score * typeWeight) / totalTypeWeight
                }
            }

            Pair(name, finalScore)
        }

        // 找出最佳匹配
        val bestMatch = moveScores.maxByOrNull { it.second } ?:
        return Pair(UNKNOWN_ACTION, 0f)

        // 計算最佳分數與次佳分數的差距
        val secondBest = moveScores
            .filter { it.first != bestMatch.first }
            .maxByOrNull { it.second }

        // 如果最佳分數明顯優於次佳，提升置信度
        val enhancedConfidence = if (secondBest != null) {
            val margin = bestMatch.second - secondBest.second
            // 添加穩定性獎勵
            val bonus = min(0.15f, margin * 2)
            min(1.0f, bestMatch.second + bonus)
        } else {
            bestMatch.second
        }

        // 添加動作預測記錄
        addPredictionRecord(bestMatch.first, enhancedConfidence, features)

        // 檢查置信度是否超過閾值
        return if (enhancedConfidence >= confidenceThreshold) {
            Pair(bestMatch.first, enhancedConfidence)
        } else {
            Pair(UNKNOWN_ACTION, enhancedConfidence)
        }
    }

    /**
     * 添加預測記錄
     */
    private fun addPredictionRecord(moveName: String, confidence: Float, features: List<Float>) {
        // 添加新紀錄
        predictionHistory.add(PredictionRecord(
            timestamp = System.currentTimeMillis(),
            moveName = moveName,
            confidence = confidence,
            features = features
        ))

        // 保持歷史記錄在一定大小內
        if (predictionHistory.size > 10) {
            predictionHistory.removeAt(0)
        }
    }

    /**
     * 基於時間窗口的預測，提供更穩定的結果
     */
    fun predictWithWindow(
        features: List<Float>,
        previousPredictions: List<Pair<String, Float>>,
        requiredConsensus: Int = 2
    ): Pair<String, Float> {
        // 檢測姿勢是否處於轉換狀態
        val isTransitioning = transitionDetector.update(features, System.currentTimeMillis())

        // 調整所需共識
        val adjustedConsensus = if (isTransitioning) {
            // 轉換狀態需要更高共識
            requiredConsensus + 1
        } else {
            requiredConsensus
        }

        // 獲取當前預測
        val currentPrediction = predict(features)

        // 結合之前的預測
        val allPredictions = previousPredictions + currentPrediction

        // 只考慮非unknown且超過閾值的預測
        val validPredictions = allPredictions
            .filter { it.first != UNKNOWN_ACTION && it.second >= confidenceThreshold * 0.85f }

        // 考慮時間衰減因子 - 較舊的預測權重較低
        val weightedPredictions = validPredictions.mapIndexed { index, prediction ->
            // 指數衰減權重
            val weight = exp(index.toFloat() / validPredictions.size - 1)
            Triple(prediction.first, prediction.second, weight)
        }

        // 按動作分組並計算加權投票
        val predictionVotes = weightedPredictions
            .groupBy { it.first }  // 按動作分組
            .mapValues { (_, predictions) ->
                // 計算加權總和
                predictions.sumOf { (_, confidence, weight) ->
                    (confidence * weight).toDouble()
                }.toFloat()
            }

        // 找出票數最多的預測
        val mostVoted = predictionVotes.maxByOrNull { it.value }

        // 檢查是否滿足共識要求
        return if (mostVoted != null &&
            validPredictions.count { it.first == mostVoted.key } >= adjustedConsensus) {
            // 計算該預測的平均置信度
            val avgConfidence = validPredictions
                .filter { it.first == mostVoted.key }
                .map { it.second }
                .average()
                .toFloat()

            // 添加穩定性獎勵
            val stabilityBonus = min(0.1f, 0.02f * validPredictions.count { it.first == mostVoted.key })
            val finalConfidence = min(1.0f, avgConfidence + stabilityBonus)

            Pair(mostVoted.key, finalConfidence)
        } else {
            Pair(UNKNOWN_ACTION, 0f)
        }
    }

    /**
     * 獲取已訓練動作列表
     */
    fun getTrainedMoves(): List<String> {
        return moveDefinitions.keys.toList()
    }

    /**
     * 檢查分類器是否已訓練
     */
    fun isTrained(): Boolean {
        return moveDefinitions.isNotEmpty()
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
     * 獲取診斷資訊
     */
    fun getDiagnosticInfo(): Map<String, Any> {
        val info = mutableMapOf<String, Any>()

        // 基本資訊
        info["numMoves"] = moveDefinitions.size
        info["confidenceThreshold"] = confidenceThreshold
        info["selectedFeatures"] = selectedFeatures.sorted()
        info["numImportantFeatures"] = featureImportance.count { it.value > 0.5f }

        // 每個動作使用的特徵數量
        val moveFeatureCounts = moveDefinitions.mapValues { it.value.featureRanges.size }
        info["moveFeatureCounts"] = moveFeatureCounts

        // 特徵類型統計
        val featureTypeCounts = selectedFeatures.groupBy { featureTypes[it] ?: -1 }
            .mapValues { it.value.size }
        info["featureTypeCounts"] = featureTypeCounts

        return info
    }
}