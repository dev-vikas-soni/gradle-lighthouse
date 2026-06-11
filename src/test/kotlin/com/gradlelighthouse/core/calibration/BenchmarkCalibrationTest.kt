package com.gradlelighthouse.core.calibration

import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.LighthouseCategory
import com.gradlelighthouse.core.Severity
import com.gradlelighthouse.core.scoring.ModernHealthScoreEngine
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * CI Calibration Test: Ensures the scoring engine remains calibrated to industry benchmarks.
 */
class BenchmarkCalibrationTest {

    private val engine = ModernHealthScoreEngine()
    private val runner = CalibrationRunner(engine)

    @Test
    fun `validate architectural benchmarks`() {
        val targets = listOf(
            BenchmarkTarget(
                name = "Now in Android",
                expectedMin = 95.0,
                expectedMax = 100.0,
                sampleIssues = emptyList() // The gold standard
            ),
            BenchmarkTarget(
                name = "Signal Android",
                expectedMin = 80.0,
                expectedMax = 88.0,
                sampleIssues = listOf(
                    createIssue(LighthouseCategory.ARCHITECTURE, Severity.ERROR, "Feature Coupling"),
                    createIssue(LighthouseCategory.COMPLEXITY, Severity.WARNING, "Large Class"),
                    createIssue(LighthouseCategory.MODERNIZATION, Severity.WARNING, "Remaining XML")
                )
            ),
            BenchmarkTarget(
                name = "Firefox Android",
                expectedMin = 40.0,
                expectedMax = 55.0,
                sampleIssues = (1..60).map {
                    createIssue(LighthouseCategory.BUILD_PERFORMANCE, Severity.ERROR, "Build Bottleneck $it")
                }
            )
        )

        val report = runner.run(targets)
        runner.printReport(report)

        assertTrue(report.results.all { it.isWithinRange }, "Score calibration has drifted outside of expected industry ranges.")
    }

    private fun createIssue(category: LighthouseCategory, severity: Severity, title: String): AuditIssue {
        return AuditIssue(
            category = category,
            severity = severity,
            title = title,
            reasoning = "Test",
            impactAnalysis = "Test",
            resolution = "Test",
            roiAfterFix = "Test"
        )
    }
}
