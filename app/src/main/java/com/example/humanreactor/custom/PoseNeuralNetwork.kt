package com.example.humanreactor.custom

import android.content.Context
import android.util.Log
import com.google.common.reflect.TypeToken

class PoseNeuralNetwork {
    // 網絡結構
    private var inputSize = 0
    private var hiddenSize = 32
    private var outputSize = 0

    // 權重和偏置
    private lateinit var weightsInputToHidden: Array<DoubleArray>
    private lateinit var biasesHidden: DoubleArray
    private lateinit var weightsHiddenToOutput: Array<DoubleArray>
    private lateinit var biasesOutput: DoubleArray

    // 類別映射
    private val classNames = mutableListOf<String>()

    // 訓練參數
    private var learningRate = 0.01
    private var epochs = 150

    fun modelTrained():Boolean{
        return true
    }
    // 初始化網絡
    fun initialize(inputSize: Int, outputSize: Int) {
        this.inputSize = inputSize
        this.outputSize = outputSize

        // 初始化權重（小的隨機值）
        weightsInputToHidden = Array(hiddenSize) { DoubleArray(inputSize) { (Math.random() * 2 - 1) * 0.1 } }
        biasesHidden = DoubleArray(hiddenSize) { 0.0 }

        weightsHiddenToOutput = Array(outputSize) { DoubleArray(hiddenSize) { (Math.random() * 2 - 1) * 0.1 } }
        biasesOutput = DoubleArray(outputSize) { 0.0 }
    }

    // 從數據集設置類別
    fun setClasses(dataset: List<Pair<List<List<Double>>, String>>) {
        val uniqueClasses = dataset.map { it.second }.distinct()
        classNames.clear()
        classNames.addAll(uniqueClasses)
    }

    // 前向傳播
    fun forward(input: DoubleArray): DoubleArray {
        // 輸入層到隱藏層
        val hiddenOutput = DoubleArray(hiddenSize)
        for (i in 0 until hiddenSize) {
            var sum = biasesHidden[i]
            for (j in 0 until inputSize) {
                sum += input[j] * weightsInputToHidden[i][j]
            }
            // ReLU激活
            hiddenOutput[i] = if (sum > 0) sum else 0.0
        }

        // 隱藏層到輸出層
        val output = DoubleArray(outputSize)
        for (i in 0 until outputSize) {
            var sum = biasesOutput[i]
            for (j in 0 until hiddenSize) {
                sum += hiddenOutput[j] * weightsHiddenToOutput[i][j]
            }
            output[i] = sum
        }

        // Softmax激活
        val expValues = output.map { Math.exp(it) }
        val sumExp = expValues.sum()
        return expValues.map { it / sumExp }.toDoubleArray()
    }

    // 訓練模型
    fun train(dataset: List<Pair<List<List<Double>>, String>>) {
        // 處理數據
        val samples = mutableListOf<Pair<DoubleArray, DoubleArray>>()

        dataset.forEach { (samplesList, className) ->
            // 查找類別索引
            val classIndex = classNames.indexOf(className)
            if (classIndex >= 0) {
                // 創建目標輸出向量（one-hot編碼）
                val target = DoubleArray(outputSize) { if (it == classIndex) 1.0 else 0.0 }

                // 添加每個樣本
                samplesList.forEach { featureList ->
                    samples.add(Pair(featureList.toDoubleArray(), target))
                }
            }
        }

        if (samples.isEmpty()) return

        // 確保網絡已初始化
        if (inputSize == 0 || outputSize == 0) {
            inputSize = samples[0].first.size
            outputSize = classNames.size
            initialize(inputSize, outputSize)
        }

        // 執行訓練
        repeat(epochs) { epoch ->
            // 打亂數據
            val shuffledSamples = samples.shuffled()

            // 遍歷每個樣本
            shuffledSamples.forEach { (input, target) ->
                // 前向傳播
                val hiddenOutput = DoubleArray(hiddenSize)
                for (i in 0 until hiddenSize) {
                    var sum = biasesHidden[i]
                    for (j in 0 until inputSize) {
                        sum += input[j] * weightsInputToHidden[i][j]
                    }
                    // ReLU激活
                    hiddenOutput[i] = if (sum > 0) sum else 0.0
                }

                // 隱藏層到輸出層
                val output = DoubleArray(outputSize)
                for (i in 0 until outputSize) {
                    var sum = biasesOutput[i]
                    for (j in 0 until hiddenSize) {
                        sum += hiddenOutput[j] * weightsHiddenToOutput[i][j]
                    }
                    output[i] = sum
                }

                // Softmax激活
                val expValues = output.map { Math.exp(it) }
                val sumExp = expValues.sum()
                val softmaxOutput = expValues.map { it / sumExp }.toDoubleArray()

                // 反向傳播 - 計算輸出層誤差
                val outputErrors = DoubleArray(outputSize)
                for (i in 0 until outputSize) {
                    outputErrors[i] = softmaxOutput[i] - target[i]
                }

                // 計算隱藏層誤差
                val hiddenErrors = DoubleArray(hiddenSize)
                for (i in 0 until hiddenSize) {
                    for (j in 0 until outputSize) {
                        hiddenErrors[i] += outputErrors[j] * weightsHiddenToOutput[j][i]
                    }
                    // ReLU導數
                    hiddenErrors[i] *= if (hiddenOutput[i] > 0) 1.0 else 0.0
                }

                // 更新輸出層權重和偏置
                for (i in 0 until outputSize) {
                    biasesOutput[i] -= learningRate * outputErrors[i]
                    for (j in 0 until hiddenSize) {
                        weightsHiddenToOutput[i][j] -= learningRate * outputErrors[i] * hiddenOutput[j]
                    }
                }

                // 更新隱藏層權重和偏置
                for (i in 0 until hiddenSize) {
                    biasesHidden[i] -= learningRate * hiddenErrors[i]
                    for (j in 0 until inputSize) {
                        weightsInputToHidden[i][j] -= learningRate * hiddenErrors[i] * input[j]
                    }
                }
            }
        }
    }

    // 預測
    fun predict(features: List<Double>): Pair<String, Double> {
        if (classNames.isEmpty()) return Pair("未知", 0.0)

        // 前向傳播
        val output = forward(features.toDoubleArray())

        // 找到最大概率的類別
        var maxIndex = 0
        var maxProb = 0.0

        for (i in output.indices) {
            if (output[i] > maxProb) {
                maxProb = output[i]
                maxIndex = i
            }
        }

        // 返回類別名稱和概率
        return if (maxIndex < classNames.size) {
            Pair(classNames[maxIndex], maxProb)
        } else {
            Pair("未知", 0.0)
        }
    }

    // 預測多個樣本
    fun predictPose(samples: List<List<Double>>): String {
        if (samples.isEmpty() || classNames.isEmpty()) return "未知"

        // 對每個樣本進行預測
        val predictions = mutableMapOf<String, Double>()

        samples.forEach { features ->
            val (className, confidence) = predict(features)
            predictions[className] = (predictions[className] ?: 0.0) + confidence
        }

        // 返回得分最高的類別
        return predictions.maxByOrNull { it.value }?.key ?: "未知"
    }

    // 設置訓練參數
    fun setTrainingParameters(rate: Double, numEpochs: Int) {
        if (rate > 0) learningRate = rate
        if (numEpochs > 0) epochs = numEpochs
    }

    // 使用自定義方法保存模型
    fun saveModel(context: Context, filename: String) {
        try {
            context.openFileOutput(filename, Context.MODE_PRIVATE).use { fos ->
                // 保存網絡結構
                fos.write("$inputSize,$hiddenSize,$outputSize\n".toByteArray())

                // 保存類別名稱
                fos.write((classNames.joinToString(",") + "\n").toByteArray())

                // 保存weightsInputToHidden
                for (i in 0 until hiddenSize) {
                    fos.write((weightsInputToHidden[i].joinToString(",") + "\n").toByteArray())
                }

                // 保存biasesHidden
                fos.write((biasesHidden.joinToString(",") + "\n").toByteArray())

                // 保存weightsHiddenToOutput
                for (i in 0 until outputSize) {
                    fos.write((weightsHiddenToOutput[i].joinToString(",") + "\n").toByteArray())
                }

                // 保存biasesOutput
                fos.write((biasesOutput.joinToString(",")).toByteArray())
            }
        } catch (e: Exception) {
            Log.e("SimplePoseNN", "保存模型失敗", e)
        }
    }

    // 使用自定義方法載入模型
    fun loadModel(context: Context, filename: String): Boolean {
        try {
            context.openFileInput(filename).bufferedReader().use { reader ->
                // 讀取網絡結構
                val structureStr = reader.readLine()
                val structureParts = structureStr.split(",").map { it.toInt() }
                inputSize = structureParts[0]
                hiddenSize = structureParts[1]
                outputSize = structureParts[2]

                // 讀取類別名稱
                val classNamesStr = reader.readLine()
                classNames.clear()
                classNames.addAll(classNamesStr.split(","))

                // 讀取weightsInputToHidden
                weightsInputToHidden = Array(hiddenSize) { DoubleArray(inputSize) }
                for (i in 0 until hiddenSize) {
                    val weightsStr = reader.readLine()
                    val weights = weightsStr.split(",").map { it.toDouble() }
                    for (j in 0 until inputSize) {
                        weightsInputToHidden[i][j] = weights[j]
                    }
                }

                // 讀取biasesHidden
                val biasesHiddenStr = reader.readLine()
                biasesHidden = biasesHiddenStr.split(",").map { it.toDouble() }.toDoubleArray()

                // 讀取weightsHiddenToOutput
                weightsHiddenToOutput = Array(outputSize) { DoubleArray(hiddenSize) }
                for (i in 0 until outputSize) {
                    val weightsStr = reader.readLine()
                    val weights = weightsStr.split(",").map { it.toDouble() }
                    for (j in 0 until hiddenSize) {
                        weightsHiddenToOutput[i][j] = weights[j]
                    }
                }

                // 讀取biasesOutput
                val biasesOutputStr = reader.readLine()
                biasesOutput = biasesOutputStr.split(",").map { it.toDouble() }.toDoubleArray()
            }
            return true
        } catch (e: Exception) {
            Log.e("SimplePoseNN", "載入模型失敗", e)
            return false
        }
    }
}