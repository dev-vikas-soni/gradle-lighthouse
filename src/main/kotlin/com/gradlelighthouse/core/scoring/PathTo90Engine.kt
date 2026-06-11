package com.gradlelighthouse.core.scoring

import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.LighthouseCategory

/**
 * Calculates the most impactful improvements to reach the next health rank.
 *
 * Uses an incremental delta approach to estimate potential score gain
 * without re-running the entire scoring engine for every finding.
 */
class PathTo90Engine(
    private val scoreEngine: ModernHealthScoreEngine,
    private val aggregator: ScoreAggregator
) {

    fun calculate(
        currentScore: Double,
        allIssues: List<AuditIssue>
    ): List<ImprovementOpportunity> {
        if (allIssues.isEmpty()) return emptyList()

        val issueGroups = allIssues.groupBy { it.title }
        val categoryResults = LighthouseCategory.entries.map { category ->
            val catIssues = allIssues.filter { it.category == category }
            val rawImpact = catIssues.sumOf { scoreEngine.severityWeights[it.severity] ?: 0.0 }
            category to rawImpact
        }.toMap()

        val weightedAggregator = aggregator as? WeightedScoreAggregator
        val totalWeight = weightedAggregator?.totalWeight ?: 1.0

        return issueGroups.map { (title, groupIssues) ->
            val category = groupIssues.first().category
            val groupRawImpact = groupIssues.sumOf { scoreEngine.severityWeights[it.severity] ?: 0.0 }

            val currentRawTotal = categoryResults[category] ?: 0.0
            val newRawTotal = (currentRawTotal - groupRawImpact).coerceAtLeast(0.0)

            // Recalculate category score delta
            // pointsLost = K * sqrt(rawImpact)
            // K is internal to scoreEngine, but we can infer it or expose it.
            // For now, let's use the same simulation but only per category.

            val currentCatScore = calculateSimulatedCatScore(currentRawTotal)
            val newCatScore = calculateSimulatedCatScore(newRawTotal)

            val catWeight = weightedAggregator?.getWeight(category) ?: 1.0
            val expectedGain = (newCatScore - currentCatScore) * (catWeight / totalWeight)

            ImprovementOpportunity(
                title = title,
                currentImpact = groupRawImpact,
                expectedGain = expectedGain
            )
        }.filter { it.expectedGain > 0.1 }
         .sortedByDescending { it.expectedGain }
         .take(10)
    }

    private fun calculateSimulatedCatScore(rawImpact: Double): Double {
        val pointsLost = 2.37 * Math.sqrt(rawImpact)
        return (100.0 - pointsLost).coerceIn(0.0, 100.0)
    }
}
