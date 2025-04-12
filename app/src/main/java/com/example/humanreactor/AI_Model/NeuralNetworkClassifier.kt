package com.example.humanreactor.AI_Model

import android.util.Log
import com.example.humanreactor.customizedMove.NormalizedSample
import kotlin.math.exp

/**
 * Neural Network Classifier, used to replace rule-based classifiers
 * Implements a simple Multi-Layer Perceptron (MLP) for action classification
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

    // Weight matrices and biases
    private var inputToHiddenWeights: Array<FloatArray> = emptyArray()
    private var hiddenBias: FloatArray = floatArrayOf()
    private var hiddenToOutputWeights: Array<FloatArray> = emptyArray()
    private var outputBias: FloatArray = floatArrayOf()

    // Batch normalization parameters
    private var batchNormGamma: FloatArray = floatArrayOf()
    private var batchNormBeta: FloatArray = floatArrayOf()
    private var batchNormMean: FloatArray = floatArrayOf()
    private var batchNormVar: FloatArray = floatArrayOf()

    // Class label mapping
    private var classes: List<String> = listOf()
    private var classToIndexMap: Map<String, Int> = mapOf()
    private var indexToClassMap: Map<Int, String> = mapOf()

    // Model statistics
    private var inputFeatureSize: Int = 0
    private var featureImportance: MutableMap<Int, Float> = mutableMapOf()

    // Training status
    private var isTrained: Boolean = false

    /**
     * Train the neural network model
     */
    fun train(samples: List<NormalizedSample>): Boolean {
        try {
            Log.d(TAG, "Starting neural network model training, sample count: ${samples.size}")

            // Check if samples are valid
            if (samples.isEmpty()) {
                Log.e(TAG, "No sample data available for training")
                return false
            }

            // Get unique class labels
            classes = samples.map { it.label }.distinct().filterNot { it == UNKNOWN_ACTION }
            if (classes.isEmpty()) {
                Log.e(TAG, "No valid class labels")
                return false
            }

            // Create class mappings
            classToIndexMap = classes.mapIndexed { index, label -> label to index }.toMap()
            indexToClassMap = classToIndexMap.entries.associate { (k, v) -> v to k }

            Log.d(TAG, "Classification classes: $classes")
            Log.d(TAG, "Class mapping: $classToIndexMap")

            // Get input feature dimensions
            inputFeatureSize = samples.firstOrNull()?.features?.size ?: 0
            if (inputFeatureSize == 0) {
                Log.e(TAG, "Feature dimension is zero")
                return false
            }

            Log.d(TAG, "Input feature dimension: $inputFeatureSize, output class count: ${classes.size}")

            // Initialize network parameters
            initializeNetwork(inputFeatureSize, hiddenLayerSize, classes.size)

            // Prepare training data
            val trainingData = samples.filter { it.label in classes }
            Log.d(TAG, "Valid training sample count: ${trainingData.size}")

            // Train the model
            trainNetwork(trainingData)

            // Calculate feature importance
            calculateFeatureImportance(trainingData)

            isTrained = true
            Log.d(TAG, "Neural network training completed")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error occurred during training", e)
            return false
        }
    }

    /**
     * Initialize network parameters
     */
    private fun initializeNetwork(inputSize: Int, hiddenSize: Int, outputSize: Int) {
        Log.d(TAG, "Initializing network parameters: Input layer $inputSize, Hidden layer $hiddenSize, Output layer $outputSize")

        // Initialize weight matrices (using Xavier initialization)
        val inputScale = kotlin.math.sqrt(6.0f / (inputSize + hiddenSize))
        val outputScale = kotlin.math.sqrt(6.0f / (hiddenSize + outputSize))

        // Input layer to hidden layer weights
        inputToHiddenWeights = Array(inputSize) { FloatArray(hiddenSize) { (Math.random() * 2 - 1).toFloat() * inputScale } }
        hiddenBias = FloatArray(hiddenSize) { 0.01f }

        // Hidden layer to output layer weights
        hiddenToOutputWeights = Array(hiddenSize) { FloatArray(outputSize) { (Math.random() * 2 - 1).toFloat() * outputScale } }
        outputBias = FloatArray(outputSize) { 0.01f }

        // Initialize batch normalization parameters
        if (useBatchNormalization) {
            batchNormGamma = FloatArray(hiddenSize) { 1.0f }
            batchNormBeta = FloatArray(hiddenSize) { 0.0f }
            batchNormMean = FloatArray(hiddenSize) { 0.0f }
            batchNormVar = FloatArray(hiddenSize) { 1.0f }
        }
    }

    /**
     * Train the neural network
     */
    private fun trainNetwork(trainingSamples: List<NormalizedSample>) {
        // Training parameters
        val batchSize = minOf(32, trainingSamples.size)
        val validationSplit = 0.2f

        // Split training and validation sets
        val (trainingSet, validationSet) = if (trainingSamples.size > 10) {
            trainingSamples.shuffled().let {
                val splitIndex = (it.size * (1 - validationSplit)).toInt()
                it.take(splitIndex) to it.drop(splitIndex)
            }
        } else {
            trainingSamples to listOf()
        }

        Log.d(TAG, "Training samples: ${trainingSet.size}, Validation samples: ${validationSet.size}")

        // Record best model parameters
        var bestAccuracy = 0.0f
        var bestEpoch = 0

        // Run training
        for (epoch in 0 until epochs) {
            // Shuffle training data
            val shuffledTrainingSet = trainingSet.shuffled()

            // Batch training
            var epochLoss = 0.0f
            var trainingAccuracy = 0.0f

            // Batch normalization statistics
            val batchMeans = FloatArray(hiddenLayerSize) { 0.0f }
            val batchVars = FloatArray(hiddenLayerSize) { 0.0f }

            // Process each batch
            val numBatches = (shuffledTrainingSet.size + batchSize - 1) / batchSize
            for (batchIndex in 0 until numBatches) {
                val startIdx = batchIndex * batchSize
                val endIdx = minOf(startIdx + batchSize, shuffledTrainingSet.size)
                val batch = shuffledTrainingSet.subList(startIdx, endIdx)

                // Total batch loss
                var batchLoss = 0.0f
                var correctPredictions = 0

                // Collect hidden layer outputs for batch normalization
                val hiddenOutputs = Array(batch.size) { FloatArray(hiddenLayerSize) }

                // Forward propagation to collect statistics (batch normalization)
                if (useBatchNormalization) {
                    for ((i, sample) in batch.withIndex()) {
                        val hiddenOutput = forwardToHidden(sample.features, useTrainingMode = true)
                        hiddenOutputs[i] = hiddenOutput

                        // Accumulate neuron output values
                        for (j in 0 until hiddenLayerSize) {
                            batchMeans[j] += hiddenOutput[j] / batch.size
                        }
                    }

                    // Calculate variance
                    for (i in 0 until batch.size) {
                        for (j in 0 until hiddenLayerSize) {
                            val diff = hiddenOutputs[i][j] - batchMeans[j]
                            batchVars[j] += (diff * diff) / batch.size
                        }
                    }

                    // Update moving average
                    val momentum = 0.9f
                    for (j in 0 until hiddenLayerSize) {
                        batchNormMean[j] = momentum * batchNormMean[j] + (1 - momentum) * batchMeans[j]
                        batchNormVar[j] = momentum * batchNormVar[j] + (1 - momentum) * batchVars[j]
                    }
                }

                // Train for each sample
                for (sample in batch) {
                    // Create target vector (one-hot)
                    val target = createOneHotTarget(sample.label)

                    // Forward propagation
                    val output = forward(sample.features, useTrainingMode = true)

                    // Calculate loss
                    val sampleLoss = calculateLoss(output, target)
                    batchLoss += sampleLoss

                    // Determine if prediction is correct
                    val predictedClass = output.indices.maxByOrNull { output[it] } ?: 0
                    val targetClass = target.indices.maxByOrNull { target[it] } ?: 0
                    if (predictedClass == targetClass) {
                        correctPredictions++
                    }

                    // Backpropagation
                    backward(sample.features, output, target)
                }

                // Batch average loss and accuracy
                batchLoss /= batch.size
                epochLoss += batchLoss
                trainingAccuracy += correctPredictions.toFloat() / batch.size
            }

            // Calculate average loss and accuracy
            epochLoss /= numBatches
            trainingAccuracy /= numBatches

            // Validation set evaluation
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

                // Save best model
                if (validationAccuracy > bestAccuracy) {
                    bestAccuracy = validationAccuracy
                    bestEpoch = epoch
                    // In actual applications, model parameters would be saved here
                }
            }

            // Log progress every 100 epochs
            if (epoch % 100 == 0 || epoch == epochs - 1) {
                Log.d(TAG, "Epoch $epoch/$epochs - Loss: $epochLoss, " +
                        "Train Acc: $trainingAccuracy, Val Acc: $validationAccuracy")
            }

            // Early stopping condition (if no improvement for 200 epochs and past 500 epochs)
            if (epoch - bestEpoch > 200 && epoch > 500) {
                Log.d(TAG, "Early stopping, no further improvement")
                break
            }
        }

        Log.d(TAG, "Training completed, best validation accuracy: $bestAccuracy (Epoch $bestEpoch)")
    }

    /**
     * Propagate feature vector from input layer to hidden layer
     */
    private fun forwardToHidden(features: List<Float>, useTrainingMode: Boolean): FloatArray {
        // Check if feature vector matches input layer size
        if (features.size != inputFeatureSize) {
            Log.w(TAG, "Feature vector size (${features.size}) does not match input layer size ($inputFeatureSize)")
        }

        // Forward propagation from input layer to hidden layer
        val hiddenLayerOutput = FloatArray(hiddenLayerSize)

        // Calculate hidden layer input (Z = W*X + b)
        for (i in 0 until hiddenLayerSize) {
            var sum = hiddenBias[i]
            for (j in 0 until minOf(features.size, inputFeatureSize)) {
                sum += features[j] * inputToHiddenWeights[j][i]
            }
            hiddenLayerOutput[i] = sum
        }

        // Batch normalization (if enabled)
        if (useBatchNormalization) {
            for (i in 0 until hiddenLayerSize) {
                // Training mode uses batch statistics, test mode uses running averages
                val mean = if (useTrainingMode) 0.0f else batchNormMean[i]
                val variance = if (useTrainingMode) 1.0f else batchNormVar[i]
                val epsilon = 1e-5f

                // Normalize
                hiddenLayerOutput[i] = batchNormGamma[i] *
                        ((hiddenLayerOutput[i] - mean) / kotlin.math.sqrt(variance + epsilon)) +
                        batchNormBeta[i]
            }
        }

        // Activation function (ReLU)
        for (i in 0 until hiddenLayerSize) {
            hiddenLayerOutput[i] = kotlin.math.max(0.0f, hiddenLayerOutput[i])
        }

        // Dropout (only in training mode)
        if (useTrainingMode && useDropout) {
            for (i in 0 until hiddenLayerSize) {
                if (Math.random() < dropoutRate) {
                    hiddenLayerOutput[i] = 0.0f
                } else {
                    // Scale to keep expected value unchanged
                    hiddenLayerOutput[i] /= (1.0f - dropoutRate)
                }
            }
        }

        return hiddenLayerOutput
    }

    /**
     * Forward propagation - Calculate network output
     */
    private fun forward(features: List<Float>, useTrainingMode: Boolean): FloatArray {
        // Get hidden layer output
        val hiddenLayerOutput = forwardToHidden(features, useTrainingMode)

        // Forward propagation from hidden layer to output layer
        val outputLayerOutput = FloatArray(classes.size)

        // Calculate output layer input
        for (i in 0 until classes.size) {
            var sum = outputBias[i]
            for (j in 0 until hiddenLayerSize) {
                sum += hiddenLayerOutput[j] * hiddenToOutputWeights[j][i]
            }
            outputLayerOutput[i] = sum
        }

        // Softmax activation function
        var sum = 0.0f
        val expValues = FloatArray(classes.size)

        for (i in 0 until classes.size) {
            // For numerical stability, subtract the maximum value first
            val maxOutput = outputLayerOutput.maxOrNull() ?: 0.0f
            expValues[i] = exp(outputLayerOutput[i] - maxOutput)
            sum += expValues[i]
        }

        // Normalize
        for (i in 0 until classes.size) {
            outputLayerOutput[i] = expValues[i] / sum
        }

        return outputLayerOutput
    }

    /**
     * Create one-hot encoded target vector
     */
    private fun createOneHotTarget(label: String): FloatArray {
        val target = FloatArray(classes.size) { 0.0f }
        val index = classToIndexMap[label] ?: return target
        target[index] = 1.0f
        return target
    }

    /**
     * Calculate loss (cross-entropy)
     */
    private fun calculateLoss(output: FloatArray, target: FloatArray): Float {
        var loss = 0.0f
        for (i in output.indices) {
            // Avoid numerical issues (log(0))
            val outputClipped = output[i].coerceIn(1e-7f, 1.0f - 1e-7f)
            loss -= target[i] * kotlin.math.ln(outputClipped)
        }
        return loss
    }

    /**
     * Backpropagation - Update weights
     */
    private fun backward(
        features: List<Float>,
        output: FloatArray,
        target: FloatArray
    ) {
        // Output layer error
        val outputDelta = FloatArray(classes.size)
        for (i in 0 until classes.size) {
            outputDelta[i] = output[i] - target[i]
        }

        // Hidden layer output (only for gradient calculation, not using dropout)
        val hiddenOutput = forwardToHidden(features, useTrainingMode = false)

        // Update hidden layer to output layer weights
        for (i in 0 until hiddenLayerSize) {
            for (j in 0 until classes.size) {
                hiddenToOutputWeights[i][j] -= learningRate * outputDelta[j] * hiddenOutput[i]
            }
        }

        // Update output layer bias
        for (i in 0 until classes.size) {
            outputBias[i] -= learningRate * outputDelta[i]
        }

        // Calculate hidden layer error
        val hiddenDelta = FloatArray(hiddenLayerSize)
        for (i in 0 until hiddenLayerSize) {
            for (j in 0 until classes.size) {
                hiddenDelta[i] += outputDelta[j] * hiddenToOutputWeights[i][j]
            }

            // ReLU derivative
            if (hiddenOutput[i] <= 0) {
                hiddenDelta[i] = 0.0f
            }
        }

        // Update input layer to hidden layer weights
        for (i in 0 until minOf(features.size, inputFeatureSize)) {
            for (j in 0 until hiddenLayerSize) {
                inputToHiddenWeights[i][j] -= learningRate * hiddenDelta[j] * features[i]
            }
        }

        // Update hidden layer bias
        for (i in 0 until hiddenLayerSize) {
            hiddenBias[i] -= learningRate * hiddenDelta[i]
        }

        // Update batch normalization parameters (if enabled)
        if (useBatchNormalization) {
            for (i in 0 until hiddenLayerSize) {
                batchNormGamma[i] -= learningRate * hiddenDelta[i]
                batchNormBeta[i] -= learningRate * hiddenDelta[i]
            }
        }
    }

    /**
     * Calculate feature importance
     */
    private fun calculateFeatureImportance(samples: List<NormalizedSample>) {
        try {
            Log.d(TAG, "Calculating feature importance")
            featureImportance.clear()

            // For each feature, calculate its impact on network output
            for (featureIndex in 0 until inputFeatureSize) {
                var importance = 0.0f

                // Calculate sum of absolute values of weights from this feature to hidden layer
                for (i in 0 until hiddenLayerSize) {
                    importance += kotlin.math.abs(inputToHiddenWeights[featureIndex][i])
                }

                // Normalize importance score
                featureImportance[featureIndex] = importance / hiddenLayerSize
            }

            // Normalize feature importance to [0,1] range
            val maxImportance = featureImportance.values.maxOrNull() ?: 1.0f
            if (maxImportance > 0) {
                for (key in featureImportance.keys) {
                    featureImportance[key] = featureImportance[key]!! / maxImportance
                }
            }

            Log.d(TAG, "Feature importance calculation completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error occurred while calculating feature importance", e)
        }
    }

    /**
     * Predict action from feature vector
     */
    fun predict(features: List<Float>): Pair<String, Float> {
        if (!isTrained || classes.isEmpty()) {
            return Pair(UNKNOWN_ACTION, 0f)
        }

        // Forward propagation
        val output = forward(features, useTrainingMode = false)

        // Find class with highest confidence
        val maxIndex = output.indices.maxByOrNull { output[it] } ?: 0
        val confidence = output[maxIndex]

        // Check if confidence exceeds threshold
        return if (confidence >= confidenceThreshold) {
            Pair(indexToClassMap[maxIndex] ?: UNKNOWN_ACTION, confidence)
        } else {
            Pair(UNKNOWN_ACTION, confidence)
        }
    }

    /**
     * Make stable predictions using sliding window
     */
    fun predictWithWindow(
        features: List<Float>,
        previousPredictions: List<Pair<String, Float>>,
        requiredConsensus: Int = 2
    ): Pair<String, Float> {
        // Current prediction
        val currentPrediction = predict(features)

        // Combine with previous predictions
        val allPredictions = previousPredictions + currentPrediction

        // Only consider non-unknown predictions with sufficient confidence
        val validPredictions = allPredictions
            .filter { it.first != UNKNOWN_ACTION && it.second >= confidenceThreshold * 0.95f }

        // Count occurrences of each prediction
        val predictionCounts = validPredictions
            .groupBy { it.first }
            .mapValues { it.value.size }

        // Find most common prediction
        val mostCommon = predictionCounts.maxByOrNull { it.value }

        // Check if consensus threshold is reached
        return if (mostCommon != null && mostCommon.value >= requiredConsensus) {
            // Calculate average confidence for this prediction
            val avgConfidence = validPredictions
                .filter { it.first == mostCommon.key }
                .map { it.second }
                .average()
                .toFloat()

            // Add stability bonus
            val stabilityBonus = minOf(0.1f, 0.02f * mostCommon.value)
            val finalConfidence = minOf(1.0f, avgConfidence + stabilityBonus)

            Pair(mostCommon.key, finalConfidence)
        } else {
            Pair(UNKNOWN_ACTION, 0f)
        }
    }

    /**
     * Get list of trained moves
     */
    fun getTrainedMoves(): List<String> {
        return classes
    }

    /**
     * Check if classifier is trained
     */
    fun isTrained(): Boolean {
        return isTrained
    }

    /**
     * Get confidence threshold
     */
    fun getConfidenceThreshold(): Float {
        return confidenceThreshold
    }

    /**
     * Get feature importance list
     */
    fun getFeatureImportance(): Map<Int, Float> {
        return featureImportance.toMap()
    }

    /**
     * Get diagnostic information
     */
    fun getDiagnosticInfo(): Map<String, Any> {
        val info = mutableMapOf<String, Any>()

        // Basic information
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