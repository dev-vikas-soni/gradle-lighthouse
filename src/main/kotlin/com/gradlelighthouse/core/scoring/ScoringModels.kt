package com.gradlelighthouse.core.scoring

import com.gradlelighthouse.core.LighthouseCategory
import java.io.Serializable

/**
 * Health grades based on score ranges.
 */
enum class HealthGrade {
    ELITE,
    STRONG,
    MAINTAINED,
    AT_RISK,
    LEGACY;

    companion object {
        fun fromScore(score: Double): HealthGrade = when {
            score >= 95.0 -> ELITE
            score >= 80.0 -> STRONG
            score >= 60.0 -> MAINTAINED
            score >= 40.0 -> AT_RISK
            else -> LEGACY
        }
    }
}

/**
 * A deduction record showing which finding is dragging the score down.
 */
data class ScoreDeduction(
    val findingId: String,
    val category: LighthouseCategory,
    val pointsLost: Double,
    val reason: String
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * Health score for a specific category.
 */
data class CategoryScore(
    val category: LighthouseCategory,
    val score: Double,
    val deductions: List<ScoreDeduction>,

    val fatalCount: Int,
    val errorCount: Int,
    val warningCount: Int,
    val infoCount: Int
) : Serializable {
    fun grade(): HealthGrade = HealthGrade.fromScore(score)

    companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * An actionable improvement that can increase the health score.
 */
data class ImprovementOpportunity(
    val title: String,
    val currentImpact: Double,
    val expectedGain: Double
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * The final result of the modern scoring engine.
 */
data class ScoringResult(
    val overallScore: Double,
    val categoryScores: List<CategoryScore>,
    val improvements: List<ImprovementOpportunity>,
    val benchmarkResult: com.gradlelighthouse.core.benchmarking.BenchmarkResult? = null
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
