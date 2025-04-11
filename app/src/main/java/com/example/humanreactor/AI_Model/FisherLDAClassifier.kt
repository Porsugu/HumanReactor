package com.example.humanreactor.AI_Model

import com.example.humanreactor.customizedMove.NormalizedSample
import com.google.mlkit.vision.pose.Pose
import kotlin.math.exp
import kotlin.math.pow

/**
 * Classifier implementing Fisher Linear Discriminant Analysis
 * Supports training on normalized samples and predicting labels with confidence scores
 */
class FisherLDAClassifier(
    private val confidenceThreshold: Float = 0.9f
) {
    private var isTrained: Boolean = false
    private var labelSet: Set<String> = setOf()
    private var means: MutableMap<String, FloatArray> = mutableMapOf()
    private var globalMean: FloatArray = floatArrayOf()
    private var projectionVectors: MutableMap<String, FloatArray> = mutableMapOf()
    private var featureSize: Int = 0

    /**
     * Train the classifier on a list of normalized samples
     * @param samples List of normalized samples with features and labels
     * @return Boolean indicating success of training
     */
    fun train(samples: List<NormalizedSample>): Boolean {
        if (samples.isEmpty()) {
            return false
        }

        // Get unique labels
        labelSet = samples.map { it.label }.toSet()
        if (labelSet.size < 2) {
            // Need at least two classes for discrimination
            return false
        }

        featureSize = samples[0].features.size

        // Calculate means for each class and global mean
        calculateMeans(samples)

        // Calculate within-class scatter matrix
        val sw = calculateWithinClassScatter(samples)

        // Calculate between-class scatter matrix
        val sb = calculateBetweenClassScatter()

        // Calculate projection vectors for each class
        for (label in labelSet) {
            val classMean = means[label] ?: continue

            // For two-class problems, a single projection vector would be sufficient
            // For multi-class, we use one-vs-rest approach
            val projectionVector = calculateProjectionVector(sw, classMean, globalMean)
            projectionVectors[label] = projectionVector
        }

        isTrained = true
        return true
    }

    /**
     * Predict the label and confidence for given features
     * @param features List of features to predict
     * @return Pair of predicted label (or "unknown") and confidence score
     */
    fun predict(features: List<Float>): Pair<String, Float> {
        if (!isTrained || features.size != featureSize) {
            return Pair("unknown", 0.0f)
        }

        val featuresArray = features.toFloatArray()

        var bestLabel = "unknown"
        var highestConfidence = -1.0f

        // Calculate projection and confidence for each class
        for (label in labelSet) {
            val projVector = projectionVectors[label] ?: continue
            val classMean = means[label] ?: continue

            // Project the features onto the discriminant vector
            val projection = project(featuresArray, projVector)

            // Calculate the class mean projection
            val meanProjection = project(classMean, projVector)

            // Calculate confidence based on distance to projected class mean
            val distance = Math.abs(projection - meanProjection)
            val confidence = calculateConfidence(distance)

            if (confidence > highestConfidence && confidence >= confidenceThreshold) {
                highestConfidence = confidence
                bestLabel = label
            }
        }

        return Pair(bestLabel, highestConfidence)
    }

    /**
     * Predict with a sliding window approach, requiring consensus among multiple predictions
     * @param features Current features to predict
     * @param previousPredictions List of previous predictions (pairs of label and confidence)
     * @param requiredConsensus Number of consistent predictions required for consensus
     * @return Pair of predicted label (or "unknown") and confidence score
     */
    fun predictWithWindow(
        features: List<Float>,
        previousPredictions: List<Pair<String, Float>>,
        requiredConsensus: Int = 2
    ): Pair<String, Float> {
        // Make current prediction
        val currentPrediction = predict(features)

        // If current prediction is unknown, return it immediately
        if (currentPrediction.first == "unknown") {
            return currentPrediction
        }

        // Combine current prediction with previous ones (extract the labels from the pairs)
        val allPredictionLabels = previousPredictions.map { it.first } + currentPrediction.first

        // Count occurrences of each label
        val labelCounts = allPredictionLabels.groupingBy { it }.eachCount()

        // Find the most common label that meets the consensus requirement
        val consensusLabel = labelCounts.entries
            .filter { it.value >= requiredConsensus }
            .maxByOrNull { it.value }
            ?.key ?: "unknown"

        // If there's a consensus, return it with the current confidence
        // Otherwise, return "unknown" with a reduced confidence
        return if (consensusLabel != "unknown") {
            Pair(consensusLabel, currentPrediction.second)
        } else {
            Pair("unknown", currentPrediction.second * 0.5f)
        }
    }

    /**
     * Calculate class means and global mean
     */
    private fun calculateMeans(samples: List<NormalizedSample>) {
        // Initialize means map
        means.clear()
        labelSet.forEach { label ->
            means[label] = FloatArray(featureSize) { 0f }
        }

        // Initialize global mean
        globalMean = FloatArray(featureSize) { 0f }

        // Calculate sums for each class
        val classCounts = mutableMapOf<String, Int>()
        for (sample in samples) {
            val label = sample.label
            val count = classCounts.getOrDefault(label, 0) + 1
            classCounts[label] = count

            val mean = means[label] ?: continue
            for (i in sample.features.indices) {
                mean[i] += sample.features[i]
                globalMean[i] += sample.features[i]
            }
        }

        // Calculate means by dividing by counts
        for (label in labelSet) {
            val count = classCounts[label] ?: continue
            val meanArray = means[label] ?: continue

            // Create a new array with calculated means
            val newMeanArray = FloatArray(meanArray.size) { i ->
                meanArray[i] / count
            }

            // Update the means map with the new mean array
            means[label] = newMeanArray
        }

        // Calculate global mean
        globalMean = FloatArray(featureSize) { i ->
            globalMean[i] / samples.size
        }
    }

    /**
     * Calculate within-class scatter matrix
     */
    private fun calculateWithinClassScatter(samples: List<NormalizedSample>): Array<FloatArray> {
        val sw = Array(featureSize) { FloatArray(featureSize) { 0f } }

        for (sample in samples) {
            val classMean = means[sample.label] ?: continue
            val diff = FloatArray(featureSize) { i -> sample.features[i] - classMean[i] }

            // Outer product of diff with itself
            for (i in 0 until featureSize) {
                for (j in 0 until featureSize) {
                    sw[i][j] += diff[i] * diff[j]
                }
            }
        }

        return sw
    }

    /**
     * Calculate between-class scatter matrix
     */
    private fun calculateBetweenClassScatter(): Array<FloatArray> {
        val sb = Array(featureSize) { FloatArray(featureSize) { 0f } }

        for (label in labelSet) {
            val classMean = means[label] ?: continue
            val diff = FloatArray(featureSize) { i -> classMean[i] - globalMean[i] }

            // Outer product of diff with itself
            for (i in 0 until featureSize) {
                for (j in 0 until featureSize) {
                    sb[i][j] += diff[i] * diff[j]
                }
            }
        }

        return sb
    }

    /**
     * Calculate the projection vector for a class
     * For simplicity, we use a direct calculation that may work for small feature spaces
     * For larger feature spaces, more sophisticated matrix operations would be required
     */
    private fun calculateProjectionVector(
        sw: Array<FloatArray>,
        classMean: FloatArray,
        globalMean: FloatArray
    ): FloatArray {
        val projVector = FloatArray(featureSize) { 0f }
        val diff = FloatArray(featureSize) { i -> classMean[i] - globalMean[i] }

        // Simplified approach: use the difference between class mean and global mean
        // For a more accurate LDA, we would need to calculate Sw^-1 * (mean_diff)
        // But matrix inversion is complex, so we use a simplified approach here

        // Regularized pseudo-inverse approximation
        val swRegularized = regularizeMatrix(sw)

        // Apply regularized inverse to the mean difference
        for (i in 0 until featureSize) {
            for (j in 0 until featureSize) {
                projVector[i] += swRegularized[i][j] * diff[j]
            }
        }

        // Normalize the projection vector
        val magnitude = Math.sqrt(projVector.sumOf { it.toDouble().pow(2) }).toFloat()
        for (i in projVector.indices) {
            projVector[i] /= magnitude
        }

        return projVector
    }

    /**
     * Regularize the matrix by adding a small value to the diagonal
     * This is a simple approach to make the matrix more invertible
     */
    private fun regularizeMatrix(matrix: Array<FloatArray>): Array<FloatArray> {
        val regularized = Array(featureSize) { i ->
            FloatArray(featureSize) { j ->
                if (i == j) matrix[i][j] + 0.001f else matrix[i][j]
            }
        }

        // Return a simplified pseudo-inverse (for demonstration purposes)
        // In a real implementation, you would use a proper matrix library
        return simplifiedPseudoInverse(regularized)
    }

    /**
     * Very simplified pseudo-inverse calculation
     * This is not a true matrix inverse but works for demonstration
     * In practice, use a proper linear algebra library
     */
    private fun simplifiedPseudoInverse(matrix: Array<FloatArray>): Array<FloatArray> {
        val result = Array(featureSize) { FloatArray(featureSize) { 0f } }

        // Extremely simplified approach - just take reciprocals of diagonal
        // and scale off-diagonal elements
        for (i in 0 until featureSize) {
            for (j in 0 until featureSize) {
                if (i == j) {
                    result[i][j] = 1.0f / matrix[i][j]
                } else {
                    result[i][j] = -matrix[i][j] / (matrix[i][i] * matrix[j][j])
                }
            }
        }

        return result
    }

    /**
     * Project features onto a vector
     */
    private fun project(features: FloatArray, vector: FloatArray): Float {
        var result = 0.0f
        for (i in features.indices) {
            result += features[i] * vector[i]
        }
        return result
    }

    /**
     * Calculate confidence score based on distance
     * Uses a negative exponential transformation to convert distance to confidence
     */
    private fun calculateConfidence(distance: Float): Float {
        return exp(-2.0f * distance).toFloat()
    }

    fun isTrained():Boolean{return isTrained}
    fun getConfidenceThreshold():Float{return confidenceThreshold}
}

