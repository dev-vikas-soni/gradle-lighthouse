package com.gradlelighthouse.core.scoring

import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.LighthouseCategory
import com.gradlelighthouse.core.Severity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ModernHealthScoreEngineTest {

    private val engine = ModernHealthScoreEngine()

    @Test
    fun `score should be 100 when there are no issues`() {
        val result = engine.calculate(emptyList())
        assertEquals(100.0, result.overallScore)
    }

    @Test
    fun `fatal issue should have significant impact`() {
        val issues = listOf(
            createIssue(LighthouseCategory.ARCHITECTURE, Severity.FATAL, "Circular Dependency")
        )
        val result = engine.calculate(issues)

        // Fatal weight is 32. Architecture weight is 20%.
        // totalRaw = 32. pointsLost = 6.6 * sqrt(32) = 37.34.
        // Cat score = 62.66.
        // Overall: (Avg: 92.53 + Min: 62.66) / 2 = 77.60.

        assertEquals(77.60, result.overallScore, 0.01)
        assertEquals(62.66, result.categoryScores.find { it.category == LighthouseCategory.ARCHITECTURE }?.score ?: 0.0, 0.01)
    }

    @Test
    fun `weakest link logic should cap score for critical failures`() {
        val issues = (1..4).map {
            createIssue(LighthouseCategory.SECURITY, Severity.FATAL, "Vulnerability $it")
        }
        val result = engine.calculate(issues)

        // Security deduction: 4 Fatals trigger the Hard-Ceiling (score = 0).
        // Weighted average: (0.0 * 0.2 + 100 * 0.8) = 80.0.
        // Overall: (Avg: 80.0 + Min: 0.0) / 2 = 40.0.

        assertEquals(40.0, result.overallScore, 0.01)
    }

    private fun createIssue(category: LighthouseCategory, severity: Severity, title: String): AuditIssue {
        return AuditIssue(
            category = category,
            severity = severity,
            title = title,
            reasoning = "Test reasoning",
            impactAnalysis = "Test impact",
            resolution = "Test resolution",
            roiAfterFix = "Test ROI",
            remediationUrl = null,
            sourceFile = null,
            sourceLine = null
        )
    }
}
