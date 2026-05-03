package com.gradlelighthouse.auditors

import com.gradlelighthouse.core.AuditContext
import com.gradlelighthouse.core.Auditor
import com.gradlelighthouse.core.AuditIssue
import com.gradlelighthouse.core.ConsoleLogger
import com.gradlelighthouse.core.HealthScoreEngine
import com.gradlelighthouse.core.Severity
import java.io.File

/**
 * TrendTrackingAuditor: Stores and compares health scores across runs.
 *
 * Features:
 * - Persists score JSON to a known location
 * - Compares current score against previous run
 * - Reports delta/trend (improved/degraded)
 */
class TrendTrackingAuditor : Auditor {
    override val name: String = "TrendTracking"

    override fun audit(context: AuditContext): List<AuditIssue> {
        val issues = mutableListOf<AuditIssue>()
        ConsoleLogger.auditorStart(name, "📈", "[TREND]", "Comparing against previous audit...")

        val historyDir = File(context.rootDir, ".lighthouse")
        val historyFile = File(historyDir, "${context.projectName}-history.json")

        if (!historyFile.exists()) {
            issues.add(AuditIssue(
                category = "Trends",
                severity = Severity.INFO,
                title = "First Audit Run — Baseline Established",
                reasoning = "No previous audit history found for '${context.projectName}'. This run will be saved as the baseline for future comparisons.",
                impactAnalysis = "Without history, trend analysis isn't possible yet. Future runs will show score deltas.",
                resolution = "No action needed. Ensure .lighthouse/ directory is committed to VCS (or CI artifacts) for persistent tracking.",
                roiAfterFix = "Historical trend visibility after 2+ runs."
            ))
            return issues
        }

        // Read previous scores
        try {
            val historyContent = historyFile.readText()
            val previousScores = parseHistory(historyContent)
            if (previousScores.isNotEmpty()) {
                val lastScore = previousScores.last()
                val currentScore = context.currentScore ?: return issues

                val delta = currentScore - lastScore.score
                when {
                    delta < -5 -> issues.add(AuditIssue(
                        category = "Trends",
                        severity = Severity.ERROR,
                        title = "Health Score Degraded: $currentScore (${delta} points)",
                        reasoning = "Module '${context.projectName}' health score dropped from ${lastScore.score} to $currentScore (${delta} points) since ${lastScore.timestamp}.",
                        impactAnalysis = "Score degradation indicates new architectural debt was introduced. If this trend continues, the module will move to a lower maturity rank.",
                        resolution = "Review recent changes that introduced new issues. Check the detailed audit report for new FATAL/ERROR findings.",
                        roiAfterFix = "Prevent architectural drift and maintain code quality standards."
                    ))
                    delta > 5 -> issues.add(AuditIssue(
                        category = "Trends",
                        severity = Severity.INFO,
                        title = "Health Score Improved: $currentScore (+${delta} points) 🎉",
                        reasoning = "Module '${context.projectName}' health score improved from ${lastScore.score} to $currentScore (+${delta} points) since ${lastScore.timestamp}.",
                        impactAnalysis = "Great progress! The team is actively reducing architectural debt.",
                        resolution = "Keep up the momentum. Consider setting a higher failOnSeverity threshold to lock in gains.",
                        roiAfterFix = "Sustained architectural excellence."
                    ))
                }
            }
        } catch (_: Exception) { }

        return issues
    }

    data class ScoreEntry(val score: Int, val timestamp: String)

    private fun parseHistory(content: String): List<ScoreEntry> {
        val entries = mutableListOf<ScoreEntry>()
        // Simple JSON array parsing: [{"score":75,"timestamp":"2024-01-01"},...]
        val entryPattern = Regex("""\{"score"\s*:\s*(\d+)\s*,\s*"timestamp"\s*:\s*"([^"]+)"\s*\}""")
        entryPattern.findAll(content).forEach { match ->
            entries.add(ScoreEntry(
                score = match.groupValues[1].toInt(),
                timestamp = match.groupValues[2]
            ))
        }
        return entries
    }

    /**
     * Saves the current score to the history file. Called externally after scoring.
     */
    fun saveScore(rootDir: File, moduleName: String, score: Int) {
        val historyDir = File(rootDir, ".lighthouse")
        if (!historyDir.exists()) historyDir.mkdirs()

        val historyFile = File(historyDir, "$moduleName-history.json")
        val timestamp = java.time.LocalDateTime.now().toString()
        val newEntry = """{"score":$score,"timestamp":"$timestamp"}"""

        val existing = if (historyFile.exists()) {
            val content = historyFile.readText().trim()
            if (content.startsWith("[") && content.endsWith("]")) {
                content.removeSurrounding("[", "]").trim()
            } else ""
        } else ""

        val entries = if (existing.isNotEmpty()) "$existing,$newEntry" else newEntry
        // Keep last 50 entries
        val allEntries = entries.split("},").map { it.trim().removeSuffix("}") + "}" }
            .takeLast(50).joinToString(",")
        historyFile.writeText("[$allEntries]")
    }

    /**
     * Gets the previous score from history, or null if no history exists.
     */
    fun getPreviousScore(rootDir: File, moduleName: String): Int? {
        val historyFile = File(rootDir, ".lighthouse/$moduleName-history.json")
        if (!historyFile.exists()) return null
        return try {
            val content = historyFile.readText()
            val entries = parseHistory(content)
            entries.lastOrNull()?.score
        } catch (_: Exception) { null }
    }
}


