package com.example.humanreactor.AI_Model

import android.util.Log
import com.example.humanreactor.customizedMove.NormalizedSample
import kotlin.math.exp

/**
 * 神經網絡分類器，用於替代規則基礎的分類器
 * 實現了一個簡單的多層感知器 (MLP) 來進行動作分類
 */
class NeuralNetworkClassifier(
    private var confidenceThreshold: Float = 0.75f,
    private val learningRate: Float = 0.01f,
    private val epochs: Int = 1000,
    private val hiddenLayerSize: Int = 32,
    private val useBatchNormalization: Boolean = true,
    private val useDropout: Boolean = true,
    private val dropoutRate: Float = 0.3f
) {
    companion object {
        private const val TAG = "NeuralNetworkClassifier"
        private const val UNKNOWN_ACTION = "unknown"
    }

    // 權重矩陣和偏置
    private var inputToHiddenWeights: Array<FloatArray> = emptyArray()
    private var hiddenBias: FloatArray = floatArrayOf()
    private var hiddenToOutputWeights: Array<FloatArray> = emptyArray()
    private var outputBias: FloatArray = floatArrayOf()

    // 批次正規化參數
    private var batchNormGamma: FloatArray = floatArrayOf()
    private var batchNormBeta: FloatArray = floatArrayOf()
    private var batchNormMean: FloatArray = floatArrayOf()
    private var batchNormVar: FloatArray = floatArrayOf()

    // 類別標籤映射
    private var classes: List<String> = listOf()
    private var classToIndexMap: Map<String, Int> = mapOf()
    private var indexToClassMap: Map<Int, String> = mapOf()

    // 模型統計
    private var inputFeatureSize: Int = 0
    private var featureImportance: MutableMap<Int, Float> = mutableMapOf()

    // 是否已訓練
    private var isTrained: Boolean = false

    /**
     * 訓練神經網絡模型
     */
    fun train(samples: List<NormalizedSample>): Boolean {
        try {
            Log.d(TAG, "開始訓練神經網絡模型，樣本數量: ${samples.size}")

            // 檢查樣本是否有效
            if (samples.isEmpty()) {
                Log.e(TAG, "沒有樣本數據可用於訓練")
                return false
            }

            // 獲取唯一的類別標籤
            classes = samples.map { it.label }.distinct().filterNot { it == UNKNOWN_ACTION }
            if (classes.isEmpty()) {
                Log.e(TAG, "沒有有效的類別標籤")
                return false
            }

            // 創建類別映射
            classToIndexMap = classes.mapIndexed { index, label -> label to index }.toMap()
            indexToClassMap = classToIndexMap.entries.associate { (k, v) -> v to k }

            Log.d(TAG, "分類類別: $classes")
            Log.d(TAG, "類別映射: $classToIndexMap")

            // 獲取輸入特徵維度
            inputFeatureSize = samples.firstOrNull()?.features?.size ?: 0
            if (inputFeatureSize == 0) {
                Log.e(TAG, "特徵維度為零")
                return false
            }

            Log.d(TAG, "輸入特徵維度: $inputFeatureSize, 輸出類別數: ${classes.size}")

            // 初始化網絡參數
            initializeNetwork(inputFeatureSize, hiddenLayerSize, classes.size)

            // 準備訓練數據
            val trainingData = samples.filter { it.label in classes }
            Log.d(TAG, "有效訓練樣本數量: ${trainingData.size}")

            // 訓練模型
            trainNetwork(trainingData)

            // 計算特徵重要性
            calculateFeatureImportance(trainingData)

            isTrained = true
            Log.d(TAG, "神經網絡訓練完成")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "訓練過程中發生錯誤", e)
            return false
        }
    }

    /**
     * 初始化網絡參數
     */
    private fun initializeNetwork(inputSize: Int, hiddenSize: Int, outputSize: Int) {
        Log.d(TAG, "初始化網絡參數: 輸入層 $inputSize, 隱藏層 $hiddenSize, 輸出層 $outputSize")

        // 初始化權重矩陣 (使用 Xavier 初始化)
        val inputScale = kotlin.math.sqrt(6.0f / (inputSize + hiddenSize))
        val outputScale = kotlin.math.sqrt(6.0f / (hiddenSize + outputSize))

        // 輸入層到隱藏層的權重
        inputToHiddenWeights = Array(inputSize) { FloatArray(hiddenSize) { (Math.random() * 2 - 1).toFloat() * inputScale } }
        hiddenBias = FloatArray(hiddenSize) { 0.01f }

        // 隱藏層到輸出層的權重
        hiddenToOutputWeights = Array(hiddenSize) { FloatArray(outputSize) { (Math.random() * 2 - 1).toFloat() * outputScale } }
        outputBias = FloatArray(outputSize) { 0.01f }

        // 初始化批次正規化參數
        if (useBatchNormalization) {
            batchNormGamma = FloatArray(hiddenSize) { 1.0f }
            batchNormBeta = FloatArray(hiddenSize) { 0.0f }
            batchNormMean = FloatArray(hiddenSize) { 0.0f }
            batchNormVar = FloatArray(hiddenSize) { 1.0f }
        }
    }

    /**
     * 訓練神經網絡
     */
    private fun trainNetwork(trainingSamples: List<NormalizedSample>) {
        // 訓練參數
        val batchSize = minOf(32, trainingSamples.size)
        val validationSplit = 0.2f

        // 分割訓練集和驗證集
        val (trainingSet, validationSet) = if (trainingSamples.size > 10) {
            trainingSamples.shuffled().let {
                val splitIndex = (it.size * (1 - validationSplit)).toInt()
                it.take(splitIndex) to it.drop(splitIndex)
            }
        } else {
            trainingSamples to listOf()
        }

        Log.d(TAG, "訓練樣本: ${trainingSet.size}, 驗證樣本: ${validationSet.size}")

        // 記錄最佳模型參數
        var bestAccuracy = 0.0f
        var bestEpoch = 0

        // 運行訓練
        for (epoch in 0 until epochs) {
            // 打亂訓練數據
            val shuffledTrainingSet = trainingSet.shuffled()

            // 批次訓練
            var epochLoss = 0.0f
            var trainingAccuracy = 0.0f

            // 批次正規化的統計
            val batchMeans = FloatArray(hiddenLayerSize) { 0.0f }
            val batchVars = FloatArray(hiddenLayerSize) { 0.0f }

            // 處理每個批次
            val numBatches = (shuffledTrainingSet.size + batchSize - 1) / batchSize
            for (batchIndex in 0 until numBatches) {
                val startIdx = batchIndex * batchSize
                val endIdx = minOf(startIdx + batchSize, shuffledTrainingSet.size)
                val batch = shuffledTrainingSet.subList(startIdx, endIdx)

                // 批次的總損失
                var batchLoss = 0.0f
                var correctPredictions = 0

                // 收集隱藏層輸出用於批次正規化
                val hiddenOutputs = Array(batch.size) { FloatArray(hiddenLayerSize) }

                // 前向傳播收集統計信息 (批次正規化)
                if (useBatchNormalization) {
                    for ((i, sample) in batch.withIndex()) {
                        val hiddenOutput = forwardToHidden(sample.features, useTrainingMode = true)
                        hiddenOutputs[i] = hiddenOutput

                        // 累加各神經元輸出值
                        for (j in 0 until hiddenLayerSize) {
                            batchMeans[j] += hiddenOutput[j] / batch.size
                        }
                    }

                    // 計算方差
                    for (i in 0 until batch.size) {
                        for (j in 0 until hiddenLayerSize) {
                            val diff = hiddenOutputs[i][j] - batchMeans[j]
                            batchVars[j] += (diff * diff) / batch.size
                        }
                    }

                    // 更新移動平均
                    val momentum = 0.9f
                    for (j in 0 until hiddenLayerSize) {
                        batchNormMean[j] = momentum * batchNormMean[j] + (1 - momentum) * batchMeans[j]
                        batchNormVar[j] = momentum * batchNormVar[j] + (1 - momentum) * batchVars[j]
                    }
                }

                // 對每個樣本進行訓練
                for (sample in batch) {
                    // 創建目標向量 (one-hot)
                    val target = createOneHotTarget(sample.label)

                    // 前向傳播
                    val output = forward(sample.features, useTrainingMode = true)

                    // 計算損失
                    val sampleLoss = calculateLoss(output, target)
                    batchLoss += sampleLoss

                    // 判斷是否預測正確
                    val predictedClass = output.indices.maxByOrNull { output[it] } ?: 0
                    val targetClass = target.indices.maxByOrNull { target[it] } ?: 0
                    if (predictedClass == targetClass) {
                        correctPredictions++
                    }

                    // 反向傳播
                    backward(sample.features, output, target)
                }

                // 批次平均損失和準確率
                batchLoss /= batch.size
                epochLoss += batchLoss
                trainingAccuracy += correctPredictions.toFloat() / batch.size
            }

            // 計算平均損失和準確率
            epochLoss /= numBatches
            trainingAccuracy /= numBatches

            // 驗證集評估
            var validationAccuracy = 0.0f
            if (validationSet.isNotEmpty()) {
                var correct = 0
                for (sample in validationSet) {
                    val output = forward(sample.features, useTrainingMode = false)
                    val predictedIdx = output.indices.maxByOrNull { output[it] } ?: 0
                    val targetIdx = classToIndexMap[sample.label] ?: continue

                    if (predictedIdx == targetIdx) {
                        correct++
                    }
                }
                validationAccuracy = correct.toFloat() / validationSet.size

                // 保存最佳模型
                if (validationAccuracy > bestAccuracy) {
                    bestAccuracy = validationAccuracy
                    bestEpoch = epoch
                    // 實際應用中可以在這裡保存模型參數
                }
            }

            // 每 100 個 epoch 記錄一次進度
            if (epoch % 100 == 0 || epoch == epochs - 1) {
                Log.d(TAG, "Epoch $epoch/$epochs - Loss: $epochLoss, " +
                        "Train Acc: $trainingAccuracy, Val Acc: $validationAccuracy")
            }

            // 提前停止條件 (如果超過 200 個 epoch 都沒有改善，則停止)
            if (epoch - bestEpoch > 200 && epoch > 500) {
                Log.d(TAG, "提前停止，沒有進一步改進")
                break
            }
        }

        Log.d(TAG, "訓練完成，最佳驗證準確率: $bestAccuracy (Epoch $bestEpoch)")
    }

    /**
     * 將特徵向量從輸入層傳播到隱藏層
     */
    private fun forwardToHidden(features: List<Float>, useTrainingMode: Boolean): FloatArray {
        // 檢查特徵向量是否與輸入層大小匹配
        if (features.size != inputFeatureSize) {
            Log.w(TAG, "特徵向量大小 (${features.size}) 與輸入層大小 ($inputFeatureSize) 不匹配")
        }

        // 從輸入層到隱藏層的前向傳播
        val hiddenLayerOutput = FloatArray(hiddenLayerSize)

        // 計算隱藏層輸入 (Z = W*X + b)
        for (i in 0 until hiddenLayerSize) {
            var sum = hiddenBias[i]
            for (j in 0 until minOf(features.size, inputFeatureSize)) {
                sum += features[j] * inputToHiddenWeights[j][i]
            }
            hiddenLayerOutput[i] = sum
        }

        // 批次正規化 (如果啟用)
        if (useBatchNormalization) {
            for (i in 0 until hiddenLayerSize) {
                // 訓練模式使用批次統計數據，測試模式使用運行平均值
                val mean = if (useTrainingMode) 0.0f else batchNormMean[i]
                val variance = if (useTrainingMode) 1.0f else batchNormVar[i]
                val epsilon = 1e-5f

                // 正規化
                hiddenLayerOutput[i] = batchNormGamma[i] *
                        ((hiddenLayerOutput[i] - mean) / kotlin.math.sqrt(variance + epsilon)) +
                        batchNormBeta[i]
            }
        }

        // 激活函數 (ReLU)
        for (i in 0 until hiddenLayerSize) {
            hiddenLayerOutput[i] = kotlin.math.max(0.0f, hiddenLayerOutput[i])
        }

        // Dropout (僅在訓練模式)
        if (useTrainingMode && useDropout) {
            for (i in 0 until hiddenLayerSize) {
                if (Math.random() < dropoutRate) {
                    hiddenLayerOutput[i] = 0.0f
                } else {
                    // 縮放以保持期望值不變
                    hiddenLayerOutput[i] /= (1.0f - dropoutRate)
                }
            }
        }

        return hiddenLayerOutput
    }

    /**
     * 前向傳播 - 計算網絡的輸出
     */
    private fun forward(features: List<Float>, useTrainingMode: Boolean): FloatArray {
        // 獲取隱藏層輸出
        val hiddenLayerOutput = forwardToHidden(features, useTrainingMode)

        // 從隱藏層到輸出層的前向傳播
        val outputLayerOutput = FloatArray(classes.size)

        // 計算輸出層輸入
        for (i in 0 until classes.size) {
            var sum = outputBias[i]
            for (j in 0 until hiddenLayerSize) {
                sum += hiddenLayerOutput[j] * hiddenToOutputWeights[j][i]
            }
            outputLayerOutput[i] = sum
        }

        // Softmax 激活函數
        var sum = 0.0f
        val expValues = FloatArray(classes.size)

        for (i in 0 until classes.size) {
            // 為了數值穩定性，先減去最大值
            val maxOutput = outputLayerOutput.maxOrNull() ?: 0.0f
            expValues[i] = exp(outputLayerOutput[i] - maxOutput)
            sum += expValues[i]
        }

        // 歸一化
        for (i in 0 until classes.size) {
            outputLayerOutput[i] = expValues[i] / sum
        }

        return outputLayerOutput
    }

    /**
     * 創建 one-hot 編碼的目標向量
     */
    private fun createOneHotTarget(label: String): FloatArray {
        val target = FloatArray(classes.size) { 0.0f }
        val index = classToIndexMap[label] ?: return target
        target[index] = 1.0f
        return target
    }

    /**
     * 計算損失 (交叉熵)
     */
    private fun calculateLoss(output: FloatArray, target: FloatArray): Float {
        var loss = 0.0f
        for (i in output.indices) {
            // 避免數值問題 (log(0))
            val outputClipped = output[i].coerceIn(1e-7f, 1.0f - 1e-7f)
            loss -= target[i] * kotlin.math.ln(outputClipped)
        }
        return loss
    }

    /**
     * 反向傳播 - 更新權重
     */
    private fun backward(
        features: List<Float>,
        output: FloatArray,
        target: FloatArray
    ) {
        // 輸出層誤差
        val outputDelta = FloatArray(classes.size)
        for (i in 0 until classes.size) {
            outputDelta[i] = output[i] - target[i]
        }

        // 隱藏層輸出 (僅用於計算梯度，不使用 dropout)
        val hiddenOutput = forwardToHidden(features, useTrainingMode = false)

        // 更新隱藏層到輸出層的權重
        for (i in 0 until hiddenLayerSize) {
            for (j in 0 until classes.size) {
                hiddenToOutputWeights[i][j] -= learningRate * outputDelta[j] * hiddenOutput[i]
            }
        }

        // 更新輸出層偏置
        for (i in 0 until classes.size) {
            outputBias[i] -= learningRate * outputDelta[i]
        }

        // 計算隱藏層誤差
        val hiddenDelta = FloatArray(hiddenLayerSize)
        for (i in 0 until hiddenLayerSize) {
            for (j in 0 until classes.size) {
                hiddenDelta[i] += outputDelta[j] * hiddenToOutputWeights[i][j]
            }

            // ReLU 導數
            if (hiddenOutput[i] <= 0) {
                hiddenDelta[i] = 0.0f
            }
        }

        // 更新輸入層到隱藏層的權重
        for (i in 0 until minOf(features.size, inputFeatureSize)) {
            for (j in 0 until hiddenLayerSize) {
                inputToHiddenWeights[i][j] -= learningRate * hiddenDelta[j] * features[i]
            }
        }

        // 更新隱藏層偏置
        for (i in 0 until hiddenLayerSize) {
            hiddenBias[i] -= learningRate * hiddenDelta[i]
        }

        // 更新批次正規化參數 (如果啟用)
        if (useBatchNormalization) {
            for (i in 0 until hiddenLayerSize) {
                batchNormGamma[i] -= learningRate * hiddenDelta[i]
                batchNormBeta[i] -= learningRate * hiddenDelta[i]
            }
        }
    }

    /**
     * 計算特徵重要性
     */
    private fun calculateFeatureImportance(samples: List<NormalizedSample>) {
        try {
            Log.d(TAG, "計算特徵重要性")
            featureImportance.clear()

            // 對於每個特徵，計算其對網絡輸出的影響
            for (featureIndex in 0 until inputFeatureSize) {
                var importance = 0.0f

                // 計算這個特徵連到隱藏層的權重的絕對值總和
                for (i in 0 until hiddenLayerSize) {
                    importance += kotlin.math.abs(inputToHiddenWeights[featureIndex][i])
                }

                // 歸一化重要性分數
                featureImportance[featureIndex] = importance / hiddenLayerSize
            }

            // 對特徵重要性進行歸一化到 [0,1] 範圍
            val maxImportance = featureImportance.values.maxOrNull() ?: 1.0f
            if (maxImportance > 0) {
                for (key in featureImportance.keys) {
                    featureImportance[key] = featureImportance[key]!! / maxImportance
                }
            }

            Log.d(TAG, "特徵重要性計算完成")
        } catch (e: Exception) {
            Log.e(TAG, "計算特徵重要性時發生錯誤", e)
        }
    }

    /**
     * 從特徵向量預測動作
     */
    fun predict(features: List<Float>): Pair<String, Float> {
        if (!isTrained || classes.isEmpty()) {
            return Pair(UNKNOWN_ACTION, 0f)
        }

        // 前向傳播
        val output = forward(features, useTrainingMode = false)

        // 找出置信度最高的類別
        val maxIndex = output.indices.maxByOrNull { output[it] } ?: 0
        val confidence = output[maxIndex]

        // 檢查置信度是否超過閾值
        return if (confidence >= confidenceThreshold) {
            Pair(indexToClassMap[maxIndex] ?: UNKNOWN_ACTION, confidence)
        } else {
            Pair(UNKNOWN_ACTION, confidence)
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
        info["confidenceThreshold"] = confidenceThreshold
        info["hiddenLayerSize"] = hiddenLayerSize
        info["inputFeatureSize"] = inputFeatureSize
        info["useBatchNormalization"] = useBatchNormalization
        info["useDropout"] = useDropout
        info["numImportantFeatures"] = featureImportance.count { it.value > 0.5f }

        return info
    }
}