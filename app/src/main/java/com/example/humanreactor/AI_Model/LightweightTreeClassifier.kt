package com.example.humanreactor.AI_Model

import android.util.Log
import com.example.humanreactor.customizedMove.NormalizedSample
import kotlin.random.Random

/**
 * Lightweight Decision Tree Forest Classifier
 *
 * Features:
 * - Faster than neural networks, suitable for mobile execution
 * - More powerful than rule-based classifiers, capable of learning more complex patterns
 * - Requires minimal parameter tuning, easy to implement and optimize
 */
class LightweightTreeClassifier(
    private var confidenceThreshold: Float = 0.9f,
    private val numTrees: Int = 50,                // Number of decision trees
    private val maxDepth: Int = 10,                // Maximum tree depth
    private val minSamplesPerLeaf: Int = 3,        // Minimum samples per leaf
    private val featureSamplingRatio: Float = 0.5f // Feature sampling ratio
) {
    companion object {
        private const val TAG = "LightweightTreeClassifier"
        private const val UNKNOWN_ACTION = "unknown"
    }

    // Decision tree forest
    private val forest = mutableListOf<DecisionTree>()

    // Classification labels
    private var classes = listOf<String>()

    // Feature importance
    private val featureImportance = mutableMapOf<Int, Float>()

    // Training status
    private var isTrained = false

    // Number of features
    private var numFeatures = 0

    /**
     * Decision tree node types
     */
    sealed class TreeNode {
        // Internal node (split point)
        data class SplitNode(
            val featureIndex: Int,       // Feature index used for splitting
            val threshold: Float,        // Split threshold
            val leftChild: TreeNode,     // Left subtree (≤ threshold)
            val rightChild: TreeNode,    // Right subtree (> threshold)
            var importance: Float = 0f   // Importance of this split node
        ) : TreeNode()

        // Leaf node (prediction result)
        data class LeafNode(
            val classProbabilities: Map<String, Float> // Probabilities for each class
        ) : TreeNode()
    }

    /**
     * Decision Tree class
     */
    inner class DecisionTree {
        var rootNode: TreeNode = TreeNode.LeafNode(mapOf())
        private val usedFeatureIndices = mutableSetOf<Int>()

        /**
         * Build decision tree
         */
        fun build(
            samples: List<NormalizedSample>,
            availableFeatures: List<Int>,
            currentDepth: Int = 0
        ): TreeNode {
            // Check sample count and depth conditions
            if (samples.isEmpty()) {
                return TreeNode.LeafNode(mapOf())
            }

            // Calculate current node's class distribution
            val classCounts = samples.groupBy { it.label }
                .mapValues { it.value.size.toFloat() }
            val totalSamples = samples.size.toFloat()
            val classProbabilities = classCounts.mapValues { it.value / totalSamples }

            // Check stopping conditions
            if (currentDepth >= maxDepth ||
                samples.size <= minSamplesPerLeaf ||
                classCounts.size <= 1 ||
                availableFeatures.isEmpty()) {
                return TreeNode.LeafNode(classProbabilities)
            }

            // Find best split point
            val bestSplit = findBestSplit(samples, availableFeatures)

            // If no good split point is found, return leaf node
            if (bestSplit.gain <= 0.001f) {
                return TreeNode.LeafNode(classProbabilities)
            }

            // Split data based on best split point
            val (leftSamples, rightSamples) = samples.partition {
                    sample -> sample.features.getOrElse(bestSplit.featureIndex) { 0f } <= bestSplit.threshold
            }

            // Record used features
            usedFeatureIndices.add(bestSplit.featureIndex)

            // Create split node and recursively build subtrees
            return TreeNode.SplitNode(
                featureIndex = bestSplit.featureIndex,
                threshold = bestSplit.threshold,
                importance = bestSplit.gain,
                leftChild = build(leftSamples, availableFeatures, currentDepth + 1),
                rightChild = build(rightSamples, availableFeatures, currentDepth + 1)
            )
        }

        /**
         * Make prediction using decision tree
         */
        fun predict(features: List<Float>): Map<String, Float> {
            // Start from root node
            var currentNode = rootNode

            // Traverse until leaf node is reached
            while (currentNode is TreeNode.SplitNode) {
                val splitNode = currentNode
                val featureIndex = splitNode.featureIndex
                val featureValue = if (featureIndex < features.size) features[featureIndex] else 0f

                // Choose subtree based on feature value
                currentNode = if (featureValue <= splitNode.threshold) {
                    splitNode.leftChild
                } else {
                    splitNode.rightChild
                }
            }

            // Return class probabilities from leaf node
            return (currentNode as TreeNode.LeafNode).classProbabilities
        }

        /**
         * Get feature indices used by the decision tree
         */
        fun getUsedFeatures(): Set<Int> {
            return usedFeatureIndices
        }

        /**
         * Calculate feature importance
         */
        fun calculateFeatureImportance(maxFeatureIndex: Int): Map<Int, Float> {
            val importance = mutableMapOf<Int, Float>()

            // Initialize all feature importance to 0
            for (i in 0 until maxFeatureIndex) {
                importance[i] = 0f
            }

            // Traverse all nodes to calculate importance
            calculateNodeImportance(rootNode, importance)

            return importance
        }

        /**
         * Recursively calculate node importance
         */
        private fun calculateNodeImportance(
            node: TreeNode,
            importance: MutableMap<Int, Float>
        ) {
            when (node) {
                is TreeNode.SplitNode -> {
                    // Accumulate split node importance
                    val featureIndex = node.featureIndex
                    importance[featureIndex] = (importance[featureIndex] ?: 0f) + node.importance

                    // Recursively process child nodes
                    calculateNodeImportance(node.leftChild, importance)
                    calculateNodeImportance(node.rightChild, importance)
                }
                is TreeNode.LeafNode -> {
                    // Leaf nodes don't calculate importance
                }
            }
        }
    }

    /**
     * Split evaluation result
     */
    private data class SplitEvaluation(
        val featureIndex: Int,
        val threshold: Float,
        val gain: Float
    )

    /**
     * Train classifier
     */
    fun train(samples: List<NormalizedSample>): Boolean {
        try {
            Log.d(TAG, "Starting lightweight decision tree classifier training, sample count: ${samples.size}")

            // Check samples
            if (samples.isEmpty()) {
                Log.e(TAG, "No training samples")
                return false
            }

            // Initialize
            forest.clear()
            featureImportance.clear()

            // Get class labels and feature count
            classes = samples.map { it.label }.distinct().filterNot { it == UNKNOWN_ACTION }

            if (samples.firstOrNull()?.features.isNullOrEmpty()) {
                Log.e(TAG, "Sample features are empty")
                return false
            }

            numFeatures = samples.maxOf { it.features.size }

            Log.d(TAG, "Feature count: $numFeatures, class count: ${classes.size}")

            // Random forest training (each tree uses random subset of data)
            for (i in 0 until numTrees) {
                // Randomly select training samples (Bootstrap Sampling)
                val bootstrapSamples = bootstrapSampling(samples)

                // Randomly select feature subset
                val featureIndices = randomFeatureSubset(numFeatures)

                // Build decision tree
                val tree = DecisionTree()
                tree.rootNode = tree.build(bootstrapSamples, featureIndices)

                // Add tree to forest
                forest.add(tree)

                Log.d(TAG, "Completed training decision tree ${i+1}/$numTrees")
            }

            // Calculate feature importance (average of all trees)
            calculateOverallFeatureImportance()

            isTrained = true
            Log.d(TAG, "Lightweight decision tree classifier training completed")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error occurred during training", e)
            return false
        }
    }

    /**
     * Calculate overall feature importance
     */
    private fun calculateOverallFeatureImportance() {
        // Initialize
        for (i in 0 until numFeatures) {
            featureImportance[i] = 0f
        }

        // Accumulate feature importance from all trees
        forest.forEach { tree ->
            val treeImportance = tree.calculateFeatureImportance(numFeatures)
            treeImportance.forEach { (featureIndex, importance) ->
                featureImportance[featureIndex] = (featureImportance[featureIndex] ?: 0f) + importance
            }
        }

        // Average feature importance
        if (forest.isNotEmpty()) {
            featureImportance.keys.forEach { key ->
                featureImportance[key] = featureImportance[key]!! / forest.size
            }
        }

        // Normalize to [0, 1] range
        val maxImportance = featureImportance.values.maxOrNull() ?: 1f
        if (maxImportance > 0) {
            featureImportance.keys.forEach { key ->
                featureImportance[key] = featureImportance[key]!! / maxImportance
            }
        }

        Log.d(TAG, "Top 5 most important features: ${
            featureImportance.entries.sortedByDescending { it.value }.take(5)
                .joinToString(", ") { "${it.key}: ${it.value}" }
        }")
    }

    /**
     * Randomly select feature subset
     */
    private fun randomFeatureSubset(numFeatures: Int): List<Int> {
        // Determine number of features to select
        val numFeaturesToSelect = (numFeatures * featureSamplingRatio).toInt().coerceAtLeast(1)

        // Generate list of all feature indices and shuffle randomly
        return (0 until numFeatures).shuffled().take(numFeaturesToSelect)
    }

    /**
     * Bootstrap sampling
     */
    private fun bootstrapSampling(samples: List<NormalizedSample>): List<NormalizedSample> {
        val result = mutableListOf<NormalizedSample>()
        val numSamples = samples.size

        // Randomly select samples with replacement
        repeat(numSamples) {
            val index = Random.nextInt(numSamples)
            result.add(samples[index])
        }

        return result
    }

    /**
     * Find best split point
     */
    private fun findBestSplit(
        samples: List<NormalizedSample>,
        availableFeatures: List<Int>
    ): SplitEvaluation {
        // Initialize best split evaluation result
        var bestSplit = SplitEvaluation(0, 0f, 0f)

        // Current node entropy (impurity)
        val currentEntropy = calculateEntropy(samples)

        // Evaluate split points for each available feature
        for (featureIndex in availableFeatures) {
            // Get all values for this feature
            val featureValues = samples.mapNotNull {
                if (featureIndex < it.features.size) it.features[featureIndex] else null
            }

            if (featureValues.isEmpty()) continue

            // Select some candidate split thresholds
            val uniqueValues = featureValues.distinct().sorted()
            val candidateThresholds = mutableListOf<Float>()

            // If too many unique values, select evenly spaced points
            if (uniqueValues.size > 10) {
                val step = uniqueValues.size / 10
                for (i in 0 until uniqueValues.size step step) {
                    candidateThresholds.add(uniqueValues[i])
                }
            } else {
                candidateThresholds.addAll(uniqueValues)
            }

            // Evaluate each candidate threshold
            for (threshold in candidateThresholds) {
                // Split samples based on threshold
                val (leftSamples, rightSamples) = samples.partition {
                        sample ->
                    val value = if (featureIndex < sample.features.size) sample.features[featureIndex] else 0f
                    value <= threshold
                }

                // Skip if split is unbalanced
                if (leftSamples.isEmpty() || rightSamples.isEmpty()) continue

                // Calculate entropy for left and right child nodes
                val leftEntropy = calculateEntropy(leftSamples)
                val rightEntropy = calculateEntropy(rightSamples)

                // Calculate information gain
                val leftWeight = leftSamples.size.toFloat() / samples.size
                val rightWeight = rightSamples.size.toFloat() / samples.size
                val weightedEntropy = leftWeight * leftEntropy + rightWeight * rightEntropy
                val gain = currentEntropy - weightedEntropy

                // Update best split point
                if (gain > bestSplit.gain) {
                    bestSplit = SplitEvaluation(featureIndex, threshold, gain)
                }
            }
        }

        return bestSplit
    }

    /**
     * Calculate entropy (impurity measure)
     */
    private fun calculateEntropy(samples: List<NormalizedSample>): Float {
        if (samples.isEmpty()) return 0f

        // Calculate sample count for each class
        val classCounts = samples.groupBy { it.label }
            .mapValues { it.value.size.toFloat() / samples.size }

        // Calculate entropy
        var entropy = 0.0
        for (p in classCounts.values) {
            if (p > 0) {
                entropy -= p * kotlin.math.ln(p)
            }
        }
        return entropy.toFloat()
    }

    /**
     * Predict action
     */
    fun predict(features: List<Float>): Pair<String, Float> {
        if (!isTrained || forest.isEmpty()) {
            return Pair(UNKNOWN_ACTION, 0f)
        }

        // Get predictions from all trees in the forest
        val predictions = forest.map { tree -> tree.predict(features) }

        // Combine prediction results from all trees
        val combinedPrediction = mutableMapOf<String, Float>()

        // Initialize probabilities for all classes to 0
        classes.forEach { className ->
            combinedPrediction[className] = 0f
        }

        // Accumulate probabilities from all trees
        predictions.forEach { prediction ->
            prediction.forEach { (className, probability) ->
                combinedPrediction[className] = (combinedPrediction[className] ?: 0f) + probability
            }
        }

        // Average probabilities
        combinedPrediction.keys.forEach { className ->
            combinedPrediction[className] = combinedPrediction[className]!! / forest.size
        }

        // Find class with highest probability
        val bestPrediction = combinedPrediction.maxByOrNull { it.value }
            ?: return Pair(UNKNOWN_ACTION, 0f)

        // Check if confidence exceeds threshold
        return if (bestPrediction.value >= confidenceThreshold) {
            Pair(bestPrediction.key, bestPrediction.value)
        } else {
            Pair(UNKNOWN_ACTION, bestPrediction.value)
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
        info["numTrees"] = forest.size
        info["maxDepth"] = maxDepth
        info["confidenceThreshold"] = confidenceThreshold
        info["numFeatures"] = numFeatures
        info["numImportantFeatures"] = featureImportance.count { it.value > 0.5f }

        return info
    }
}