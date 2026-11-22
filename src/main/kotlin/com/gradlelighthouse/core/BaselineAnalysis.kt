package com.gradlelighthouse.core

import java.io.File

/**
 * Describes baseline classification for a single audit run.
 *
 * Health scores continue to represent current-state debt (all findings). The baseline
 * only classifies findings as accepted existing debt versus newly introduced findings.
 */
data class BaselineAnalysis(
    val newIssues: List<AuditIssue>,
    val baselinedIssues: List<AuditIssue>,
    val staleFingerprints: Set<String>
) {
    val totalIssueCount: Int get() = newIssues.size + baselinedIssues.size
    val baselineIssueCount: Int get() = baselinedIssues.size
    val newIssueCount: Int get() = newIssues.size
    val staleBaselineCount: Int get() = staleFingerprints.size

    companion object {
        fun analyze(
            issues: List<AuditIssue>,
            baselineFingerprints: Set<String>,
            rootDir: File
        ): BaselineAnalysis {
            val baseline = baselineFingerprints.map(String::trim).filter(String::isNotEmpty).toSet()
            val classified = issues.partition { it.fingerprint(rootDir) in baseline }
            val currentFingerprints = issues.map { it.fingerprint(rootDir) }.toSet()
            return BaselineAnalysis(
                newIssues = classified.second,
                baselinedIssues = classified.first,
                staleFingerprints = baseline - currentFingerprints
            )
        }
    }
}
