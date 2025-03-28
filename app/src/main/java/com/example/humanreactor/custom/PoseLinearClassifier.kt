package com.example.humanreactor.custom

class PoseLinearClassifier {
    // save weights and biases for each angles
    private val weights = mutableMapOf<String, List<Double>>()
    private val biases = mutableMapOf<String, Double>()

    // lr and epochs
    private var learningRate = 0.01
    private var epochs = 1000

    fun modelTrained():Boolean{
        return weights.size > 0
    }

    // train model
    fun train(dataset: List<Pair<List<List<Double>>, String>>) {
        val allSamples = mutableListOf<Pair<List<Double>, String>>()

        dataset.forEach { (samplesList, className) ->
            samplesList.forEach { sample ->
                allSamples.add(Pair(sample, className))
            }
        }

        val classes = allSamples.map { it.second }.distinct()

        val featureDimension = allSamples.firstOrNull()?.first?.size ?: 0
        if (featureDimension == 0 || allSamples.isEmpty()) return

        // init weight and biases for each classes
        classes.forEach { className ->
            weights[className] = List(featureDimension) { Math.random() * 0.1 - 0.05 }
            biases[className] = 0.0
        }

        // train
        repeat(epochs) { epoch ->
            val shuffledData = allSamples.shuffled()

            shuffledData.forEach { (features, actualClass) ->
                // scoring for each sample
                val scores = mutableMapOf<String, Double>()

                classes.forEach { className ->
                    val w = weights[className] ?: return@forEach
                    val b = biases[className] ?: 0.0

                    //  w·x + b
                    var score = b
                    for (i in features.indices) {
                        score += w[i] * features[i]
                    }
                    scores[className] = score
                }

                // use softmax for prob
                val expScores = scores.mapValues { Math.exp(it.value) }
                val sumExp = expScores.values.sum()
                val probabilities = expScores.mapValues { it.value / sumExp }

                classes.forEach { className ->
                    val w = weights[className]?.toMutableList() ?: return@forEach
                    val b = biases[className] ?: 0.0

                    val targetProb = if (className == actualClass) 1.0 else 0.0
                    val currentProb = probabilities[className] ?: 0.0
                    val gradient = currentProb - targetProb

                    for (i in features.indices) {
                        w[i] = w[i] - learningRate * gradient * features[i]
                    }

                    biases[className] = b - learningRate * gradient

                    weights[className] = w
                }
            }
        }
    }

    fun predict(features: List<Double>): Pair<String, Double> {
        val scores = mutableMapOf<String, Double>()

        weights.forEach { (className, w) ->
            val b = biases[className] ?: 0.0

            var score = b
            for (i in features.indices) {
                if (i < w.size) {
                    score += w[i] * features[i]
                }
            }
            scores[className] = score
        }

        val expScores = scores.mapValues { Math.exp(it.value) }
        val sumExp = expScores.values.sum()
        val probabilities = expScores.mapValues { it.value / sumExp }

        return probabilities.maxByOrNull { it.value }?.let {
            Pair(it.key, it.value)
        } ?: Pair("未知", 0.0)
    }

    fun predictPose(samples: List<List<Double>>): String {
        val predictions = samples.map { predict(it) }

        val weightedVotes = mutableMapOf<String, Double>()

        predictions.forEach { (className, confidence) ->
            weightedVotes[className] = (weightedVotes[className] ?: 0.0) + confidence
        }

        return weightedVotes.maxByOrNull { it.value }?.key ?: "未知"
    }

    // 設置學習參數
    fun setLearningParameters(rate: Double, numEpochs: Int) {
        if (rate > 0) learningRate = rate
        if (numEpochs > 0) epochs = numEpochs
    }

    // 獲取模型參數（用於保存模型）
    fun getModelParameters(): Pair<Map<String, List<Double>>, Map<String, Double>> {
        return Pair(weights, biases)
    }

    // 載入模型參數
    fun loadModelParameters(modelWeights: Map<String, List<Double>>, modelBiases: Map<String, Double>) {
        weights.clear()
        biases.clear()

        weights.putAll(modelWeights)
        biases.putAll(modelBiases)
    }
}