//package com.example.humanreactor.AI_Model
//
//import android.util.Log
//import com.example.humanreactor.customizedMove.NormalizedSample
//
///**
// * Improved rule-based classifier that uses normalized samples
// */
//class ImprovedRuleBasedClassifier(
//    private val confidenceThreshold: Float = 0.90f
//) {
//    companion object {
//        private const val TAG = "ImprovedRuleBasedClassifier"
//        private const val UNKNOWN_ACTION = "unknown"
//
//        // Distance threshold used for feature weight calculation
//        private const val FEATURE_SIGNIFICANCE_THRESHOLD = 0.1f
////        0.05 0.03
//    }
//
//    // Feature definitions for each move
//    private val moveDefinitions = mutableMapOf<String, MoveFeatureDefinition>()
//
//    // Feature importance scores
//    private val featureImportance = mutableMapOf<Int, Float>()
//
//    // Structured representation of a move's feature definitions
//    data class MoveFeatureDefinition(
//        val name: String,
//        val featureRanges: Map<Int, EnhancedFeatureRange>
//    )
//
//    // Enhanced feature range with distribution information
//    data class EnhancedFeatureRange(
//        val min: Float,        // Minimum value
//        val max: Float,        // Maximum value
//        val q1: Float,         // First quartile
//        val median: Float,     // Median
//        val q3: Float,         // Third quartile
//        val stdDev: Float,     // Standard deviation
//        val weight: Float      // Feature weight
//    ) {
//        // Check if value is within range
//        fun contains(value: Float): Boolean = value in min..max
//
//        // Calculate match score (0.0 to 1.0)
//        fun getMatchScore(value: Float): Float {
//            // Core region (IQR - interquartile range) - highest score
//            if (value in q1..q3) return 1.0f
//
//            // General range - high score
//            if (value in min..max) return 0.8f
//
//            // Outside range - calculate decay score based on standard deviation
//            val distance = when {
//                value < min -> min - value
//                else -> value - max
//            }
//
//            // Use standard deviation as decay factor
//            return if (distance <= stdDev * 2) {
//                0.8f * (1.0f - (distance / (stdDev * 2)))
//            } else {
//                0.0f
//            }
//        }
//    }
//
//    /**
//     * Train the classifier using normalized samples
//     */
//    fun train(samples: List<NormalizedSample>): Boolean {
//        try {
//            moveDefinitions.clear()
//            featureImportance.clear()
//
//            // Group samples by move name
//            val samplesByMove = samples.groupBy { it.label }
//
//            // Ensure we have enough samples
//            if (samplesByMove.isEmpty()) return false
//
//            // Get number of features
//            val numFeatures = samples.firstOrNull()?.features?.size ?: return false
//            Log.d(TAG, "numFeatures:  ${numFeatures}")
//
//            // Calculate feature importance (feature discriminative power)
//            calculateFeatureImportance(samplesByMove, numFeatures)
//            Log.d(TAG, "calculateFeatureImportance done")
//            // Calculate feature definitions for each move
//            for ((moveName, moveSamples) in samplesByMove) {
//                // Skip if no samples
//                if (moveSamples.isEmpty()) continue
//
//                // Calculate feature ranges for this move
//                val featureRanges = calculateEnhancedFeatureRanges(moveSamples)
//
//                // Register move definition
//                moveDefinitions[moveName] = MoveFeatureDefinition(
//                    name = moveName,
//                    featureRanges = featureRanges
//                )
//
//                Log.d(TAG, "Registered move $moveName with ${featureRanges.size} feature ranges")
//            }
//
//            return moveDefinitions.isNotEmpty()
//        } catch (e: Exception) {
//            Log.e(TAG, "Error training classifier", e)
//            return false
//        }
//    }
//
//    /**
//     * Calculate feature importance (measures each feature's ability to discriminate between different moves)
//     */
//    private fun calculateFeatureImportance(samplesByMove: Map<String, List<NormalizedSample>>, numFeatures: Int) {
//        // Log initial information
//        Log.d(TAG, "Starting calculateFeatureImportance with $numFeatures features")
//        Log.d(TAG, "Number of moves: ${samplesByMove.size}")
//
//        // Log samples per move
//        samplesByMove.forEach { (moveName, samples) ->
//            Log.d(TAG, "Move: $moveName has ${samples.size} samples")
//
//            // Log the first sample for each move if available
//            if (samples.isNotEmpty()) {
//                val firstSample = samples.first()
//                Log.d(TAG, "  First sample label: ${firstSample.label}")
//                Log.d(TAG, "  First sample features size: ${firstSample.features.size}")
//                Log.d(TAG, "  First sample features: ${firstSample.features}")
//            } else {
//                Log.d(TAG, "  WARNING: No samples for move $moveName")
//            }
//        }
//
//        // For each feature...
//        for (featureIndex in 0 until numFeatures) {
//            Log.d(TAG, "Processing feature index: $featureIndex")
//
//            try {
//                // Calculate feature means for each move
//                val moveFeatureMeans = mutableMapOf<String, Float>()
//
//                samplesByMove.forEach { (moveName, samples) ->
//                    if (samples.isEmpty()) {
//                        Log.d(TAG, "  Move $moveName has no samples, using default mean value")
//                        moveFeatureMeans[moveName] = 0.0f
//                    } else {
//                        try {
//                            // Check if all samples have enough features
//                            val invalidSamples = samples.filter { it.features.size <= featureIndex }
//                            if (invalidSamples.isNotEmpty()) {
//                                Log.d(TAG, "  WARNING: ${invalidSamples.size} samples in move $moveName don't have feature at index $featureIndex")
//                                Log.d(TAG, "  First invalid sample features size: ${invalidSamples.firstOrNull()?.features?.size ?: 0}")
//                            }
//
//                            // Only use valid samples
//                            val validSamples = samples.filter { it.features.size > featureIndex }
//                            if (validSamples.isEmpty()) {
//                                Log.d(TAG, "  No valid samples for move $moveName at feature index $featureIndex, using default")
//                                moveFeatureMeans[moveName] = 0.0f
//                            } else {
//                                val mean = validSamples.map { it.features[featureIndex] }.average().toFloat()
//                                moveFeatureMeans[moveName] = mean
//                                Log.d(TAG, "  Move $moveName mean for feature $featureIndex: $mean")
//                            }
//                        } catch (e: Exception) {
//                            Log.e(TAG, "  Error calculating mean for move $moveName at feature $featureIndex", e)
//                            moveFeatureMeans[moveName] = 0.0f
//                        }
//                    }
//                }
//
//                // If only one move, set low importance
//                if (moveFeatureMeans.size <= 1) {
//                    Log.d(TAG, "  Only ${moveFeatureMeans.size} moves, setting low importance (0.5)")
//                    featureImportance[featureIndex] = 0.5f
//                    continue
//                }
//
//                // Calculate global mean
//                val globalMean = moveFeatureMeans.values.average()
//                Log.d(TAG, "  Global mean for feature $featureIndex: $globalMean")
//
//                // Calculate between-class variance
//                val betweenClassVariance = moveFeatureMeans.values
//                    .map { (it - globalMean) * (it - globalMean) }
//                    .average()
//                Log.d(TAG, "  Between-class variance: $betweenClassVariance")
//
//                // Calculate within-class variance with error handling
//                var withinClassVariance = 0.0
//                var validMoveCount = 0
//
//                samplesByMove.forEach { (moveName, samples) ->
//                    try {
//                        val moveMean = moveFeatureMeans[moveName] ?: 0f
//
//                        if (samples.isEmpty()) {
//                            Log.d(TAG, "  Move $moveName has no samples for within-class variance")
//                        } else {
//                            val validSamples = samples.filter { it.features.size > featureIndex }
//                            if (validSamples.isNotEmpty()) {
//                                val variance = validSamples.map {
//                                    val value = it.features[featureIndex]
//                                    (value - moveMean) * (value - moveMean)
//                                }.average()
//
//                                Log.d(TAG, "  Within-class variance for move $moveName: $variance")
//                                withinClassVariance += variance
//                                validMoveCount++
//                            } else {
//                                Log.d(TAG, "  No valid samples for within-class variance calculation for $moveName")
//                            }
//                        }
//                    } catch (e: Exception) {
//                        Log.e(TAG, "  Error calculating within-class variance for move $moveName", e)
//                    }
//                }
//
//                // Calculate average within-class variance
//                withinClassVariance = if (validMoveCount > 0) {
//                    withinClassVariance / validMoveCount
//                } else {
//                    0.0001 // Small non-zero value to avoid division by zero
//                }
//
//                Log.d(TAG, "  Average within-class variance: $withinClassVariance")
//
//                // Calculate F-score (between-class variance / within-class variance)
//                val fScore = if (withinClassVariance > 0) {
//                    (betweenClassVariance / withinClassVariance).toFloat()
//                } else {
//                    Log.d(TAG, "  Within-class variance is zero, assigning high F-score")
//                    10f  // Avoid division by zero, assign high score
//                }
//
//                Log.d(TAG, "  F-score for feature $featureIndex: $fScore")
//
//                // Normalize to 0-1 weight
//                featureImportance[featureIndex] = minOf(1f, fScore / 10f)
//                Log.d(TAG, "  Normalized importance for feature $featureIndex: ${featureImportance[featureIndex]}")
//
//            } catch (e: Exception) {
//                Log.e(TAG, "Error processing feature $featureIndex", e)
//                featureImportance[featureIndex] = 0.1f // Default low importance for failed features
//            }
//        }
//
//        Log.d(TAG, "Calculated ${featureImportance.size} feature importance scores")
//    }
//
//    /**
//     * Calculate enhanced feature ranges from samples
//     */
//    private fun calculateEnhancedFeatureRanges(samples: List<NormalizedSample>): Map<Int, EnhancedFeatureRange> {
//        val featureRanges = mutableMapOf<Int, EnhancedFeatureRange>()
//
//        // Log basic info for debugging
//        Log.d(TAG, "Processing ${samples.size} samples for feature ranges")
//
//        // Count valid samples (with non-empty feature lists)
//        val validSamples = samples.filter { it.features.isNotEmpty() }
//        if (validSamples.isEmpty()) {
//            Log.d(TAG, "No valid samples found with non-empty feature lists")
//            return featureRanges
//        }
//
//        Log.d(TAG, "Found ${validSamples.size} valid samples out of ${samples.size} total")
//
//        // Get number of features from the first valid sample
//        val numFeatures = validSamples.first().features.size
//        Log.d(TAG, "Feature vector size: $numFeatures")
//
//        // For each feature index...
//        for (featureIndex in 0 until numFeatures) {
//            try {
//                // Get all values for this feature across valid samples
//                val featureValues = validSamples
//                    .filter { it.features.size > featureIndex } // Extra safety check
//                    .map { it.features[featureIndex] }
//
//                // Skip if no values
//                if (featureValues.isEmpty()) {
//                    Log.d(TAG, "No valid values for feature index $featureIndex, skipping")
//                    continue
//                }
//
//                // Get feature importance score
//                val importance = featureImportance[featureIndex] ?: 0.5f
//
//                // Only include features with meaningful variation
//                val variance = calculateVariance(featureValues)
//                if (variance < FEATURE_SIGNIFICANCE_THRESHOLD && importance < 0.3f) {
//                    Log.d(TAG, "Feature $featureIndex has low variance ($variance) and importance ($importance), skipping")
//                    continue
//                }
//
//                // Calculate distribution statistics
//                val min = featureValues.minOrNull() ?: 0f
//                val max = featureValues.maxOrNull() ?: 0f
//
//                // Sort for quartile calculations
//                val sortedValues = featureValues.sorted()
//
//                // Safe quartile calculations with bounds checking
//                val q1 = if (sortedValues.size >= 4) {
//                    sortedValues[sortedValues.size / 4]
//                } else {
//                    min // Use min as fallback for small sets
//                }
//
//                val median = if (sortedValues.isNotEmpty()) {
//                    sortedValues[sortedValues.size / 2]
//                } else {
//                    (min + max) / 2f // Use middle as fallback
//                }
//
//                val q3 = if (sortedValues.size >= 4) {
//                    sortedValues[sortedValues.size * 3 / 4]
//                } else {
//                    max // Use max as fallback for small sets
//                }
//
//                // Calculate standard deviation
//                val mean = featureValues.average().toFloat()
//                val stdDev = kotlin.math.sqrt(
//                    featureValues.map { (it - mean) * (it - mean) }.sum() / featureValues.size.coerceAtLeast(1)
//                ).toFloat()
//
//                // Calculate feature weight (combining importance and variance)
//                val weight = importance * (1 + variance).coerceAtMost(2f)
//
//                // Create enhanced feature range
//                featureRanges[featureIndex] = EnhancedFeatureRange(
//                    min = min,
//                    max = max,
//                    q1 = q1,
//                    median = median,
//                    q3 = q3,
//                    stdDev = stdDev,
//                    weight = weight
//                )
//
//                Log.d(TAG, "Added feature range for index $featureIndex with weight $weight")
//            } catch (e: Exception) {
//                Log.e(TAG, "Error processing feature index $featureIndex", e)
//            }
//        }
//
//        Log.d(TAG, "Calculated ${featureRanges.size} feature ranges")
//        return featureRanges
//    }
//
//    /**
//     * Calculate variance of a list of feature values
//     */
//    private fun calculateVariance(values: List<Float>): Float {
//        if (values.isEmpty()) return 0f
//
//        val mean = values.average().toFloat()
//        val sumSquaredDiff = values.sumOf { ((it - mean) * (it - mean)).toDouble() }
//        return (sumSquaredDiff / values.size).toFloat()
//    }
//
//    /**
//     * Predict action from normalized features
//     */
//    fun predict(features: List<Float>): Pair<String, Float> {
//        if (moveDefinitions.isEmpty()) {
//            return Pair(UNKNOWN_ACTION, 0f)
//        }
//
//        // Calculate match scores for each move
//        val moveScores = moveDefinitions.map { (name, definition) ->
//            // Get all relevant features with match scores and weights
//            val scoresAndWeights = definition.featureRanges.map { (featureIndex, range) ->
//                // Ensure feature index is valid
//                if (featureIndex < features.size) {
//                    // Get feature value and calculate match score
//                    val featureValue = features[featureIndex]
//                    Pair(range.getMatchScore(featureValue), range.weight)
//                } else {
//                    Pair(0f, 0f)
//                }
//            }
//
//            // Calculate weighted average score
//            val totalWeight = scoresAndWeights.sumOf { it.second.toDouble() }
//            val overallScore = if (totalWeight > 0) {
//                scoresAndWeights.sumOf { (score, weight) ->
//                    (score * weight).toDouble()
//                }.toFloat() / totalWeight.toFloat()
//            } else {
//                0f
//            }
//
//            Pair(name, overallScore)
//        }
//
//        // Find best match
//        val bestMatch = moveScores.maxByOrNull { it.second }
//            ?: return Pair(UNKNOWN_ACTION, 0f)
//
//        // Calculate gap between best and second-best match
//        val secondBest = moveScores
//            .filter { it.first != bestMatch.first }
//            .maxByOrNull { it.second }
//
//        // If there's a significant advantage, enhance confidence score
//        val enhancedConfidence = if (secondBest != null) {
//            val margin = bestMatch.second - secondBest.second
//            val bonus = minOf(0.15f, margin * 2) // Max 15% confidence boost
//            minOf(1.0f, bestMatch.second + bonus)
//        } else {
//            bestMatch.second
//        }
//
//        // Check if confidence exceeds threshold
//        return if (enhancedConfidence >= confidenceThreshold) {
//            Pair(bestMatch.first, enhancedConfidence)
//        } else {
//            Pair(UNKNOWN_ACTION, enhancedConfidence)
//        }
//    }
//
//    /**
//     * Predict with stability window
//     */
//    fun predictWithWindow(
//        features: List<Float>,
//        previousPredictions: List<Pair<String, Float>>,
//        requiredConsensus: Int = 2
//    ): Pair<String, Float> {
//        // Make current prediction
//        val currentPrediction = predict(features)
//
//        // Combine with previous predictions
//        val allPredictions = previousPredictions + currentPrediction
//
//        // Only consider non-unknown predictions with sufficient confidence
//        val validPredictions = allPredictions
//            .filter { it.first != UNKNOWN_ACTION && it.second >= confidenceThreshold * 1.0f }
//
//        // Count occurrences of each prediction
//        val predictionCounts = validPredictions
//            .groupBy { it.first }
//            .mapValues { it.value.size }
//
//        // Find most common prediction
//        val mostCommon = predictionCounts.maxByOrNull { it.value }
//
//        // Check if we have sufficient consensus
//        return if (mostCommon != null && mostCommon.value >= requiredConsensus) {
//            // Calculate average confidence for this prediction
//            val avgConfidence = validPredictions
//                .filter { it.first == mostCommon.key }
//                .map { it.second }
//                .average()
//                .toFloat()
//
//            // Add stability bonus
//            val stabilityBonus = minOf(0.1f, 0.02f * mostCommon.value)
//            val finalConfidence = minOf(1.0f, avgConfidence + stabilityBonus)
//
//            Pair(mostCommon.key, finalConfidence)
//        } else {
//            Pair(UNKNOWN_ACTION, 0f)
//        }
//    }
//
//    /**
//     * Get list of trained moves
//     */
//    fun getTrainedMoves(): List<String> {
//        return moveDefinitions.keys.toList()
//    }
//
//    /**
//     * Check if the classifier is trained
//     */
//    fun isTrained(): Boolean {
//        return moveDefinitions.isNotEmpty()
//    }
//
//    /**
//     * Get confidence threshold
//     */
//    fun getConfidenceThreshold(): Float {
//        return confidenceThreshold
//    }
//
//    /**
//     * Get feature importance list
//     */
//    fun getFeatureImportance(): Map<Int, Float> {
//        return featureImportance.toMap()
//    }
//
//    /**
//     * Get diagnostic information for move definitions
//     */
//    fun getDiagnosticInfo(): Map<String, Any> {
//        val info = mutableMapOf<String, Any>()
//
//        // Add basic information
//        info["numMoves"] = moveDefinitions.size
//        info["confidenceThreshold"] = confidenceThreshold
//        info["numImportantFeatures"] = featureImportance.count { it.value > 0.5f }
//
//        // Add feature counts for each move
//        val moveFeatureCounts = moveDefinitions.mapValues { it.value.featureRanges.size }
//        info["moveFeatureCounts"] = moveFeatureCounts
//
//        return info
//    }
//}

package com.example.humanreactor.AI_Model

import android.util.Log
import com.example.humanreactor.customizedMove.NormalizedSample

/**
 * Improved rule-based classifier that uses normalized samples
 */
class ImprovedRuleBasedClassifier(
    private var confidenceThreshold: Float = 0.8f,
    private val useDynamicThreshold: Boolean = false
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
            Log.d(TAG, "numFeatures:  ${numFeatures}")

            // Calculate feature importance (feature discriminative power)
            calculateFeatureImportance(samplesByMove, numFeatures)
            Log.d(TAG, "calculateFeatureImportance done")
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

            // If dynamic threshold is enabled, find optimal threshold
            if (useDynamicThreshold) {
                optimizeConfidenceThreshold(samples)
            }

            return moveDefinitions.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error training classifier", e)
            return false
        }
    }

    /**
     * Optimize confidence threshold within a reasonable range around the initial threshold
     */
    private fun optimizeConfidenceThreshold(samples: List<NormalizedSample>) {
        try {
            // Use initial confidenceThreshold as the center point
            val initialThreshold = confidenceThreshold
            Log.d(TAG, "Starting dynamic threshold optimization around initial threshold: $initialThreshold")

            // Define search range: initial threshold ± 0.05, but keep within [0.6, 0.95] bounds
            val minThreshold = maxOf(0.6f, initialThreshold - 0.05f)
            val maxThreshold = minOf(0.95f, initialThreshold + 0.05f)

            // Test with fine-grained threshold values within the defined range
            val thresholdStepSize = 0.01f  // 1% increments for precise optimization
            val thresholdRange = generateSequence(minThreshold) { it + thresholdStepSize }
                .takeWhile { it <= maxThreshold }
                .toList()

            Log.d(TAG, "Testing thresholds in safe range: $thresholdRange")

            // Track best threshold and its performance
            var bestThreshold = initialThreshold
            var bestScore = 0.0f

            // Test each threshold
            val results = thresholdRange.map { threshold ->
                // Store original threshold
                val originalThreshold = confidenceThreshold
                // Set temporary threshold for testing
                confidenceThreshold = threshold

                // Evaluate classifier on all samples
                val classResults = mutableMapOf<String, ClassificationMetrics>()
                var totalSamples = 0

                // Initialize metrics for all classes in the training set
                samples.map { it.label }.distinct().forEach { className ->
                    classResults[className] = ClassificationMetrics(className)
                }

                // Evaluate all samples
                samples.forEach { sample ->
                    totalSamples++
                    val predictedClass = predict(sample.features).first
                    val trueClass = sample.label

                    if (predictedClass != UNKNOWN_ACTION) {
                        if (predictedClass == trueClass) {
                            // True positive for this class
                            classResults[trueClass]!!.truePositives++

                            // True negative for all other classes
                            classResults.keys.filter { it != trueClass }.forEach { otherClass ->
                                classResults[otherClass]!!.trueNegatives++
                            }
                        } else {
                            // False positive for predicted class
                            classResults[predictedClass]!!.falsePositives++

                            // False negative for true class
                            classResults[trueClass]!!.falseNegatives++

                            // True negative for all other classes
                            classResults.keys.filter { it != trueClass && it != predictedClass }.forEach { otherClass ->
                                classResults[otherClass]!!.trueNegatives++
                            }
                        }
                    } else {
                        // Unknown prediction counts as false negative for true class
                        classResults[trueClass]!!.falseNegatives++

                        // And true negative for all other classes
                        classResults.keys.filter { it != trueClass }.forEach { otherClass ->
                            classResults[otherClass]!!.trueNegatives++
                        }
                    }
                }

                // Calculate overall metrics across all classes
                val classesWithSamples = classResults.filter { it.value.totalSamples > 0 }

                // Calculate average rates across classes
                val averageTpRate = if (classesWithSamples.isNotEmpty()) {
                    classesWithSamples.values.map { it.truePositiveRate }.average().toFloat()
                } else {
                    0.0f
                }

                val averageFpRate = if (classesWithSamples.isNotEmpty()) {
                    classesWithSamples.values.map { it.falsePositiveRate }.average().toFloat()
                } else {
                    1.0f
                }

                // Calculate F1 score (harmonic mean of precision and recall)
                val macroF1 = if (classesWithSamples.isNotEmpty()) {
                    classesWithSamples.values.map { it.f1Score }.average().toFloat()
                } else {
                    0.0f
                }

                // Custom balanced score with higher weight on minimizing false positives
                // This prioritizes precision over recall
                val customScore = (0.5f * averageTpRate) + (0.5f * (1.0f - averageFpRate))

                // Restore original threshold
                confidenceThreshold = originalThreshold

                Log.d(TAG, "Threshold: $threshold, Avg TP Rate: $averageTpRate, " +
                        "Avg FP Rate: $averageFpRate, F1: $macroF1, " +
                        "Balanced Score: $customScore")

                // Return result for this threshold
                ThresholdTestResult(
                    threshold = threshold,
                    averageTpRate = averageTpRate,
                    averageFpRate = averageFpRate,
                    customBalancedScore = customScore,
                    macroF1Score = macroF1,
                    classMetrics = classResults
                )
            }

            // Find threshold with best balanced score
            val bestResult = results.maxByOrNull { it.customBalancedScore }

            if (bestResult != null) {
                bestThreshold = bestResult.threshold
                bestScore = bestResult.customBalancedScore

                // Set the optimal threshold
                confidenceThreshold = bestThreshold

                Log.d(TAG, "Optimized threshold: $bestThreshold (changed from $initialThreshold)")
                Log.d(TAG, "Performance metrics - TP Rate: ${bestResult.averageTpRate}, " +
                        "FP Rate: ${bestResult.averageFpRate}, F1 Score: ${bestResult.macroF1Score}")

                // Log detailed metrics for best threshold
                bestResult.classMetrics.values.forEach { metrics ->
                    if (metrics.totalSamples > 0) {
                        Log.d(TAG, "${metrics.className}: TP=${metrics.truePositives}, " +
                                "FP=${metrics.falsePositives}, FN=${metrics.falseNegatives}, " +
                                "TPR=${metrics.truePositiveRate}, FPR=${metrics.falsePositiveRate}, " +
                                "Precision=${metrics.precision}, F1=${metrics.f1Score}")
                    }
                }

                // Additional stats about improvement
//                val improvement = (bestScore - results.find { it.threshold == initialThreshold }?.customBalancedScore!!
//                    ?: 0.0f) * 100
//                Log.d(TAG, "Score improvement: $improvement%")
            } else {
                Log.d(TAG, "No valid threshold found, keeping original: $initialThreshold")
                confidenceThreshold = initialThreshold
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error optimizing threshold", e)
            // Keep original threshold in case of error
        }
    }

    /**
     * Helper class to track classification metrics per class
     */
    private data class ClassificationMetrics(
        val className: String,
        var truePositives: Int = 0,
        var falsePositives: Int = 0,
        var falseNegatives: Int = 0,
        var trueNegatives: Int = 0
    ) {
        val totalSamples: Int
            get() = truePositives + falseNegatives

        val truePositiveRate: Float
            get() = if (totalSamples > 0) truePositives.toFloat() / totalSamples else 0.0f

        val falsePositiveRate: Float
            get() = if (trueNegatives + falsePositives > 0) {
                falsePositives.toFloat() / (trueNegatives + falsePositives)
            } else {
                0.0f
            }

        val precision: Float
            get() = if (truePositives + falsePositives > 0) {
                truePositives.toFloat() / (truePositives + falsePositives)
            } else {
                0.0f
            }

        val recall: Float
            get() = truePositiveRate

        val f1Score: Float
            get() {
                val p = precision
                val r = recall
                return if (p + r > 0) {
                    2 * p * r / (p + r)
                } else {
                    0.0f
                }
            }

        val specificity: Float
            get() = if (trueNegatives + falsePositives > 0) {
                trueNegatives.toFloat() / (trueNegatives + falsePositives)
            } else {
                1.0f
            }

        // Balanced accuracy (average of TPR and TNR)
        val balancedAccuracy: Float
            get() = (truePositiveRate + specificity) / 2
    }

    /**
     * Helper class to track threshold test results
     */
    private data class ThresholdTestResult(
        val threshold: Float,
        val averageTpRate: Float,
        val averageFpRate: Float,
        val customBalancedScore: Float,
        val macroF1Score: Float,
        val classMetrics: Map<String, ClassificationMetrics>
    )

    /**
     * Calculate feature importance (measures each feature's ability to discriminate between different moves)
     */
    private fun calculateFeatureImportance(samplesByMove: Map<String, List<NormalizedSample>>, numFeatures: Int) {
        // Log initial information
        Log.d(TAG, "Starting calculateFeatureImportance with $numFeatures features")
        Log.d(TAG, "Number of moves: ${samplesByMove.size}")

        // Log samples per move
        samplesByMove.forEach { (moveName, samples) ->
            Log.d(TAG, "Move: $moveName has ${samples.size} samples")

            // Log the first sample for each move if available
            if (samples.isNotEmpty()) {
                val firstSample = samples.first()
                Log.d(TAG, "  First sample label: ${firstSample.label}")
                Log.d(TAG, "  First sample features size: ${firstSample.features.size}")
                Log.d(TAG, "  First sample features: ${firstSample.features}")
            } else {
                Log.d(TAG, "  WARNING: No samples for move $moveName")
            }
        }

        // For each feature...
        for (featureIndex in 0 until numFeatures) {
            Log.d(TAG, "Processing feature index: $featureIndex")

            try {
                // Calculate feature means for each move
                val moveFeatureMeans = mutableMapOf<String, Float>()

                samplesByMove.forEach { (moveName, samples) ->
                    if (samples.isEmpty()) {
                        Log.d(TAG, "  Move $moveName has no samples, using default mean value")
                        moveFeatureMeans[moveName] = 0.0f
                    } else {
                        try {
                            // Check if all samples have enough features
                            val invalidSamples = samples.filter { it.features.size <= featureIndex }
                            if (invalidSamples.isNotEmpty()) {
                                Log.d(TAG, "  WARNING: ${invalidSamples.size} samples in move $moveName don't have feature at index $featureIndex")
                                Log.d(TAG, "  First invalid sample features size: ${invalidSamples.firstOrNull()?.features?.size ?: 0}")
                            }

                            // Only use valid samples
                            val validSamples = samples.filter { it.features.size > featureIndex }
                            if (validSamples.isEmpty()) {
                                Log.d(TAG, "  No valid samples for move $moveName at feature index $featureIndex, using default")
                                moveFeatureMeans[moveName] = 0.0f
                            } else {
                                val mean = validSamples.map { it.features[featureIndex] }.average().toFloat()
                                moveFeatureMeans[moveName] = mean
                                Log.d(TAG, "  Move $moveName mean for feature $featureIndex: $mean")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "  Error calculating mean for move $moveName at feature $featureIndex", e)
                            moveFeatureMeans[moveName] = 0.0f
                        }
                    }
                }

                // If only one move, set low importance
                if (moveFeatureMeans.size <= 1) {
                    Log.d(TAG, "  Only ${moveFeatureMeans.size} moves, setting low importance (0.5)")
                    featureImportance[featureIndex] = 0.5f
                    continue
                }

                // Calculate global mean
                val globalMean = moveFeatureMeans.values.average()
                Log.d(TAG, "  Global mean for feature $featureIndex: $globalMean")

                // Calculate between-class variance
                val betweenClassVariance = moveFeatureMeans.values
                    .map { (it - globalMean) * (it - globalMean) }
                    .average()
                Log.d(TAG, "  Between-class variance: $betweenClassVariance")

                // Calculate within-class variance with error handling
                var withinClassVariance = 0.0
                var validMoveCount = 0

                samplesByMove.forEach { (moveName, samples) ->
                    try {
                        val moveMean = moveFeatureMeans[moveName] ?: 0f

                        if (samples.isEmpty()) {
                            Log.d(TAG, "  Move $moveName has no samples for within-class variance")
                        } else {
                            val validSamples = samples.filter { it.features.size > featureIndex }
                            if (validSamples.isNotEmpty()) {
                                val variance = validSamples.map {
                                    val value = it.features[featureIndex]
                                    (value - moveMean) * (value - moveMean)
                                }.average()

                                Log.d(TAG, "  Within-class variance for move $moveName: $variance")
                                withinClassVariance += variance
                                validMoveCount++
                            } else {
                                Log.d(TAG, "  No valid samples for within-class variance calculation for $moveName")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "  Error calculating within-class variance for move $moveName", e)
                    }
                }

                // Calculate average within-class variance
                withinClassVariance = if (validMoveCount > 0) {
                    withinClassVariance / validMoveCount
                } else {
                    0.0001 // Small non-zero value to avoid division by zero
                }

                Log.d(TAG, "  Average within-class variance: $withinClassVariance")

                // Calculate F-score (between-class variance / within-class variance)
                val fScore = if (withinClassVariance > 0) {
                    (betweenClassVariance / withinClassVariance).toFloat()
                } else {
                    Log.d(TAG, "  Within-class variance is zero, assigning high F-score")
                    10f  // Avoid division by zero, assign high score
                }

                Log.d(TAG, "  F-score for feature $featureIndex: $fScore")

                // Normalize to 0-1 weight
                featureImportance[featureIndex] = minOf(1f, fScore / 10f)
                Log.d(TAG, "  Normalized importance for feature $featureIndex: ${featureImportance[featureIndex]}")

            } catch (e: Exception) {
                Log.e(TAG, "Error processing feature $featureIndex", e)
                featureImportance[featureIndex] = 0.1f // Default low importance for failed features
            }
        }

        Log.d(TAG, "Calculated ${featureImportance.size} feature importance scores")
    }

    /**
     * Calculate enhanced feature ranges from samples
     */
    private fun calculateEnhancedFeatureRanges(samples: List<NormalizedSample>): Map<Int, EnhancedFeatureRange> {
        val featureRanges = mutableMapOf<Int, EnhancedFeatureRange>()

        // Log basic info for debugging
        Log.d(TAG, "Processing ${samples.size} samples for feature ranges")

        // Count valid samples (with non-empty feature lists)
        val validSamples = samples.filter { it.features.isNotEmpty() }
        if (validSamples.isEmpty()) {
            Log.d(TAG, "No valid samples found with non-empty feature lists")
            return featureRanges
        }

        Log.d(TAG, "Found ${validSamples.size} valid samples out of ${samples.size} total")

        // Get number of features from the first valid sample
        val numFeatures = validSamples.first().features.size
        Log.d(TAG, "Feature vector size: $numFeatures")

        // For each feature index...
        for (featureIndex in 0 until numFeatures) {
            try {
                // Get all values for this feature across valid samples
                val featureValues = validSamples
                    .filter { it.features.size > featureIndex } // Extra safety check
                    .map { it.features[featureIndex] }

                // Skip if no values
                if (featureValues.isEmpty()) {
                    Log.d(TAG, "No valid values for feature index $featureIndex, skipping")
                    continue
                }

                // Get feature importance score
                val importance = featureImportance[featureIndex] ?: 0.5f

                // Only include features with meaningful variation
                val variance = calculateVariance(featureValues)
                if (variance < FEATURE_SIGNIFICANCE_THRESHOLD && importance < 0.3f) {
                    Log.d(TAG, "Feature $featureIndex has low variance ($variance) and importance ($importance), skipping")
                    continue
                }

                // Calculate distribution statistics
                val min = featureValues.minOrNull() ?: 0f
                val max = featureValues.maxOrNull() ?: 0f

                // Sort for quartile calculations
                val sortedValues = featureValues.sorted()

                // Safe quartile calculations with bounds checking
                val q1 = if (sortedValues.size >= 4) {
                    sortedValues[sortedValues.size / 4]
                } else {
                    min // Use min as fallback for small sets
                }

                val median = if (sortedValues.isNotEmpty()) {
                    sortedValues[sortedValues.size / 2]
                } else {
                    (min + max) / 2f // Use middle as fallback
                }

                val q3 = if (sortedValues.size >= 4) {
                    sortedValues[sortedValues.size * 3 / 4]
                } else {
                    max // Use max as fallback for small sets
                }

                // Calculate standard deviation
                val mean = featureValues.average().toFloat()
                val stdDev = kotlin.math.sqrt(
                    featureValues.map { (it - mean) * (it - mean) }.sum() / featureValues.size.coerceAtLeast(1)
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

                Log.d(TAG, "Added feature range for index $featureIndex with weight $weight")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing feature index $featureIndex", e)
            }
        }

        Log.d(TAG, "Calculated ${featureRanges.size} feature ranges")
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
        info["useDynamicThreshold"] = useDynamicThreshold

        // Add feature counts for each move
        val moveFeatureCounts = moveDefinitions.mapValues { it.value.featureRanges.size }
        info["moveFeatureCounts"] = moveFeatureCounts

        return info
    }
}