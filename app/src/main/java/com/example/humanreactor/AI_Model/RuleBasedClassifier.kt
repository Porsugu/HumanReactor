package com.example.humanreactor.AI_Model

import android.util.Log
import com.example.humanreactor.customizedMove.NormalizedSample

/**
 * Rule-based classifier that uses normalized samples
 */
class RuleBasedClassifier(
    private val confidenceThreshold: Float = 0.98f
) {
    companion object {
        private const val TAG = "RuleBasedClassifier"
        private const val UNKNOWN_ACTION = "unknown"
    }

    // Feature definitions for each move
    private val moveDefinitions = mutableMapOf<String, MoveFeatureRanges>()

    // Structured representation of a move's feature ranges
    data class MoveFeatureRanges(
        val name: String,
        val featureRanges: Map<Int, FeatureRange>
    )

    // Range for a single feature
    data class FeatureRange(val min: Float, val max: Float) {
        fun contains(value: Float): Boolean = value in min..max

        // Calculate how well a value fits in this range (0.0 to 1.0)
        fun getMatchScore(value: Float): Float {
            if (value in min..max) return 1.0f

            // Calculate how far outside the range the value is
            val distance = when {
                value < min -> min - value
                else -> value - max
            }

            // Range width
            val width = max - min

            // If value is within 50% of the range width outside the range,
            // calculate a partial score, otherwise 0
            return if (distance <= width * 0.5f) {
                1.0f - (distance / (width * 0.5f))
            } else {
                0.0f
            }
        }
    }

    /**
     * Train the classifier using normalized samples
     */
    fun train(samples: List<NormalizedSample>): Boolean {
        try {
            moveDefinitions.clear()

            // Group samples by move name
            val samplesByMove = samples.groupBy { it.label }

            for ((moveName, moveSamples) in samplesByMove) {
                // Skip if no samples
                if (moveSamples.isEmpty()) continue

                // Calculate feature ranges for this move
                val featureRanges = calculateFeatureRanges(moveSamples)

                // Register move definition
                moveDefinitions[moveName] = MoveFeatureRanges(
                    name = moveName,
                    featureRanges = featureRanges
                )

                Log.d(TAG, "Registered move $moveName with ${featureRanges.size} feature ranges")
            }

            return moveDefinitions.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error training classifier", e)
            return false
        }
    }

    /**
     * Calculate feature ranges from samples
     */
    private fun calculateFeatureRanges(samples: List<NormalizedSample>): Map<Int, FeatureRange> {
        val featureRanges = mutableMapOf<Int, FeatureRange>()

        // Ensure we have feature values to process
        if (samples.isEmpty() || samples.first().features.isEmpty()) {
            return featureRanges
        }

        // Get number of features
        val numFeatures = samples.first().features.size

        // For each feature index
        for (featureIndex in 0 until numFeatures) {
            // Get all values for this feature across samples
            val featureValues = samples.map { it.features[featureIndex] }

            // Skip if no values
            if (featureValues.isEmpty()) continue

            // Calculate min and max with some margin
            val margin = 0.1f  // Add 10% margin
            val minVal = featureValues.minOrNull()!! - margin
            val maxVal = featureValues.maxOrNull()!! + margin

            // Only include features with meaningful variation
            if (maxVal - minVal > 0.01f) {
                featureRanges[featureIndex] = FeatureRange(minVal, maxVal)
            }
        }

        return featureRanges
    }

    /**
     * Predict action from normalized features
     */
    fun predict(features: List<Float>): Pair<String, Float> {
        if (moveDefinitions.isEmpty()) {
            return Pair(UNKNOWN_ACTION, 0f)
        }

        // Calculate match scores for each move
        val moveScores = moveDefinitions.map { (name, definition) ->
            // Get all relevant features for this move
            val scores = definition.featureRanges.map { (featureIndex, range) ->
                // Ensure feature index is valid
                if (featureIndex < features.size) {
                    // Get feature value and calculate match score
                    val featureValue = features[featureIndex]
                    range.getMatchScore(featureValue)
                } else {
                    0f
                }
            }

            // Calculate overall score (average of feature scores)
            val overallScore = if (scores.isEmpty()) 0f else scores.sum() / scores.size

            Pair(name, overallScore)
        }

        // Find best match
        val bestMatch = moveScores.maxByOrNull { it.second }
            ?: return Pair(UNKNOWN_ACTION, 0f)

        // Check if confidence exceeds threshold
        return if (bestMatch.second >= confidenceThreshold) {
            bestMatch
        } else {
            Pair(UNKNOWN_ACTION, bestMatch.second)
        }
    }

    /**
     * Predict with stability window
     */
    fun predictWithWindow(features: List<Float>, previousPredictions: List<Pair<String, Float>>,
                          requiredConsensus: Int = 2): Pair<String, Float> {
        // Make current prediction
        val currentPrediction = predict(features)

        // Combine with previous predictions
        val allPredictions = previousPredictions + currentPrediction

        // Only consider non-unknown predictions with sufficient confidence
        val validPredictions = allPredictions
            .filter { it.first != UNKNOWN_ACTION }

        // Count occurrences of each prediction
        val predictionCounts = validPredictions
            .groupBy { it.first }
            .mapValues { it.value.size }

        // Find most common prediction
        val mostCommon = predictionCounts.maxByOrNull { it.value }

        // Check if we have sufficient consensus
        return if (mostCommon != null && mostCommon.value >= requiredConsensus) {
            // Calculate average confidence for this prediction
            val avgConfidence = validPredictions
                .filter { it.first == mostCommon.key }
                .map { it.second }
                .average()
                .toFloat()

            Pair(mostCommon.key, avgConfidence)
        } else {
            Pair(UNKNOWN_ACTION, 0f)
        }
    }

    /**
     * Get list of trained moves
     */
    fun getTrainedMoves(): List<String> {
        return moveDefinitions.keys.toList()
    }

    /**
     * Check if the classifier is trained
     */
    fun isTrained(): Boolean {
        return moveDefinitions.isNotEmpty()
    }

    fun getConfidenceThreshold():Float{
        return confidenceThreshold
    }
}