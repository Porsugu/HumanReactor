package com.example.humanreactor.AI_Model

import android.util.Log
import com.example.humanreactor.customizedMove.NormalizedSample

/**
 * Improved rule-based classifier that uses normalized samples
 */
class ImprovedRuleBasedClassifier(
    private val confidenceThreshold: Float = 0.9f
) {
    companion object {
        private const val TAG = "ImprovedRuleBasedClassifier"
        private const val UNKNOWN_ACTION = "unknown"

        // Distance threshold used for feature weight calculation
        private const val FEATURE_SIGNIFICANCE_THRESHOLD = 0.1f
//        0.05 0.03
    }

    // Feature definitions for each move
    private val moveDefinitions = mutableMapOf<String, MoveFeatureDefinition>()

    // Feature importance scores
    private val featureImportance = mutableMapOf<Int, Float>()

    // Structured representation of a move's feature definitions
    data class MoveFeatureDefinition(
        val name: String,
        val featureRanges: Map<Int, EnhancedFeatureRange>
    )

    // Enhanced feature range with distribution information
    data class EnhancedFeatureRange(
        val min: Float,        // Minimum value
        val max: Float,        // Maximum value
        val q1: Float,         // First quartile
        val median: Float,     // Median
        val q3: Float,         // Third quartile
        val stdDev: Float,     // Standard deviation
        val weight: Float      // Feature weight
    ) {
        // Check if value is within range
        fun contains(value: Float): Boolean = value in min..max

        // Calculate match score (0.0 to 1.0)
        fun getMatchScore(value: Float): Float {
            // Core region (IQR - interquartile range) - highest score
            if (value in q1..q3) return 1.0f

            // General range - high score
            if (value in min..max) return 0.8f

            // Outside range - calculate decay score based on standard deviation
            val distance = when {
                value < min -> min - value
                else -> value - max
            }

            // Use standard deviation as decay factor
            return if (distance <= stdDev * 2) {
                0.8f * (1.0f - (distance / (stdDev * 2)))
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
            featureImportance.clear()

            // Group samples by move name
            val samplesByMove = samples.groupBy { it.label }

            // Ensure we have enough samples
            if (samplesByMove.isEmpty()) return false

            // Get number of features
            val numFeatures = samples.firstOrNull()?.features?.size ?: return false

            // Calculate feature importance (feature discriminative power)
            calculateFeatureImportance(samplesByMove, numFeatures)

            // Calculate feature definitions for each move
            for ((moveName, moveSamples) in samplesByMove) {
                // Skip if no samples
                if (moveSamples.isEmpty()) continue

                // Calculate feature ranges for this move
                val featureRanges = calculateEnhancedFeatureRanges(moveSamples)

                // Register move definition
                moveDefinitions[moveName] = MoveFeatureDefinition(
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
     * Calculate feature importance (measures each feature's ability to discriminate between different moves)
     */
    private fun calculateFeatureImportance(
        samplesByMove: Map<String, List<NormalizedSample>>,
        numFeatures: Int
    ) {
        // For each feature...
        for (featureIndex in 0 until numFeatures) {
            // Calculate feature means for each move
            val moveFeatureMeans = samplesByMove.mapValues { (_, samples) ->
                samples.map { it.features[featureIndex] }.average().toFloat()
            }

            // If only one move, set low importance
            if (moveFeatureMeans.size <= 1) {
                featureImportance[featureIndex] = 0.5f
                continue
            }

            // Calculate global mean
            val globalMean = moveFeatureMeans.values.average()

            // Calculate between-class variance
            val betweenClassVariance = moveFeatureMeans.values
                .map { (it - globalMean) * (it - globalMean) }
                .average()

            // Calculate within-class variance
            val withinClassVariance = samplesByMove.map { (moveName, samples) ->
                val moveMean = moveFeatureMeans[moveName] ?: 0f
                samples.map {
                    val value = it.features[featureIndex]
                    (value - moveMean) * (value - moveMean)
                }.average()
            }.average()

            // Calculate F-score (between-class variance / within-class variance)
            val fScore = if (withinClassVariance > 0) {
                (betweenClassVariance / withinClassVariance).toFloat()
            } else {
                10f  // Avoid division by zero, assign high score
            }

            // Normalize to 0-1 weight
            featureImportance[featureIndex] = minOf(1f, fScore / 10f)
        }

        Log.d(TAG, "Calculated ${featureImportance.size} feature importance scores")
    }

    /**
     * Calculate enhanced feature ranges from samples
     */
    private fun calculateEnhancedFeatureRanges(samples: List<NormalizedSample>): Map<Int, EnhancedFeatureRange> {
        val featureRanges = mutableMapOf<Int, EnhancedFeatureRange>()

        // Ensure we have feature values to process
        if (samples.isEmpty() || samples.first().features.isEmpty()) {
            return featureRanges
        }

        // Get number of features
        val numFeatures = samples.first().features.size

        // For each feature index...
        for (featureIndex in 0 until numFeatures) {
            // Get all values for this feature across samples
            val featureValues = samples.map { it.features[featureIndex] }

            // Skip if no values
            if (featureValues.isEmpty()) continue

            // Get feature importance score
            val importance = featureImportance[featureIndex] ?: 0.5f

            // Only include features with meaningful variation
            val variance = calculateVariance(featureValues)
            if (variance < FEATURE_SIGNIFICANCE_THRESHOLD && importance < 0.3f) {
                continue
            }

            // Calculate distribution statistics
            val min = featureValues.minOrNull()!!
            val max = featureValues.maxOrNull()!!

            // Sort for quartile calculations
            val sortedValues = featureValues.sorted()
            val q1 = sortedValues[sortedValues.size / 4]
            val median = sortedValues[sortedValues.size / 2]
            val q3 = sortedValues[sortedValues.size * 3 / 4]

            // Calculate standard deviation
            val mean = featureValues.average().toFloat()
            val stdDev = kotlin.math.sqrt(
                featureValues.map { (it - mean) * (it - mean) }.sum() / featureValues.size
            ).toFloat()

            // Calculate feature weight (combining importance and variance)
            val weight = importance * (1 + variance).coerceAtMost(2f)

            // Create enhanced feature range
            featureRanges[featureIndex] = EnhancedFeatureRange(
                min = min,
                max = max,
                q1 = q1,
                median = median,
                q3 = q3,
                stdDev = stdDev,
                weight = weight
            )
        }

        return featureRanges
    }

    /**
     * Calculate variance of a list of feature values
     */
    private fun calculateVariance(values: List<Float>): Float {
        if (values.isEmpty()) return 0f

        val mean = values.average().toFloat()
        val sumSquaredDiff = values.sumOf { ((it - mean) * (it - mean)).toDouble() }
        return (sumSquaredDiff / values.size).toFloat()
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
            // Get all relevant features with match scores and weights
            val scoresAndWeights = definition.featureRanges.map { (featureIndex, range) ->
                // Ensure feature index is valid
                if (featureIndex < features.size) {
                    // Get feature value and calculate match score
                    val featureValue = features[featureIndex]
                    Pair(range.getMatchScore(featureValue), range.weight)
                } else {
                    Pair(0f, 0f)
                }
            }

            // Calculate weighted average score
            val totalWeight = scoresAndWeights.sumOf { it.second.toDouble() }
            val overallScore = if (totalWeight > 0) {
                scoresAndWeights.sumOf { (score, weight) ->
                    (score * weight).toDouble()
                }.toFloat() / totalWeight.toFloat()
            } else {
                0f
            }

            Pair(name, overallScore)
        }

        // Find best match
        val bestMatch = moveScores.maxByOrNull { it.second }
            ?: return Pair(UNKNOWN_ACTION, 0f)

        // Calculate gap between best and second-best match
        val secondBest = moveScores
            .filter { it.first != bestMatch.first }
            .maxByOrNull { it.second }

        // If there's a significant advantage, enhance confidence score
        val enhancedConfidence = if (secondBest != null) {
            val margin = bestMatch.second - secondBest.second
            val bonus = minOf(0.15f, margin * 2) // Max 15% confidence boost
            minOf(1.0f, bestMatch.second + bonus)
        } else {
            bestMatch.second
        }

        // Check if confidence exceeds threshold
        return if (enhancedConfidence >= confidenceThreshold) {
            Pair(bestMatch.first, enhancedConfidence)
        } else {
            Pair(UNKNOWN_ACTION, enhancedConfidence)
        }
    }

    /**
     * Predict with stability window
     */
    fun predictWithWindow(
        features: List<Float>,
        previousPredictions: List<Pair<String, Float>>,
        requiredConsensus: Int = 2
    ): Pair<String, Float> {
        // Make current prediction
        val currentPrediction = predict(features)

        // Combine with previous predictions
        val allPredictions = previousPredictions + currentPrediction

        // Only consider non-unknown predictions with sufficient confidence
        val validPredictions = allPredictions
            .filter { it.first != UNKNOWN_ACTION && it.second >= confidenceThreshold * 1.0f }

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
        return moveDefinitions.keys.toList()
    }

    /**
     * Check if the classifier is trained
     */
    fun isTrained(): Boolean {
        return moveDefinitions.isNotEmpty()
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
     * Get diagnostic information for move definitions
     */
    fun getDiagnosticInfo(): Map<String, Any> {
        val info = mutableMapOf<String, Any>()

        // Add basic information
        info["numMoves"] = moveDefinitions.size
        info["confidenceThreshold"] = confidenceThreshold
        info["numImportantFeatures"] = featureImportance.count { it.value > 0.5f }

        // Add feature counts for each move
        val moveFeatureCounts = moveDefinitions.mapValues { it.value.featureRanges.size }
        info["moveFeatureCounts"] = moveFeatureCounts

        return info
    }
}