package com.gradlelighthouse.core.scoring

import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.LighthouseCategory
import com.gradlelighthouse.core.Severity

/**
 * Calculates the most impactful improvements to reach the next health rank.
 *
 * Uses an incremental delta approach to estimate potential score gain
 * by simulating the removal of finding groups and re-running the aggregation logic.
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

        // 1. Calculate baseline raw impact per category
        val categoryRawImpacts = LighthouseCategory.entries.associateWith { category ->
            val catIssues = allIssues.filter { it.category == category }
            catIssues.sumOf { scoreEngine.severityWeights[it.severity] ?: 0.0 }
        }

        val issueGroups = allIssues.groupBy { it.title }

        return issueGroups.map { (title, groupIssues) ->
            // 2. Simulate removing this group
            val category = groupIssues.first().category
            val groupRawImpact = groupIssues.sumOf { scoreEngine.severityWeights[it.severity] ?: 0.0 }

            // 3. Recalculate only the affected category
            val simulatedCategoryScores = LighthouseCategory.entries.map { cat ->
                val currentRaw = categoryRawImpacts[cat] ?: 0.0
                val newRaw = if (cat == category) (currentRaw - groupRawImpact).coerceAtLeast(0.0) else currentRaw

                // Estimate cat score (ignores fatal hard-ceiling for simplicity in delta estimation)
                val pointsLost = scoreEngine.calculatePointsLost(newRaw)
                val score = (100.0 - pointsLost).coerceIn(0.0, 100.0)

                CategoryScore(
                    category = cat,
                    score = score,
                    deductions = emptyList(),
                    fatalCount = 0, errorCount = 0, warningCount = 0, infoCount = 0
                )
            }

            // 4. Run aggregator on simulated scores
            val simulatedOverallScore = aggregator.aggregate(simulatedCategoryScores)
            val gain = (simulatedOverallScore - currentScore).coerceAtLeast(0.0)

            ImprovementOpportunity(
                title = title,
                currentImpact = groupRawImpact,
                expectedGain = gain
            )
        }.filter { it.expectedGain > 0.01 }
         .sortedByDescending { it.expectedGain }
         .take(10)
    }
}
