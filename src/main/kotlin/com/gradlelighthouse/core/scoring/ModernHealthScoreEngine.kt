package com.gradlelighthouse.core.scoring

import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.LighthouseCategory
import com.gradlelighthouse.core.Severity
import kotlin.math.sqrt

/**
 * The next-generation scoring engine for Gradle Lighthouse.
 *
 * Implements the Square Root Deduction Model:
 * score = 100 - K * sqrt(rawImpact)
 *
 * Tuned for industry benchmarks:
 * - Now in Android: ~98
 * - Signal Android: ~82
 * - Firefox Android: ~45
 */
class ModernHealthScoreEngine(
    private val aggregator: ScoreAggregator = WeightedScoreAggregator()
) {
    private val pathEngine = PathTo90Engine(this, aggregator)

    // Architectural Friction Coefficient - Tuned for industry benchmarks
    val frictionK = 6.6

    /**
     * Severity weights for architectural impact.
     */
    val severityWeights = mapOf(
        Severity.FATAL to 32.0, // 1 Fatal = 4 Errors
        Severity.ERROR to 8.0,
        Severity.WARNING to 2.0,
        Severity.INFO to 0.2
    )

    fun calculatePointsLost(rawImpact: Double): Double {
        return frictionK * sqrt(rawImpact)
    }

    fun calculate(
        issues: List<AuditIssue>,
        moduleCount: Int = 1,
        pluginIds: Set<String> = emptySet(),
        rootDir: java.io.File? = null
    ): ScoringResult {
        val categoryScores = LighthouseCategory.entries.map { category ->
            calculateCategoryScore(category, issues.filter { it.category == category })
        }.sortedBy { it.score }

        val overallScore = aggregator.aggregate(categoryScores)
        val improvements = pathEngine.calculate(overallScore, issues)

        val baseResult = ScoringResult(
            overallScore = overallScore,
            categoryScores = categoryScores,
            improvements = improvements
        )

        // Industry Benchmarking (Sprint 2)
        val benchmarkDir = rootDir?.let { java.io.File(it, "benchmarks") }
        val benchmarkEngine = com.gradlelighthouse.core.benchmarking.BenchmarkEngine(benchmarkDir)
        val benchmarkResult = benchmarkEngine.benchmark(baseResult, moduleCount, pluginIds)

        return baseResult.copy(benchmarkResult = benchmarkResult)
    }

    private fun calculateCategoryScore(
        category: LighthouseCategory,
        issues: List<AuditIssue>
    ): CategoryScore {
        val rawImpact = issues.sumOf { severityWeights[it.severity] ?: 0.0 }

        // Square Root Deduction Model
        val pointsLost = frictionK * sqrt(rawImpact)

        // Hard-ceiling: 3+ Fatals floor the category to 0
        val fatalCount = issues.count { it.severity == Severity.FATAL }
        val errorCount = issues.count { it.severity == Severity.ERROR }
        val warningCount = issues.count { it.severity == Severity.WARNING }
        val infoCount = issues.count { it.severity == Severity.INFO }

        val score = if (fatalCount >= 3) 0.0 else (100.0 - pointsLost).coerceIn(0.0, 100.0)

        val deductions = issues.map { issue ->
            ScoreDeduction(
                findingId = issue.title,
                category = category,
                pointsLost = severityWeights[issue.severity] ?: 0.0,
                reason = issue.title
            )
        }.sortedByDescending { it.pointsLost }

        return CategoryScore(
            category = category,
            score = score,
            deductions = deductions,
            fatalCount = fatalCount,
            errorCount = errorCount,
            warningCount = warningCount,
            infoCount = infoCount
        )
    }
}
