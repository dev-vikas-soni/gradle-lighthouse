package com.gradlelighthouse.core.calibration

import com.gradlelighthouse.core.scoring.ModernHealthScoreEngine
import kotlin.math.abs

/**
 * CalibrationRunner: Empirically validates the scoring engine against benchmark targets.
 */
class CalibrationRunner(
    private val engine: ModernHealthScoreEngine
) {

    fun run(targets: List<BenchmarkTarget>): CalibrationReport {
        val results = targets.map { target ->
            val result = engine.calculate(target.sampleIssues)
            val actual = result.overallScore
            val isWithin = actual in target.expectedMin..target.expectedMax

            val delta = if (actual < target.expectedMin) {
                actual - target.expectedMin
            } else if (actual > target.expectedMax) {
                actual - target.expectedMax
            } else 0.0

            CalibrationResult(target, actual, delta, isWithin)
        }

        val totalDelta = results.sumOf { it.delta }
        val avgDelta = totalDelta / results.size

        // Tuning Strategy: Only adjust K.
        // If scores are too high (positive delta), K is too low (needs to increase).
        // If scores are too low (negative delta), K is too high (needs to decrease).
        val suggestedKAdjustment = avgDelta * 0.05 // Proportional control

        return CalibrationReport(results, avgDelta, suggestedKAdjustment)
    }

    fun printReport(report: CalibrationReport) {
        println("\n" + "═".repeat(80))
        println(" GRADLE LIGHTHOUSE CALIBRATION REPORT")
        println("═".repeat(80))
        println("%-20s | %-15s | %-10s | %-10s".format("Repository", "Expected", "Actual", "Status"))
        println("-".repeat(80))

        report.results.forEach { res ->
            val range = "[%.0f, %.0f]".format(res.target.expectedMin, res.target.expectedMax)
            val statusColor = if (res.isWithinRange) "\u001B[32m" else "\u001B[31m"
            println("%-20s | %-15s | %-10.2f | %s%s\u001B[0m".format(
                res.target.name, range, res.actualScore, statusColor, res.status
            ))
        }

        println("-".repeat(80))
        println("Average Delta: %.2f".format(report.averageDelta))

        if (abs(report.averageDelta) > 1.0) {
            println("\n\u001B[33mRECOMMENDATION:\u001B[0m")
            println("Scores are drifting. Suggested K adjustment: ${"%.3f".format(report.suggestedKAdjustment)}")
        } else {
            println("\n\u001B[32mCALIBRATION STABLE:\u001B[0m Current friction coefficient K is accurate.")
        }
        println("═".repeat(80) + "\n")
    }
}
