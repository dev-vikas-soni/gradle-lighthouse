package com.gradlelighthouse.core.scoring

import com.gradlelighthouse.core.LighthouseCategory

/**
 * Strategy for aggregating multiple category scores into a single project score.
 */
interface ScoreAggregator {
    fun aggregate(categoryScores: List<CategoryScore>): Double
}

/**
 * A weighted aggregator that calculates the score based on category importance.
 *
 * It also applies a "Weakest Link" penalty: the overall score is capped
 * if any individual category is performing extremely poorly.
 */
class WeightedScoreAggregator(
    val weights: Map<LighthouseCategory, Double> = DEFAULT_WEIGHTS
) : ScoreAggregator {

    val totalWeight: Double by lazy { weights.values.sum() }

    fun getWeight(category: LighthouseCategory): Double = weights[category] ?: 1.0

    override fun aggregate(categoryScores: List<CategoryScore>): Double {
        if (categoryScores.isEmpty()) return 100.0

        var totalWeight = 0.0
        var weightedSum = 0.0

        categoryScores.forEach { catScore ->
            val weight = weights[catScore.category] ?: 1.0
            weightedSum += catScore.score * weight
            totalWeight += weight
        }

        val weightedAverage = if (totalWeight > 0) weightedSum / totalWeight else 0.0

        // Apply "Weakest Link" logic: The score is the average of the weighted mean and the poorest category.
        // This ensures that one failing area significantly drags down the overall health.
        val minCategoryScore = categoryScores.minOfOrNull { it.score } ?: 100.0
        val finalScore = (weightedAverage + minCategoryScore) / 2.0

        return finalScore.coerceIn(0.0, 100.0)
    }

    companion object {
        val DEFAULT_WEIGHTS = mapOf(
            LighthouseCategory.ARCHITECTURE to 20.0,
            LighthouseCategory.SECURITY to 20.0,
            LighthouseCategory.PERFORMANCE to 15.0,
            LighthouseCategory.BUILD_PERFORMANCE to 15.0,
            LighthouseCategory.COMPLEXITY to 10.0,
            LighthouseCategory.MODERNIZATION to 10.0,
            LighthouseCategory.QUALITY to 5.0,
            LighthouseCategory.DEPENDENCY_HYGIENE to 3.0,
            LighthouseCategory.APP_SIZE to 2.0
        )
    }
}
