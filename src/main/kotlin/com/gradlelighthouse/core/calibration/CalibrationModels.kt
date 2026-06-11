package com.gradlelighthouse.core.calibration

import com.gradlelighthouse.core.AuditIssue
import java.io.Serializable

/**
 * Defines the "Ground Truth" for a benchmark repository.
 */
data class BenchmarkTarget(
    val name: String,
    val expectedMin: Double,
    val expectedMax: Double,
    val sampleIssues: List<AuditIssue>
) : Serializable

/**
 * The result of a single calibration run.
 */
data class CalibrationResult(
    val target: BenchmarkTarget,
    val actualScore: Double,
    val delta: Double,
    val isWithinRange: Boolean
) : Serializable {
    val status: String = if (isWithinRange) "PASS" else "FAIL"
}

/**
 * Summary of the entire calibration suite.
 */
data class CalibrationReport(
    val results: List<CalibrationResult>,
    val averageDelta: Double,
    val suggestedKAdjustment: Double
)
