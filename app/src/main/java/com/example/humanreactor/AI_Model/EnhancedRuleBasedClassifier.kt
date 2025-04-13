package com.example.humanreactor.AI_Model

import android.util.Log
import com.example.humanreactor.customizedMove.NormalizedSample

/**
 * Enhanced rule-based classifier that handles similar actions better
 */
class EnhancedRuleBasedClassifier(
    private val confidenceThreshold: Float = 0.80f, // Slightly lower default threshold
    private val useSimilarityAnalysis: Boolean = true
) {
    companion object {
        private const val TAG = "EnhancedRuleBasedClassifier"
        private const val UNKNOWN_ACTION = "unknown"

        // Increased threshold to be more selective about features
        private const val FEATURE_SIGNIFICANCE_THRESHOLD = 0.15f

        // Confusion threshold for identifying similar moves
        private const val CONFUSION_THRESHOLD = 0.75f
    }

    // Feature definitions for each move
    private val moveDefinitions = mutableMapOf<String, MoveFeatureDefinition>()

    // Feature importance scores
    private val featureImportance = mutableMapOf<Int, Float>()

    // Similarity mapping between moves (move pairs that are easily confused)
    private val similarMoves = mutableMapOf<String, List<String>>()

    // Discriminative features for similar move pairs
    private val discriminativeFeatures = mutableMapOf<Pair<String, String>, List<Int>>()

    // Structured representation of a move's feature definitions
    data class MoveFeatureDefinition(
        val name: String,
        val featureRanges: Map<Int, EnhancedFeatureRange>
    )

    // Enhanced feature range with distribution information
    data class EnhancedFeatureRange(
        val min: Float,          // Minimum value
        val max: Float,          // Maximum value
        val q1: Float,           // First quartile
        val median: Float,       // Median
        val q3: Float,           // Third quartile
        val stdDev: Float,       // Standard deviation
        val weight: Float,       // Feature weight
        val distribution: List<Float> = listOf() // Sample of distribution for more precise matching
    ) {
        // Check if value is within range
        fun contains(value: Float): Boolean = value in min..max

        // Calculate match score (0.0 to 1.0) with improved sensitivity
        fun getMatchScore(value: Float): Float {
            // Core region (IQR - interquartile range) - highest score
            if (value in q1..q3) {
                // Refined score based on closeness to median
                val distanceToMedian = kotlin.math.abs(value - median)
                val iqrRange = q3 - q1

                // Highest score near median, slightly lower at edges of IQR
                return 1.0f - (distanceToMedian / iqrRange) * 0.2f
            }

            // General range - graduated score
            if (value in min..max) {
                // Calculate relative position in the tail
                return if (value < q1) {
                    0.8f - 0.2f * ((q1 - value) / (q1 - min))
                } else {
                    0.8f - 0.2f * ((value - q3) / (max - q3))
                }
            }

            // Outside range - calculate decay score based on standard deviation
            val distance = when {
                value < min -> min - value
                else -> value - max
            }

            // Use standard deviation as decay factor with faster falloff
            return if (distance <= stdDev * 1.5f) {
                0.6f * (1.0f - (distance / (stdDev * 1.5f)))
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
            similarMoves.clear()
            discriminativeFeatures.clear()

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

            // If we're using similarity analysis, identify similar moves and their discriminative features
            if (useSimilarityAnalysis && moveDefinitions.size > 1) {
                analyzeSimilarMoves(samplesByMove)
            }

            return moveDefinitions.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error training classifier", e)
            return false
        }
    }

    /**
     * Analyze similarities between moves to find potentially confusing move pairs
     */
    private fun analyzeSimilarMoves(samplesByMove: Map<String, List<NormalizedSample>>) {
        // Create list of move pairs to analyze
        val moveNames = moveDefinitions.keys.toList()
        val movePairs = moveNames.flatMap { move1 ->
            moveNames.filter { move2 -> move1 != move2 }.map { move2 ->
                Pair(move1, move2)
            }
        }

        // For each move pair, calculate similarity score
        for (movePair in movePairs) {
            val (move1, move2) = movePair
            val def1 = moveDefinitions[move1] ?: continue
            val def2 = moveDefinitions[move2] ?: continue

            // Find common features
            val commonFeatures = def1.featureRanges.keys.intersect(def2.featureRanges.keys)

            // Calculate overlap across features
            var overlapCount = 0
            var totalFeatures = 0

            for (featureIdx in commonFeatures) {
                val range1 = def1.featureRanges[featureIdx] ?: continue
                val range2 = def2.featureRanges[featureIdx] ?: continue

                // Check for overlap
                if (range1.max >= range2.min && range1.min <= range2.max) {
                    overlapCount++
                }
                totalFeatures++
            }

            // Calculate similarity as percentage of overlapping features
            val similarityScore = if (totalFeatures > 0) {
                overlapCount.toFloat() / totalFeatures.toFloat()
            } else {
                0f
            }

            // If similarity is high, store as similar moves
            if (similarityScore >= CONFUSION_THRESHOLD) {
                // Add to similar moves list
                similarMoves[move1] = similarMoves.getOrDefault(move1, listOf()) + move2

                // Find discriminative features for this pair
                findDiscriminativeFeatures(movePair, samplesByMove)

                Log.d(TAG, "Similar moves identified: $move1 and $move2 (similarity: $similarityScore)")
            }
        }
    }

    /**
     * Find the most discriminative features for distinguishing between similar moves
     */
    private fun findDiscriminativeFeatures(
        movePair: Pair<String, String>,
        samplesByMove: Map<String, List<NormalizedSample>>
    ) {
        val (move1, move2) = movePair
        val samples1 = samplesByMove[move1] ?: return
        val samples2 = samplesByMove[move2] ?: return

        if (samples1.isEmpty() || samples2.isEmpty()) return

        // Get number of features
        val numFeatures = samples1.first().features.size

        // Calculate discrimination power for each feature
        val featureDiscrimPower = mutableMapOf<Int, Float>()

        for (featureIdx in 0 until numFeatures) {
            // Get feature values for both moves
            val values1 = samples1.map { it.features[featureIdx] }
            val values2 = samples2.map { it.features[featureIdx] }

            // Calculate means
            val mean1 = values1.average().toFloat()
            val mean2 = values2.average().toFloat()

            // Calculate standard deviations
            val stdDev1 = calculateStdDev(values1, mean1)
            val stdDev2 = calculateStdDev(values2, mean2)

            // Calculate discrimination power using Fisher's criterion
            val meanDiff = kotlin.math.abs(mean1 - mean2)
            val pooledStdDev = (stdDev1 + stdDev2) / 2f

            val discriminationPower = if (pooledStdDev > 0) {
                meanDiff / pooledStdDev
            } else if (meanDiff > 0) {
                10f  // High value if means differ but no variance
            } else {
                0f   // No discrimination power
            }

            featureDiscrimPower[featureIdx] = discriminationPower
        }

        // Sort features by discrimination power and take top ones
        val topFeatures = featureDiscrimPower.entries
            .sortedByDescending { it.value }
            .filter { it.value > 0.5f }  // Only features with good discrimination
            .map { it.key }
            .take(5)  // Take top 5 most discriminative features
            .toList()

        // Store discriminative features for this pair
        if (topFeatures.isNotEmpty()) {
            discriminativeFeatures[movePair] = topFeatures
            Log.d(TAG, "Discriminative features for $move1 vs $move2: $topFeatures")
        }
    }

    /**
     * Calculate standard deviation
     */
    private fun calculateStdDev(values: List<Float>, mean: Float): Float {
        if (values.isEmpty()) return 0f

        val sumSquaredDiff = values.sumOf { ((it - mean) * (it - mean)).toDouble() }
        return kotlin.math.sqrt(sumSquaredDiff / values.size).toFloat()
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

            // Normalize to 0-1 weight with higher emphasis on discriminative features
            featureImportance[featureIndex] = minOf(1f, (fScore / 8f) * 1.25f)
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
            if (variance < FEATURE_SIGNIFICANCE_THRESHOLD && importance < 0.4f) {
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
            // Amplify weight for highly discriminative features
            val weight = if (importance > 0.7f) {
                importance * 1.5f * (1 + variance).coerceAtMost(2.5f)
            } else {
                importance * (1 + variance).coerceAtMost(2f)
            }

            // Store a sample of the distribution for more precise matching
            val distributionSample = if (featureValues.size <= 10) {
                featureValues
            } else {
                // Take evenly spaced samples
                val step = featureValues.size / 10
                (0 until 10).map { i -> featureValues[i * step] }
            }

            // Create enhanced feature range
            featureRanges[featureIndex] = EnhancedFeatureRange(
                min = min,
                max = max,
                q1 = q1,
                median = median,
                q3 = q3,
                stdDev = stdDev,
                weight = weight,
                distribution = distributionSample
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
     * Cached result for fast prediction in similar frames
     */
    private var cachedPrediction: Triple<List<Float>, String, Float>? = null

    /**
     * Predict action from normalized features
     * @param features The feature vector to predict from
     * @param expectedMove Optional hint about which move to expect (for optimization)
     * @param fastCacheCheck If true, will use cache when possible
     */
    fun predict(
        features: List<Float>,
        expectedMove: String? = null,
        fastCacheCheck: Boolean = false
    ): Pair<String, Float> {
        if (moveDefinitions.isEmpty()) {
            return Pair(UNKNOWN_ACTION, 0f)
        }

        // Cache check for very similar feature vectors (great for static poses)
        if (fastCacheCheck && cachedPrediction != null) {
            val (cachedFeatures, cachedMove, cachedConfidence) = cachedPrediction!!

            // Only use cache if feature vectors are very similar and cache is recent
            if (cachedFeatures.size == features.size &&
                isFeatureVectorVerySimilar(cachedFeatures, features)) {
                return Pair(cachedMove, cachedConfidence)
            }
        }

        // Fast path when we have an expected move (can greatly improve performance)
        if (expectedMove != null && expectedMove != UNKNOWN_ACTION &&
            moveDefinitions.containsKey(expectedMove)) {

            // Calculate score only for the expected move
            val expectedScore = calculateScoreForMove(features, expectedMove)

            // If score is very high, we can skip full prediction
            if (expectedScore >= confidenceThreshold * 1.1f) {
                // Store in cache
                cachedPrediction = Triple(features.toList(), expectedMove, expectedScore)
                return Pair(expectedMove, expectedScore)
            }
        }

        // Standard full prediction path
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

        // Find best match and second best match
        val bestMatch = moveScores.maxByOrNull { it.second }
            ?: return Pair(UNKNOWN_ACTION, 0f)

        val secondBest = moveScores
            .filter { it.first != bestMatch.first }
            .maxByOrNull { it.second }

        // Check if top two matches are similar moves
        if (secondBest != null &&
            useSimilarityAnalysis &&
            similarMoves[bestMatch.first]?.contains(secondBest.first) == true) {

            // Get discriminative features for this pair
            val discFeatures = discriminativeFeatures[Pair(bestMatch.first, secondBest.first)]
                ?: discriminativeFeatures[Pair(secondBest.first, bestMatch.first)]

            if (discFeatures != null && discFeatures.isNotEmpty()) {
                // Re-evaluate using only discriminative features
                val refinedScores = refinePredictionForSimilarMoves(
                    features,
                    listOf(bestMatch.first, secondBest.first),
                    discFeatures
                )

                // Update best match if refined prediction is available
                if (refinedScores.isNotEmpty()) {
                    val refinedBest = refinedScores.maxByOrNull { it.second }!!

                    // Use refined best match with original confidence adjusted by refinement
                    val refinementBoost = 0.1f  // Confidence boost for refinement
                    val enhancedConfidence = minOf(1.0f, bestMatch.second + refinementBoost)

                    // Store in cache
                    cachedPrediction = Triple(features.toList(), refinedBest.first, enhancedConfidence)
                    return Pair(refinedBest.first, enhancedConfidence)
                }
            }
        }

        // Standard confidence calculation
        val enhancedConfidence = if (secondBest != null) {
            val margin = bestMatch.second - secondBest.second
            val bonus = minOf(0.15f, margin * 2.5f) // Max 15% confidence boost, more aggressive
            minOf(1.0f, bestMatch.second + bonus)
        } else {
            bestMatch.second
        }

        // Store result in cache
        val result = if (enhancedConfidence >= confidenceThreshold) {
            Pair(bestMatch.first, enhancedConfidence)
        } else {
            Pair(UNKNOWN_ACTION, enhancedConfidence)
        }

        cachedPrediction = Triple(features.toList(), result.first, result.second)
        return result
    }

    /**
     * Calculate score for a specific move (fast calculation)
     */
    private fun calculateScoreForMove(features: List<Float>, moveName: String): Float {
        val definition = moveDefinitions[moveName] ?: return 0f

        // Calculate scores for this move's features
        val scoresAndWeights = definition.featureRanges.map { (featureIndex, range) ->
            if (featureIndex < features.size) {
                val featureValue = features[featureIndex]
                Pair(range.getMatchScore(featureValue), range.weight)
            } else {
                Pair(0f, 0f)
            }
        }

        // Calculate weighted average
        val totalWeight = scoresAndWeights.sumOf { it.second.toDouble() }
        return if (totalWeight > 0) {
            (scoresAndWeights.sumOf { (score, weight) ->
                (score * weight).toDouble()
            } / totalWeight).toFloat()
        } else {
            0f
        }
    }

    /**
     * Check if two feature vectors are very similar (for caching purposes)
     */
    private fun isFeatureVectorVerySimilar(v1: List<Float>, v2: List<Float>): Boolean {
        if (v1.size != v2.size) return false

        // Calculate normalized Euclidean distance for select important features
        var sumSquaredDiff = 0f
        var count = 0

        // Only check a subset of important features for speed
        val featuresToCheck = featureImportance.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key }

        for (idx in featuresToCheck) {
            if (idx < v1.size) {
                val diff = v1[idx] - v2[idx]
                sumSquaredDiff += diff * diff
                count++
            }
        }

        if (count == 0) return false

        val distance = kotlin.math.sqrt(sumSquaredDiff / count)
        return distance < 0.03f  // Threshold for "very similar"
    }

    /**
     * Refine prediction for similar moves using discriminative features
     */
    private fun refinePredictionForSimilarMoves(
        features: List<Float>,
        moveNames: List<String>,
        discriminativeFeatures: List<Int>
    ): List<Pair<String, Float>> {
        val refinedScores = mutableListOf<Pair<String, Float>>()

        for (moveName in moveNames) {
            val definition = moveDefinitions[moveName] ?: continue

            // Calculate scores using only discriminative features
            var totalScore = 0f
            var totalWeight = 0f

            for (featureIdx in discriminativeFeatures) {
                val range = definition.featureRanges[featureIdx] ?: continue

                if (featureIdx < features.size) {
                    val featureValue = features[featureIdx]
                    val score = range.getMatchScore(featureValue)

                    // Use higher weight for discriminative analysis
                    val enhancedWeight = range.weight * 1.5f

                    totalScore += score * enhancedWeight
                    totalWeight += enhancedWeight
                }
            }

            val refinedScore = if (totalWeight > 0) {
                totalScore / totalWeight
            } else {
                0f
            }

            refinedScores.add(Pair(moveName, refinedScore))
        }

        return refinedScores
    }

    /**
     * Predict with stability window and confusion resolution
     * Optimized for performance with fast path option
     */
    fun predictWithWindow(
        features: List<Float>,
        previousPredictions: List<Pair<String, Float>>,
        requiredConsensus: Int = 2,
        fastPath: Boolean = true
    ): Pair<String, Float> {
        // First check if we already have strong consensus in previous predictions
        // This is the "fast path" that can avoid doing a full prediction
        if (fastPath && previousPredictions.size >= requiredConsensus) {
            val recentPredictions = previousPredictions.takeLast(4)
                .filter { it.first != UNKNOWN_ACTION && it.second >= confidenceThreshold }

            // Check if we have enough matching predictions already
            val predictionCounts = recentPredictions
                .groupBy { it.first }
                .mapValues { it.value.size }

            val consistentPrediction = predictionCounts
                .filter { it.value >= requiredConsensus }
                .maxByOrNull { it.value }

            if (consistentPrediction != null) {
                // We have strong consensus already, do a lightweight score calculation
                // instead of full prediction
                val scoreOnly = calculateQuickScore(features, consistentPrediction.key)

                // If quick score is reasonable, use fast path
                if (scoreOnly >= confidenceThreshold * 0.85f) {
                    // Get average confidence from previous predictions
                    val avgConfidence = recentPredictions
                        .filter { it.first == consistentPrediction.key }
                        .map { it.second }
                        .average()
                        .toFloat()

                    // Calculate final confidence with stability bonus
                    val stabilityBonus = minOf(0.15f, 0.04f * consistentPrediction.value)
                    val finalConfidence = minOf(1.0f, (avgConfidence + scoreOnly) / 2 + stabilityBonus)

                    return Pair(consistentPrediction.key, finalConfidence)
                }
            }
        }

        // Fall back to standard prediction if fast path didn't work
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

        // Check if we have sufficient consensus
        return if (mostCommon != null && mostCommon.value >= requiredConsensus) {
            // Calculate average confidence for this prediction
            val avgConfidence = validPredictions
                .filter { it.first == mostCommon.key }
                .map { it.second }
                .average()
                .toFloat()

            // Add stability bonus
            val stabilityBonus = minOf(0.12f, 0.03f * mostCommon.value)
            val finalConfidence = minOf(1.0f, avgConfidence + stabilityBonus)

            Pair(mostCommon.key, finalConfidence)
        } else {
            // Check for trend in recent predictions (last 3)
            val recentPredictions = allPredictions.takeLast(3)
                .filter { it.first != UNKNOWN_ACTION }

            // If we have 3 recent predictions all with different labels but good confidence,
            // it might indicate a transition between poses - use most recent with reduced confidence
            if (recentPredictions.size == 3 &&
                recentPredictions.map { it.first }.distinct().size == 3 &&
                recentPredictions.all { it.second >= confidenceThreshold * 0.9f }) {

                val mostRecent = recentPredictions.last()
                return Pair(mostRecent.first, mostRecent.second * 0.9f)
            }

            Pair(UNKNOWN_ACTION, 0f)
        }
    }

    /**
     * Calculate a quick score for a specific move without going through full prediction
     * This is used for the fast path in predictWithWindow
     */
    private fun calculateQuickScore(features: List<Float>, moveName: String): Float {
        val definition = moveDefinitions[moveName] ?: return 0f

        // Get top 5 most important features for this move
        val topFeatures = definition.featureRanges
            .entries
            .sortedByDescending { it.value.weight }
            .take(5)

        // Calculate score using only these important features
        var totalScore = 0f
        var totalWeight = 0f

        for ((featureIdx, range) in topFeatures) {
            if (featureIdx < features.size) {
                val featureValue = features[featureIdx]
                val score = range.getMatchScore(featureValue)

                totalScore += score * range.weight
                totalWeight += range.weight
            }
        }

        return if (totalWeight > 0) {
            totalScore / totalWeight
        } else {
            0f
        }
    }

    /**
     * Get list of trained moves
     */
    fun getTrainedMoves(): List<String> {
        return moveDefinitions.keys.toList()
    }

    /**
     * Get list of similar move pairs
     */
    fun getSimilarMoves(): Map<String, List<String>> {
        return similarMoves.toMap()
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
     * Get discriminative features for move pairs
     */
    fun getDiscriminativeFeatures(): Map<Pair<String, String>, List<Int>> {
        return discriminativeFeatures.toMap()
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
        info["numSimilarMovePairs"] = similarMoves.values.flatten().size / 2
        info["usingSimilarityAnalysis"] = useSimilarityAnalysis

        // Add feature counts for each move
        val moveFeatureCounts = moveDefinitions.mapValues { it.value.featureRanges.size }
        info["moveFeatureCounts"] = moveFeatureCounts

        return info
    }
}