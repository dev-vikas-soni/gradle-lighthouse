package com.gradlelighthouse.core

import java.io.Serializable

/**
 * Unified Health Score Engine for Gradle Lighthouse.
 *
 * Centralizes all scoring logic, rank computation, and deduction tracking
 * to eliminate the inconsistency between module-level and aggregate reports.
 *
 * Scoring uses an exponential decay model:
 *   score = 100 × 0.98^(totalWeightedImpact)
 *
 * This ensures diminishing returns — the first few issues hit hard,
 * but the score floors at [MIN_SCORE] to avoid demoralizing teams.
 */
object HealthScoreEngine : Serializable {

    private const val serialVersionUID = 1L
    private const val DECAY_BASE = 0.98
    private const val MIN_SCORE = 5
    private const val MAX_SCORE = 100

    /**
     * Severity weights used for score deduction.
     * Tuned for Android projects where FATAL = production crash.
     */
    private val SEVERITY_WEIGHTS = mapOf(
        Severity.FATAL to 35.0,
        Severity.ERROR to 15.0,
        Severity.WARNING to 5.0,
        Severity.INFO to 1.0
    )

    /**
     * Architectural Maturity Ranks — unified across all reports.
     */
    enum class ArchitectRank(val displayName: String, val emoji: String, val minScore: Int) {
        GRANDMASTER("Grandmaster Architect", "🏆", 95),
        EXPERT("Expert Architect", "⭐", 85),
        STANDARD("Standard Architect", "🔧", 70),
        AT_RISK("At Risk", "⚠️", 50),
        LEGACY("Legacy", "🔴", 0);

        companion object {
            fun fromScore(score: Int): ArchitectRank = values().first { score >= it.minScore }
        }
    }

    /**
     * A deduction record showing which category is dragging the score down.
     */
    data class Deduction(
        val category: String,
        val points: Int,
        val primaryBottleneck: String
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    /**
     * Complete health report for a module — everything needed for rendering.
     */
    data class HealthReport(
        val score: Int,
        val rank: ArchitectRank,
        val deductions: List<Deduction>,
        val fatalCount: Int,
        val errorCount: Int,
        val warningCount: Int,
        val infoCount: Int,
        val totalIssueCount: Int
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    /**
     * Calculates the health score from a list of audit issues.
     */
    fun calculateScore(issues: List<AuditIssue>): Int {
        if (issues.isEmpty()) return MAX_SCORE

        val totalImpact = issues.sumOf { SEVERITY_WEIGHTS[it.severity] ?: 0.0 }
        val rawScore = (MAX_SCORE * Math.pow(DECAY_BASE, totalImpact)).toInt()
        return rawScore.coerceIn(MIN_SCORE, MAX_SCORE)
    }

    /**
     * Generates category-level deductions sorted by impact (highest first).
     */
    fun calculateDeductions(issues: List<AuditIssue>): List<Deduction> {
        return issues.groupBy { it.category }.map { (category, categoryIssues) ->
            val points = categoryIssues.sumOf { (SEVERITY_WEIGHTS[it.severity] ?: 0.0).toInt() }
            val bottleneck = categoryIssues
                .sortedByDescending { SEVERITY_WEIGHTS[it.severity] ?: 0.0 }
                .first()
                .title
            Deduction(category, points, bottleneck)
        }.sortedByDescending { it.points }
    }

    /**
     * Generates a complete health report from audit issues.
     */
    fun generateReport(issues: List<AuditIssue>): HealthReport {
        val score = calculateScore(issues)
        return HealthReport(
            score = score,
            rank = ArchitectRank.fromScore(score),
            deductions = calculateDeductions(issues),
            fatalCount = issues.count { it.severity == Severity.FATAL },
            errorCount = issues.count { it.severity == Severity.ERROR },
            warningCount = issues.count { it.severity == Severity.WARNING },
            infoCount = issues.count { it.severity == Severity.INFO },
            totalIssueCount = issues.size
        )
    }

    /**
     * Returns the CSS color for a given score.
     */
    fun scoreColor(score: Int): String = when {
        score >= 90 -> "#10b981"
        score >= 75 -> "#3b82f6"
        score >= 60 -> "#f59e0b"
        else -> "#ef4444"
    }

    /**
     * Estimates developer hours saved per release cycle for a given severity.
     */
    fun estimatedHoursSaved(severity: Severity): String = when (severity) {
        Severity.FATAL -> "4.5h"
        Severity.ERROR -> "2.0h"
        Severity.WARNING -> "0.5h"
        Severity.INFO -> "0.1h"
    }
}
